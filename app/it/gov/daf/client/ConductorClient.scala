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

package it.gov.daf.client

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import it.gov.daf.ingestion.config.DafConfig
import it.gov.daf.model.ConductorModel.{TaskPollResult, TaskPostRequest}
import it.gov.daf.model.Workflow
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.{WSAuthScheme, WSClient}

import scala.concurrent.Future

/*
object ConductorClient{

  var instance:ConductorClient = _

  def apply()(implicit ws:WSClient) = {
    if(instance == null) {
      instance = new ConductorClient
    }else instance
  }

}*/

object ConductorClient {

  private val logger = Logger(this.getClass.getName)
  private val dafServicesConfig = DafConfig.apply.servicesConfig

  private val headers = Seq(("Content-Type", "application/json")) //,("Accept", "text/plain")


  def pollTask[A:Decoder](taskType:String)(implicit ws:WSClient):Future[Either[String,Option[TaskPollResult[A]]]] = {

    logger.debug("pollTask")

    val workerId = taskType +"-"+DateTime.now(DateTimeZone.UTC).getMillis()+"-"+Thread.currentThread().getId
    val wsRequest = ws.url(s"${dafServicesConfig.conductorServiceUrl}/api/tasks/poll/$taskType?workerid=$workerId").withHeaders(headers:_*)

    logger.debug(s"pollTask request: $wsRequest")

    wsRequest.get map{ resp =>

      if(resp.status == 204)
        Right(None)
      else if(resp.status < 300){

        parse(resp.body).right.flatMap( _.as[TaskPollResult[A]] ) match{
          case Right(rr) => Right(Some(rr))
          case Left(ee) => Left(s"Parsing or decoding in TaskPollResult failure:${ee.getMessage} \n response body: ${resp.body}")
        }

      }else
        Left(s"Task pollinginvocation problem: \n http code:${resp.status} \nbody:  ${resp.body}")


    }

  }


  def updateTask[A:Encoder](taskPostRequest:TaskPostRequest[A])(implicit ws:WSClient):Future[Either[String,String]] = {

    logger.debug("updateTask")
    /*
  POST http://localhost:8080/api/tasks

  {
    "workflowInstanceId": "5db47ff7-ba96-4c0c-8edc-963776e0dc14",
    "taskId": "e37d9e61-15c0-4270-9534-31a8fa7b0f40",
    "reasonForIncompletion": "fatto e basta",
    "callbackAfterSeconds": 0,
    "workerId": "sonoio",
    "status": "COMPLETED",
    "outputData": {"result":"weeeee"}
  }*/
    val wsRequest = ws.url(s"${dafServicesConfig.conductorServiceUrl}/api/tasks").withHeaders(headers:_*)
    val jsonPost = taskPostRequest.asJson.noSpaces

    logger.debug(s"updateTask request: $wsRequest \njson: $jsonPost")

    wsRequest.post(jsonPost) map{ resp =>
      if(resp.status == 200){
        Right(resp.body)
      }else
        Left(s"Update Conductor Task invocation problem: \n http code:${resp.status} \nbody:  ${resp.body}")

    }

  }


  def startWorkflow[A:Encoder](createFeedWorkflow:Workflow[A] )(implicit ws:WSClient): Future[Either[String, String]] = {

    logger.debug("startWorkflow")

    val jsonPost = createFeedWorkflow.asJson.noSpaces
    val wsRequest = ws.url(dafServicesConfig.conductorServiceUrl + "/api/workflow").withHeaders(headers:_*)

    logger.debug(s"startWorkflow request: $wsRequest \njson: $jsonPost")

    wsRequest.post(jsonPost).map { resp =>
      if (resp.status == 200) {
        //Worker.startSingleRun("prova_task", 5.seconds, 5.minutes, 1.hour, new SparkWork(2.hour))
        Right(resp.body)
      } else
        Left(s"startWorkflow invocation problem: \n http code:${resp.status} \nbody:  ${resp.body}")
    }

  }



}
