
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

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import io.circe.Encoder
import io.circe.syntax._



package object controllers {
  /*
  def handleException[A:Encoder](f: =>Future[Either[String, A]] ):Future[Result] = {

    Try{f} match{
      case Success(x) => x.map{
        case Right(xx) => Results.Ok(xx.asJson.noSpaces)
        case Left (ee) => Results.InternalServerError(ee)
      }
      case Failure(e) => e.printStackTrace();Future.successful{ Results.InternalServerError( e.getLocalizedMessage + e.getStackTrace.mkString("\n") ) }
    }
  }*/

  def handleException[A:Encoder](f: =>Future[Either[String, A]] ):Future[Result] = {

    f map {
        case Right(xx) => Results.Ok(xx.asJson.noSpaces)
        case Left (ee) => Results.InternalServerError(ee)
      }

  }

}
