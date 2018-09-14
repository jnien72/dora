package com.eds.dora.util

import java.text.SimpleDateFormat
import java.util.Date

object DateTimeUtils {

  def getDateTimeExp(millis: Long): String = {
    getDateTimeExp(millis, "yyyy-MM-dd HH:mm:ss")
  }

  def getDateTimeExp(millis: Long, pattern: String): String = {
    val sdf: SimpleDateFormat = new SimpleDateFormat(pattern)
    sdf.format(new Date(millis))
  }

  def getMillisFromDateTimeExp(dateTimeExpression:String,pattern:String):Long={
    val sdf: SimpleDateFormat = new SimpleDateFormat(pattern)
    sdf.parse(dateTimeExpression).getTime
  }

  def getTimeDiffExp(start: Long, end: Long): String = {
    val elapsedMillis = if (end > 0) (end - start) else System.currentTimeMillis() - start
    if (elapsedMillis < 1000) {
      return elapsedMillis + " ms"
    }
    val elapsedSeconds = elapsedMillis / 1000;
    if (elapsedSeconds < 60) {
      return elapsedSeconds + " sec(s)";
    }
    val elapsedMinutes = elapsedSeconds / 60;
    if(elapsedMinutes<60){
      return elapsedMinutes + " min(s) " + (elapsedSeconds % 60) + " sec(s)";
    }
    val elapsedHours = elapsedMinutes / 60
    return elapsedHours + " hr(s) " + (elapsedMinutes % 60) + " min(s)";
  }

}
