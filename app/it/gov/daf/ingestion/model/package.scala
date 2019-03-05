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

package it.gov.daf.ingestion

package object model {


  trait PrettyPrintable {

    override def toString: String = prettyPrint(this)

    /*
    private def prettyPrint(a: Any): String = {

      //pprint.apply(a).toString()
      pprint.tokenize(a, width = 5).mkString

    }*/

    /**
      * Pretty prints a Scala value similar to its source represention.
      * Particularly useful for case classes.
      * @param a - The value to pretty print.
      * @param indentSize - Number of spaces for each indent.
      * @param maxElementWidth - Largest element size before wrapping.
      * @param depth - Initial depth to pretty print indents.
      * @return
      */

    private def prettyPrint(a: Any, indentSize: Int = 2, maxElementWidth: Int = 30, depth: Int = 0): String = {
      val indent = " " * depth * indentSize
      val fieldIndent = indent + (" " * indentSize)
      val thisDepth = prettyPrint(_: Any, indentSize, maxElementWidth, depth)
      val nextDepth = prettyPrint(_: Any, indentSize, maxElementWidth, depth + 1)
      a match {
        // Make Strings look similar to their literal form.
        case s: String =>
          val replaceMap = Seq(
            "\n" -> "\\n",
            "\r" -> "\\r",
            "\t" -> "\\t",
            "\"" -> "\\\""
          )
          '"' + replaceMap.foldLeft(s) { case (acc, (c, r)) => acc.replace(c, r) } + '"'
        // For an empty Seq just use its normal String representation.
        case xs: Seq[_] if xs.isEmpty => xs.toString()
        case xs: Seq[_] =>
          // If the Seq is not too long, pretty print on one line.
          val resultOneLine = xs.map(nextDepth).toString()
          if (resultOneLine.length <= maxElementWidth) return resultOneLine
          // Otherwise, build it with newlines and proper field indents.
          val result = xs.map(x => s"\n$fieldIndent${nextDepth(x)}").toString()
          result.substring(0, result.length - 1) + "\n" + indent + ")"
        // Product should cover case classes.
        case Some(x) if x.isInstanceOf[Array[String]] =>  val array = x.asInstanceOf[Array[String]] // added to handle arrays
                                                          s"Some(${array.mkString(s",$indent")})"
        case p: Product =>
          val prefix = p.productPrefix
          // We'll use reflection to get the constructor arg names and values.
          val cls = p.getClass
          val fields = cls.getDeclaredFields.filterNot(_.isSynthetic).map(_.getName)
          val values = p.productIterator.toSeq
          // If we weren't able to match up fields/values, fall back to toString.
          if (fields.length != values.length) return p.toString
          fields.zip(values).toList match {
            // If there are no fields, just use the normal String representation.
            case Nil => p.toString
            // If there is just one field, let's just print it as a wrapper.
            case (_, value) :: Nil => s"$prefix(${thisDepth(value)})"
            // If there is more than one field, build up the field names and values.
            case kvps =>
              val prettyFields = kvps.map { case (k, v) => s"$fieldIndent$k = ${nextDepth(v)}" }
              // If the result is not too long, pretty print on one line.
              val resultOneLine = s"$prefix(${prettyFields.mkString(", ")})"
              if (resultOneLine.length <= maxElementWidth) return resultOneLine
              // Otherwise, build it with newlines and proper field indents.
              s"$prefix(\n${prettyFields.mkString(",\n")}\n$indent)"
          }

        case array:Array[String] => {array.mkString(s",$indent")}// added to handle arrays
        // If we haven't specialized this type, just use its toString.
        case _ => s"${a.toString}   (${a.getClass.getCanonicalName})"
      }
    }



   /**
    * Pretty prints case classes with field names.
    * Handles sequences and arrays of such values.
    * Ideally, one could take the output and paste it into source code and have it compile.
      */
   /*
    def prettyPrint(a: Any): String = {
      import java.lang.reflect.Field
      // Recursively get all the fields; this will grab vals declared in parents of case classes.
      def getFields(cls: Class[_]): List[Field] =
        Option(cls.getSuperclass).map(getFields).getOrElse(Nil) ++
          cls.getDeclaredFields.toList.filterNot(f =>
            f.isSynthetic || java.lang.reflect.Modifier.isStatic(f.getModifiers))
      a match {
        // Make Strings look similar to their literal form.
        case s: String =>
          '"' + Seq("\n" -> "\\n", "\r" -> "\\r", "\t" -> "\\t", "\"" -> "\\\"", "\\" -> "\\\\").foldLeft(s) {
            case (acc, (c, r)) => acc.replace(c, r) } + '"'
        case xs: Seq[_] =>
          xs.map(prettyPrint).toString
        case xs: Array[_] =>
          s"Array(${xs.map(prettyPrint) mkString ", "})"
        // This covers case classes.
        case p: Product =>
          s"${p.productPrefix}(${
            (getFields(p.getClass) map { f =>
              f setAccessible true
              s"${f.getName} = ${prettyPrint(f.get(p))}"
            }) mkString ", "
          })"
        // General objects and primitives end up here.
        case q =>
          Option(q).map(_.toString).getOrElse("¡null!")
      }
    }*/


  }

}
