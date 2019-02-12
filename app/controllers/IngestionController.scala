
/*
 *
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Source}
import cats.data.EitherT
import io.swagger.annotations.{ApiImplicitParams, _}
import it.gov.daf.common.utils.RequestContext.execInContext
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import play.api.mvc._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import cats.implicits._
import io.circe.generic.auto._
import it.gov.daf.client.ConductorClient
import it.gov.daf.common.authentication.Authentication
import it.gov.daf.common.config.ConfigReadException
import it.gov.daf.common.sso.common.CredentialManager
import it.gov.daf.ingestion.config.DafServicesConfig
import it.gov.daf.ingestion.worker.{LivyWork, TestWork, Worker}
import it.gov.daf.model.ConductorModel.AuthHeader
import it.gov.daf.model.{CreateFeedWorkflow, CreateFeedWorkflowInput}
import org.pac4j.play.store.PlaySessionStore
import play.api.libs.circe.Circe

import scala.concurrent.duration._
import scala.util.{Failure, Success}


@Api
class IngestionController @Inject()( configuration: Configuration, playSessionStore: PlaySessionStore)(implicit actorSystem: ActorSystem, ws: WSClient) extends Controller with Circe{

  Authentication(configuration,playSessionStore)

  private val logger = Logger(this.getClass.getName)

  val dafServicesConfig = DafServicesConfig.reader.read(configuration) match {
    case Success(config) => config
    case Failure(error)  => throw ConfigReadException(s"Unable to configure [daf services config]", error)
  }

  // Not a REST API
  // Upload del file + infer
  def infer = Action.async(parse.multipartFormData) { implicit request =>

    execInContext[Future[Result]]("infer") { () =>
      handleException {

        logger.debug("in infer action")

        val username = CredentialManager.readCredentialFromRequest(request).username
        val authHeader = ("Authorization", request.headers.get("Authorization").get)

        val datasetName: Option[String] = request.body.dataParts.get("datasetName").flatMap(_.headOption)
        val quote: Option[String] = request.body.dataParts.get("quote").flatMap(_.headOption)
        val delimiter: Option[String] = request.body.dataParts.get("delimiter").flatMap(_.headOption)
        val escape: Option[String] = request.body.dataParts.get("escape").flatMap(_.headOption)
        val isCsv: Boolean = request.body.dataParts.get("iscsv").flatMap(_.headOption).getOrElse("false") == "true"

        val filePartList = request.body.file("sample").map(_.ref.file) match{
          case Some(file) => FilePart( "sample", file.getName, Option("text/plain"), FileIO.fromPath(file.toPath) ) :: List.empty
          case None => List.empty
        }

        val multipartList = filePartList ++ quote.map{ DataPart("quote", _) } ++
          delimiter.map{ DataPart("delimiter", _) } ++
          escape.map{ DataPart("escape", _) } ++
          Some(DataPart("iscsv", isCsv.toString))

        def checkInputData:Either[String,String] = datasetName match{
          case Some(x) => Right("ok")
          case None => Left("field datasetName cannot be empty")
        }

        def callInferService: Future[Either[String,String]] = {

          logger.debug("callInferService")

          ws.url(dafServicesConfig.inferSchemaUrl + "/spark-local/infer").post(Source(multipartList)).map { resp =>
            if (resp.status == OK)
              Right(Json.stringify(resp.json))
            else
              Left(s"Spark schema infer service problem: \n ${resp.body}")

          }
        }

        def callFileUploadService: Future[Either[String, String]] = {

          logger.debug("callFileUploadService")

          val request = ws.url(dafServicesConfig.proxyServiceUrl + s"/hdfs/proxy/uploads/$username/feed/${datasetName.get}.${if (isCsv) "csv" else "json"}?op=CREATE").withHeaders(authHeader)
          logger.debug(s"request: $request")

          request.put(Source(filePartList)).map { resp =>
            if (resp.status == OK)
              Right("ok")
            else
              Left(s"Hdfs upload service problem: \n ${resp.body}")

          }
        }


        val out = for {
          _       <- EitherT(Future.successful{checkInputData})
          esito   <- EitherT(callInferService)
          esito2  <- EitherT(callFileUploadService)
        } yield esito

        out.value

      }
    }

  }


  @ApiOperation(
    value = "Start a feed workflow",
    response = classOf[String]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Feed workflow input information", name="body payload",
      required = true, dataType = "it.gov.daf.model.CreateFeedWorkflowInput", paramType = "body")
    )
  )
  def startFeed = Action.async( circe.json[CreateFeedWorkflowInput] ) { implicit request =>

    execInContext[Future[Result]]("startFeed") { () =>
      handleException{

        def callStartWorkflow(input:CreateFeedWorkflowInput): Future[Either[String,String]] = {

          val createFeedWorkflow = CreateFeedWorkflow("feed-creation", 1, Map("feedInput"->input))

          ConductorClient.startWorkflow(createFeedWorkflow).map{
            case ok@Right(_) => Worker.startSingleWorkerCycle( "spark-job-launch", 5.seconds, 20.minutes,
                                                                new LivyWork( "input_spark_task", "result", Array("/uploads/atroisi/daf.jar") )
                                                              )
                                ok//Worker.startSingleRun("task_uno", 5.seconds, 5.minutes, 1.hour, new SparkWork(2.hour)); ok
            case ko@Left(_) => ko
          }

        }


        val authHeader = ("authorization", request.headers.get("authorization").get)
        val inputDataWithCredential = request.body.copy( authHeader=Some(AuthHeader.fromHeaderValue( request.headers.get("Authorization") ).get), env=Some(Map("sec-man-url"->dafServicesConfig.proxyServiceUrl)) )

        val res = for {
          //inputData <- EitherT(Future.successful{checkInputData})
          //inputDataWithCredential = request.body.copy( authHeader=Some(AuthHeader.fromHeaderValue( request.headers.get("Authorization"))) )
          out <- EitherT(callStartWorkflow(inputDataWithCredential))
        } yield out

        res.value

      }

    }
  }

/*
  case class Prova( uno:String, due:Option[String])
  def wewe = Action.async( circe.json[Prova] ) { implicit request =>

    execInContext[Future[Result]]("wewe") { () =>
      handleException{
        println("--->"+request.body)
        Future.successful(Right(request.body))

      }

    }
  }*/

  /* Try{ callServices(request.body) } match{
   case Success(x) => x.map{
     case Right(xx) => Ok(xx)
     case Left (ee) => InternalServerError(ee)
   }
   case Failure(e) => e.printStackTrace();Future.successful{ InternalServerError( e.getLocalizedMessage + e.getStackTrace.mkString("\n") ) }
 }*/

  /*
  def startIngestion = Action { implicit request =>
    execInContext[Result]("startIngestion") { () =>


      def workerFunc = { println("wewewewe")
                         Thread.sleep(10000)
                         "fattoo"}

      startWorker(workerFunc,11.seconds)

      Ok("todooo")

    }
  }


  private def startWorker(workerFunction: =>String,timeout:FiniteDuration):Unit = {

    val f = Future{workerFunction}


    val f1 = akka.pattern.after( timeout, actorSystem.scheduler )(Future.failed{new TimeoutException("timeouut")})
    val f2 = Future.firstCompletedOf(List(f, f1))

    f2.onComplete{
      case Success(x) => println(x)
      case Failure(e) => println(e)
    }

  }
  */


}
