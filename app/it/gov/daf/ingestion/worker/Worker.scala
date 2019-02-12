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

package it.gov.daf.ingestion.worker

import akka.actor.ActorSystem
import cats.data.EitherT
import it.gov.daf.ingestion.config.DafConfig
import play.api.libs.ws.WSClient
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import cats.implicits._
import io.circe.{Decoder, Encoder}
import it.gov.daf.client.ConductorClient
import it.gov.daf.model.ConductorModel.{TaskPollResult, TaskPostRequest}
import play.api.Logger
import scala.util.{Failure, Success}

class Worker[A:Decoder, B:Encoder] private (taskType:String, pollingInterval:FiniteDuration, pollingTimeout:FiniteDuration, work:Work[A,B])
                                           ( implicit actorSystem: ActorSystem, ws: WSClient) {

  private val logger = Logger(this.getClass.getName)

  val dafServicesConfig = DafConfig.apply

  var running:Boolean = false

  private def startWork():Future[Either[String,String]] = {

    logger.debug("startWork")

    //def pollForTask:EitherT[Future,String,Option[TaskPollResult[A]]] = EitherT( execUntilWithTimeout(ConductorClient.pollTask(taskType), pollingInterval, pollingTimeout, (in:Option[TaskPollResult[A]]) => in.nonEmpty ) )

    def pollForTask:Future[Either[String,Option[TaskPollResult[A]]]]= execUntilWithTimeout(ConductorClient.pollTask(taskType), pollingInterval, pollingTimeout, (in:Option[TaskPollResult[A]]) => in.nonEmpty )



    pollForTask flatMap{ // TASK POLLING

      case Right(opt) =>

        val taskPollResult=opt.get
        val out = for {

          workOutput <- EitherT( work.execute(taskPollResult.inputData) )  // DO SOMETHING

          taskPostRequest:TaskPostRequest[B] = TaskPostRequest( taskPollResult.workflowInstanceId,
                                                                taskPollResult.taskId,
                                                                None ,0, "COMPLETED",
                                                                workOutput )

          updateTaskResp <- EitherT( ConductorClient.updateTask(taskPostRequest) ) // COMUNICATE TASK COMPLETITION

        }yield updateTaskResp


        out.value flatMap {
          case r@Right(_) => Future.successful(r)
          case Left(error) => ConductorClient.updateTask(
                                                          TaskPostRequest(  taskPollResult.workflowInstanceId,
                                                                            taskPollResult.taskId,
                                                                            Some(error) ,0, "FAILED",
                                                                            Map.empty )
                                                        )

        }

      case Left(err) => Future.successful(Left(err))

    }

  }


    /*
    val out = for {
      taskPollResult <- pollForTask // TASK POLLING
      workOutput <- EitherT( work.execute(taskPollResult.get.inputData) )  // DO SOMETHING

      taskPostRequest:TaskPostRequest[B] = TaskPostRequest( taskPollResult.get.workflowInstanceId,
                                                            taskPollResult.get.taskId,
                                                            None ,0, "COMPLETED",
                                                            workOutput )

      updateTaskResp <- EitherT( ConductorClient.updateTask(taskPostRequest) ) // COMUNICATE TASK COMPLETITION
    }yield (taskPollResult,updateTaskResp)


    out.value map{
      case r@Right(_) => r
      case l@Left(err) => ConductorClient.updateTask(TaskPostRequest())
    }*/




  private def startSingleRun():Unit = {

    logger.debug("startSingleRun")

    /*
    val f = Future{startWork()}

    val f1 = akka.pattern.after( singleRunTimeout, actorSystem.scheduler )(Future.failed{new TimeoutException(s"Single Run Timeout ($singleRunTimeout)")})
    val f2 = Future.firstCompletedOf(List(f, f1))
    */

    startWork().onComplete{
      case Success(fx) => fx.foreach( ei=>logger.info(s"Completed, resulting: $ei.toString") )
      case Failure(e) => logger.error(e.getMessage,e)
    }

  }


  def startWorker(runInterval:FiniteDuration):Unit = {

    logger.debug("startWorker")

    if(running){
      logger.warn(s"Worker ${this.taskType} already running" )
      return
    }

    running = true
    val f = executionLoop( startWork, runInterval )

    f.onComplete{
      case Success(fx) => fx.foreach( ei=>logger.info(s"Worker finish its jobs, resulting: $ei.toString") )
      case Failure(e) => logger.error( s"Worker exit with error: ${e.getMessage}", e );running = false
    }

  }


}


object Worker {

  def startSingleWorkerCycle[A:Decoder, B:Encoder](taskType:String,         // TASK TYPE IS POLLING FOR
                                           pollingInterval:FiniteDuration,  // TASK POLLING INTERVAL
                                           pollingTimeout:FiniteDuration,   // HOW POLLING PHASE CAN WAIT TO HAVE A TASK (SINGLE CYCLE DURATION) (0 ->forever)
                                           //singleRunTimeout:FiniteDuration,
                                           work:Work[A,B])
                                          ( implicit actorSystem: ActorSystem, ws: WSClient) =
    new Worker(taskType,pollingInterval,pollingTimeout,work).startSingleRun()


  def startWorkerCycle[A:Decoder, B:Encoder](taskType:String,           // TASK TYPE IS POLLING FOR
                                        pollingInterval:FiniteDuration, // TASK POLLING INTERVAL
                                        pollingTimeout:FiniteDuration,  // HOW POLLING PHASE CAN WAIT TO HAVE A TASK (SINGLE CYCLE DURATION) (0 ->forever)
                                        runInterval:FiniteDuration,     // MINIMUM INTERVAL BETWEEN TWO WORKER CYCLE
                                        work:Work[A,B])
                                       ( implicit actorSystem: ActorSystem, ws: WSClient) =
    new Worker(taskType,pollingInterval,pollingTimeout,work).startWorker(runInterval)


}

