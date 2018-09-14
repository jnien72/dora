package com.eds.dora.util

object AssertUtils {

  def isEmpty(value: String): Boolean = {
    !hasValue(value)
  }

  def hasValue(value: String): Boolean = {
    value != null && value.trim.length > 0
  }

}
