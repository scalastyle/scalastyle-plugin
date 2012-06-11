// Copyright (C) 2011-2012 the original author or authors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.scalastyle.scalastyleplugin

object ScalastylePluginException {
  def apply(message: String, cause: Throwable): ScalastylePluginException = new ScalastylePluginException(message, cause)
  def apply(message: String): ScalastylePluginException = new ScalastylePluginException(message, null)
  def rethrow(t: Throwable, message: String): Unit = {
    t match {
      case s: ScalastylePluginException => throw s
      case _ => throw new ScalastylePluginException(message, t)
    }
  }
  def rethrow(t: Throwable): Unit = rethrow(t, t.getMessage)
}

class ScalastylePluginException(message: String, cause: Throwable) extends Exception(message, cause)