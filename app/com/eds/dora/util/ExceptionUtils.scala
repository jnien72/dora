package com.eds.dora.util

import java.io.{PrintWriter, StringWriter}

object ExceptionUtils {
  def toString(t: Throwable): String = {
    val sw: StringWriter = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    sw.toString
  }
}
