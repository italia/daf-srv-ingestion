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

import cats.data.EitherT
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import it.gov.daf.common.utils.RequestContext
import it.gov.daf.ingestion.config.DafConfig
import it.gov.daf.model.ConductorModel.AuthHeader
import it.gov.daf.model.LivyModel._
import play.api.Logger
import play.api.libs.ws.{WSAuthScheme, WSClient}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import cats.implicits._
import scala.concurrent.Future

object LivyClient {


  private val logger = Logger(this.getClass.getName)
  private val dafServicesConfig = DafConfig.apply.servicesConfig

  def getLivyUserSession(authHeader:AuthHeader)(implicit ws:WSClient):Future[Either[String,Option[LivySession]]] = {

    val userName = RequestContext.getUsername()
    EitherT( getLivySessions(authHeader) ).map{ _.sessions.find(_.owner==userName) }.value

  }

  def getLivySessions(authHeader:AuthHeader)(implicit ws:WSClient):Future[Either[String,LivySessions]] = {

    val request = ws.url(s"${dafServicesConfig.proxyServiceUrl}/livy/proxy/sessions").withHeaders(authHeader.header)

    logger.debug(s"getLivyUserSession request: $request")

    request.get map{ resp =>

      if(resp.status == 200){

        parse(resp.body).right.flatMap( _.as[LivySessions] ) match{
          case Right(rr) => Right(rr)
          case Left(ee) => Left( s"Parsing or decoding failure in getLivyUserSession :${ee.getMessage}" )
        }

      }else
        Left(s"getLivyUserSession invocation Livy service problem: status ${resp.status} \n body ${resp.body}")

    }

  }


  def getLivySession(id:Long, authHeader:AuthHeader)(implicit ws:WSClient):Future[Either[String,LivySession]] = {

    val request = ws.url(s"${dafServicesConfig.proxyServiceUrl}/livy/proxy/sessions/$id").withHeaders(authHeader.header)

    logger.debug(s"getLivySession request: $request")

    request.get map{ resp =>

      if(resp.status == 200){

        parse(resp.body).right.flatMap( _.as[LivySession] ) match{
          case Right(rr) => Right(rr)
          case Left(ee) => Left( s"Parsing or decoding failure in getLivySession :${ee.getMessage}" )
        }

      }else
        Left(s"getLivySession invocation Livy service problem: status ${resp.status} \n body ${resp.body}")

    }

  }


  def createSession(sessionRequest:SessionRequest, authHeader: AuthHeader)(implicit ws:WSClient):Future[Either[String,LivySession]] = {

    val postBody = sessionRequest.asJson.noSpaces
    val request = ws.url(s"${dafServicesConfig.proxyServiceUrl}/livy/proxy/sessions").withHeaders(authHeader.header).withHeaders("Content-Type"->"application/json")

    logger.debug(s"createSession request: $request \nwith body: $postBody")

    request.post(postBody) map{ resp =>

      if(resp.status == 201){

        parse(resp.body).right.flatMap( _.as[LivySession] ) match{
          case Right(rr) => Right(rr)
          case Left(ee) => Left( s"Parsing or decoding failure in createSession:${ee.getMessage}"  )
        }

      }else
        Left(s"createSession invocation Livy service problem: status ${resp.status} \n body ${resp.body}")

    }

  }

  def executeStatement( statementPost:StatementPostInternal, sessionId:Long, authHeader: AuthHeader )(implicit ws:WSClient):Future[Either[String,Statement]] = {

    val postBody = statementPost.asJson.noSpaces
    val request = ws.url(s"${dafServicesConfig.proxyServiceUrl}/livy/proxy/sessions/$sessionId/statements").withHeaders(authHeader.header).withHeaders("Content-Type"->"application/json")

    logger.debug(s"executeStatement request: $request \nwith body: $postBody")

    request.post(postBody) map{ resp =>

      if(resp.status == 201){

        parse(resp.body).right.flatMap( _.as[Statement] ) match{
          case Right(rr) => Right(rr)
          case Left(ee) => Left( s"Parsing or decoding failure in executeStatement:${ee.getMessage}"  )
        }

      }else
        Left(s"executeStatement invocation Livy service problem: status ${resp.status} \n body ${resp.body}")

    }

  }

  def getStatement( statementId:Long, sessionId:Long, authHeader:AuthHeader )(implicit ws:WSClient):Future[Either[String,Statement]] = {

    val request = ws.url(s"${dafServicesConfig.proxyServiceUrl}/livy/proxy/sessions/$sessionId/statements/$statementId").withHeaders(authHeader.header).withHeaders("Content-Type"->"application/json")

    logger.debug(s"getStatement request: $request")

    request.get map{ resp =>

      if(resp.status == 200){

        parse(resp.body).right.flatMap( _.as[Statement] ) match{
          case Right(rr) => Right(rr)
          case Left(ee) => Left( s"Parsing or decoding failure in getStatement:${ee.getMessage}"  )
        }

      }else
        Left(s"getStatement invocation Livy service problem: status ${resp.status} \n body ${resp.body}")

    }

  }

}
