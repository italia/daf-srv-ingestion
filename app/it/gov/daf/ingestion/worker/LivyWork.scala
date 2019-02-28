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
import cats.implicits._
import it.gov.daf.ingestion.client.LivyClient
import it.gov.daf.ingestion.model.ConductorModel.AuthHeader
import it.gov.daf.ingestion.model.LivyModel.{LivySession, SessionRequest, Statement, StatementPost}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

class LivyWork(inputKey:String, outputKey:String, jarPaths:Array[String])(implicit val actorSystem:ActorSystem, ws: WSClient) extends Work[StatementPost,Statement]{


  protected def executeImplementation(input:Map[String,StatementPost]):Future[ Either[String, Map[String,Statement]] ] = {

    val statementPost = input(inputKey)

    val out = for{
      //authHeader <- EitherT( checkCredentials(statementPost.authHeader) )
      sessionOpt <- EitherT( LivyClient.getLivyUserSession(statementPost.authHeader) )
      session <-  EitherT( if(sessionOpt.isEmpty) createLivySession(statementPost.authHeader) else Future.successful{Right(sessionOpt.get)} )
      statement <- EitherT( executeStatement(session.id, statementPost) )
    }yield  Map(outputKey -> statement)

    out.value

  }

  /* FOR TESTING
  protected def executeImplementation(input:Map[String,StatementPost]):Future[ Either[String, Map[String,Statement]] ] = {


    val statementPost = input.get("input_spark_task").get

    println("------------------>> "+statementPost.code.mkString("\n"))

    Future.successful(Right(Map("result" -> Statement(0,"we","we",None))))

  }*/


  private def createLivySession(authHeader:AuthHeader):Future[Either[String,LivySession]] ={

    val sessionRequest = SessionRequest("spark", jarPaths)
    val out = for{
      session <- EitherT( LivyClient.createSession(sessionRequest,authHeader) )
      _  <- EitherT( execUntilWithTimeout(  LivyClient.getLivySession(session.id, authHeader),
                                            5.seconds, 5.minutes,
                                            (in:LivySession) => in.state=="idle" ) )
    } yield session

    out.value

  }


  private def executeStatement(sessionId:Long, statementPost:StatementPost):Future[Either[String,Statement]] ={

    val out = for{
      statement <- EitherT( LivyClient.executeStatement(statementPost.toInternal, sessionId, statementPost.authHeader) )
      output  <- EitherT( execUntilWithTimeout( LivyClient.getStatement(statement.id, sessionId, statementPost.authHeader),
                                                10.seconds, 20.minutes,
                                                (in:Statement) => in.state=="available",
                                                (in:Statement) => in.output.fold(false)(_.status=="error")
                                              ) )
    } yield output

    out.value

  }


}
