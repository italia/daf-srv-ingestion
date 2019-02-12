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
import it.gov.daf.model.ConductorModel.AuthHeader
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

trait Work[A,B]{

  type WorkInputType = A
  type WorkOutputType = B

  private val logger = Logger(this.getClass.getName)
  //val workTimeout:FiniteDuration
  implicit val actorSystem:ActorSystem

  /*
  protected def checkCredentials(authHeader:Option[AuthHeader]):Future[Either[String,AuthHeader]]={

    authHeader match {
      case Some( AuthHeader(Some(hname), Some(authType),Some(token)) ) => Future.successful {
        Right( AuthHeader(Some(hname), Some(authType),Some(token)) )
      }
      case _ => Future.successful {
        Left("Auth header invalid")
      }
    }

  }*/

  def execute(input:Map[String,WorkInputType]):Future[Either[String,Map[String,WorkOutputType]]] = {

      executeImplementation(input).recover {
        case e: Throwable =>  logger.error(e.getMessage, e)
                              Left(e.getMessage)
      }


  }


  protected def executeImplementation(input:Map[String,WorkInputType]):Future[Either[String,Map[String,WorkOutputType]]]

}
