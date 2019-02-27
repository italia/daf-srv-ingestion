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

package it.gov.daf.ingestion.config

import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.config.{ConfigMissingException, ConfigReadException}
import play.api.{Configuration, Environment}

import scala.util.{Failure, Success}

class DafConfig @Inject()(configuration: Configuration){

  val hadoopIngestionJarPath:String = configuration.getString("hadoopIngestionJarPath").getOrElse( throw ConfigMissingException("hadoopIngestionJarPath") )

  val servicesConfig = DafServicesConfig.reader.read(configuration) match {
    case Success(config) => config
    case Failure(error)  => throw ConfigReadException(s"Unable to configure [daf services config]", error)
  }

}

object DafConfig  {

  private val config = new DafConfig(Configuration.load(Environment.simple()))

  def apply = config


}



