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

import it.gov.daf.model.ConductorModel.AuthHeader

object LivyModel {

  case class LivySession(id: Long, appId: Option[String], owner: String,
    proxyUser: String, kind: String,
    log: List[String], appInfo: Map[String, Option[String]], state: String) extends PrettyPrintable

  case class LivySessions(from: Int, total: Int, sessions: List[LivySession]) extends PrettyPrintable

  case class SessionRequest(kind: String, jars: Array[String]) extends PrettyPrintable

  case class StatementPost(code: Array[String], authHeader:AuthHeader) extends PrettyPrintable {
    def toInternal:StatementPostInternal=StatementPostInternal( code.mkString("\n") )
  }

  case class StatementPostInternal(code: String)

  case class Statement(id: Long, code: String, state: String, output: Option[Output]) extends PrettyPrintable

  case class Output(status: String, execution_count: Int, data: Option[Map[String, String]], ename:Option[String], evalue:Option[String], traceback: Option[Array[String]] ) extends PrettyPrintable

  case class JobDeletion(msg: String)

}
