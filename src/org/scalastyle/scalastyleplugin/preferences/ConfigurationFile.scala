package org.scalastyle.scalastyleplugin.preferences

import scala.xml.Elem
import org.segl.scalastyle._
import java.io._

object ConfigurationFile {
  // TODO change filename to an IFile?
  def write(filename: String, scalastyleConfiguration: ScalastyleConfiguration) = {
    val s = ScalastyleConfiguration.toXmlString(scalastyleConfiguration, 1000, 1)

    var out: Writer = null;

    try {
      out = new BufferedWriter(new FileWriter(filename))
      out.write(s)
    } catch {
      case e => throw e // TODO do something here
    } finally {
      try {
        if (out != null) {
          out.close();
        }
      } catch {
        case _ => // do nothing
      }
    }
  }

}