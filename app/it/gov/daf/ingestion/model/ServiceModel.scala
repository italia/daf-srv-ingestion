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

package it.gov.daf.ingestion.model

import it.gov.daf.ingestion.model.ConductorModel.AuthHeader

// for now this case class is the same of FeedWorkflowInput
case class IngestionWorkflowInput( datasetName:String, isCsv:Boolean, file:String, archivePath:String, schemaString:String, csvChars:Option[CsvChars],
                                    datasetPath:String, dbName:String, tableName:String, authHeader: Option[AuthHeader], env:Option[Map[String,String]] ) extends PrettyPrintable

case class FeedWorkflowInput( datasetName:String, isCsv:Boolean, file:String, archivePath:String, schemaString:String, csvChars:Option[CsvChars],
                                   datasetPath:String, dbName:String, tableName:String, authHeader: Option[AuthHeader], env:Option[Map[String,String]] ) extends PrettyPrintable

case class Workflow[A](name:String, version:Int, input:Map[String,A]) extends PrettyPrintable

case class CsvChars(quote:String, delimiter:String, escape:String) extends PrettyPrintable



