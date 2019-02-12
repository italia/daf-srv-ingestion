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

import com.google.inject.{AbstractModule, Singleton}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment, Logger}

import scala.sys.process._

@Singleton
class Module(environment: Environment, configuration: Configuration) extends AbstractModule{


  def configure(): Unit ={


    Logger.debug("executing module..")

    //bind[WSClient].toInstance("foo")
    //bind[GuiceSpec.type].toInstance(GuiceSpec)

    //bind(classOf[LoginClient]).to(classOf[LoginClientLocal])//for the initialization of SecuredInvocationManager


    //val cacheWrapper = new CacheWrapper(cookieExpiration,tokenExpiration)
    //cacheWrapper.putCredentialsWithoutTTL(ConfigReader.suspersetOpenDataUser,ConfigReader.suspersetOpenDataPwd)
    //bind(classOf[CacheWrapper]).toInstance(cacheWrapper)

  }

}


