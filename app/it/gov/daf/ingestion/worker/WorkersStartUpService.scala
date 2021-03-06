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
import com.google.inject.{Inject, Singleton}
import play.api.libs.ws.WSClient
import io.circe.generic.auto._
import scala.concurrent.duration._

@Singleton
class WorkersStartUpService @Inject()(implicit actorSystem: ActorSystem, ws: WSClient){

  //Worker.startWorkerCycle( "task_uno", 5.seconds, 20.seconds, 30.seconds, new TestWork() )

}
