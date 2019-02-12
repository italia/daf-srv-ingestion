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

package it.gov.daf.ingestion

import akka.actor.ActorSystem
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

package object worker {

  private val logger = Logger("it.gov.daf.ingestion.package_object_worker")


  def delayExecution[A]( future: Future[Either[String,A]], delay:FiniteDuration )(implicit actorSystem:ActorSystem, executionContext: ExecutionContext):Future[Either[String,A]] = {
    logger.debug(s"delayExecution : $delay")
    akka.pattern.after( delay, actorSystem.scheduler )(future)
  }

/*
  def executionWithTimeout[A]( future:Future[Either[String,A]], timeout:FiniteDuration )(implicit actorSystem:ActorSystem, executionContext: ExecutionContext):Future[Either[String,A]] = {

    logger.debug("executionWithTimeout")

    //val timedOutFuture = akka.pattern.after( timeout, actorSystem.scheduler )(Future.failed{new TimeoutException("timeouut!!")})
    //withTimeout(future, timeout)
    FutureUtil.futureWithTimeout(future,timeout)

    /*
    val completed =
    if (future != completed){
      future.
    }*/

  }
*/

  def execUntilWithTimeout[A]( futureTask: =>Future[Either[String,A]],
                               delay:FiniteDuration,
                               timeout:FiniteDuration,
                               okCondition:(A)=>Boolean,
                               koCondition:(A)=>Boolean = (_:A)=>false )
                             (implicit actorSystem:ActorSystem, executionContext: ExecutionContext):Future[Either[String,A]]={

    logger.debug("execUntilWithTimeout")

    val init = System.currentTimeMillis()

    def execUntil:Future[Either[String,A]] = if(System.currentTimeMillis()-init < timeout.toMillis)
      delayExecution(futureTask,delay) flatMap {
        case l @ Left(_) => Future{l}
        case r @ Right(resp) if koCondition(resp) =>  logger.debug(s"Exit loop with response: $resp"); Future.successful{Left(resp.toString)}
        case r @ Right(resp) if okCondition(resp) =>  logger.debug(s"Exit loop with response: $resp"); Future.successful{r}
        case r @ _ => logger.debug(s"Looping with: ${r.toString}");execUntil
      }
    else{
      logger.debug(s"Execution timed out -> $timeout")
      Future.successful(Left(s"Execution timed out -> $timeout"))
    }


    execUntil

  }


  // EXEC FOREVER
  def executionLoop[A]( performedFunc: =>Future[Either[String,A]], delay:FiniteDuration )(implicit actorSystem:ActorSystem, executionContext: ExecutionContext):Future[Either[String,A]]={

    logger.debug("executionLoop")

    def execDelayed:Future[Either[String,A]] = delayExecution(performedFunc,delay).flatMap {
        case Left(l) => logger.error(s"execution loop step: execution failure :$l"); execDelayed
        case _ => logger.debug("execution loop step"); execDelayed
      }.recoverWith{ case t:Throwable => logger.error(s"execution loop step: executionLoop failure: ${t.getMessage}",t); execDelayed}

    execDelayed

  }

}
