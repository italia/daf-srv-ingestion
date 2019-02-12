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

package it.gov.daf.model

import it.gov.daf.common.authentication.Authentication
import org.apache.commons.codec.binary.Base64


object ConductorModel {

  case class TaskPollResult[A](inputData:Map[String,A], workflowInstanceId: String, workflowType: String, taskId: String)

  case class TaskPostRequest[A](workflowInstanceId: String, taskId: String, reasonForIncompletion: Option[String], callbackAfterSeconds:Long, status: String, outputData:Map[String,A])

  case class AuthHeader(name:String, authType:String, token:String){

    lazy val user:String = {

      if(authType == "Bearer")
        Authentication.getClaimsFromToken(Some(token)).get("sub").toString
      else
        new String(Base64.decodeBase64(token.getBytes)).split(":")(0)

      // if( authToken._2.startsWith("Bearer") )
      // Authentication.getClaimsFromToken( authToken._2.split(" ").lastOption )
    }

    lazy val header:(String,String) = (name, s"$authType $token")

  }

  object AuthHeader{

    def fromHeaderValue(headerValue:Option[String]):Option[AuthHeader] = {

      headerValue match{
        case Some(hv) =>  val hvSplit= hv.split(" ")
                          Some(AuthHeader( "Authorization", hvSplit.head, hvSplit.last ))
        case None => None
      }

    }

  }

}



