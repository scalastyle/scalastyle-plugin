package org.scalastyle.scalastyleplugin

object ScalastylePluginException {
  def apply(message: String, cause: Throwable) = new ScalastylePluginException(message, cause)
  def rethrow(t: Throwable, message: String): Unit = {
    t match {
      case s: ScalastylePluginException => throw s
      case _ => throw new ScalastylePluginException(message, t)
    }
  }
  def rethrow(t: Throwable): Unit = rethrow(t, t.getMessage)
}

class ScalastylePluginException(message: String, cause: Throwable) extends Exception(message, cause)