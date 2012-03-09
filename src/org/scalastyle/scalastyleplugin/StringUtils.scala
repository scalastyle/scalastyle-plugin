package org.scalastyle.scalastyleplugin

object StringUtils {
  def toOption(s: String): Option[String] = if (isEmpty(s.trim())) None else Some(s.trim())
  def isEmpty(s: String) = (s == null || s.trim() == "")
}