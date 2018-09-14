package com.eds.dora.util

object TxDateUtils {

  val SECOND_IN_MILLIS=1000
  val MIN_IN_MILLIS=SECOND_IN_MILLIS*60L
  val HOUR_IN_MILLIS=MIN_IN_MILLIS*60L
  val DAY_IN_MILLIS=HOUR_IN_MILLIS*24L
  val WEEK_IN_MILLIS=DAY_IN_MILLIS*7L

  def parse(value:String, defaultFormattedDateTime:String):String={
    if(defaultFormattedDateTime==null || defaultFormattedDateTime.length==0){
      parse(value,System.currentTimeMillis())
    }else{
      parse(value,DateTimeUtils.getMillisFromDateTimeExp(defaultFormattedDateTime,"yyyy-MM-dd HH:mm:ss"))
    }
  }

  def parse(value:String, ts:Long):String={
    try{
      var result:String=ignoreCaseReplace(value,"$tx_date()",DateTimeUtils.getDateTimeExp(ts,"yyyy-MM-dd"))
      result=ignoreCaseReplace(result,"$tx_datetime()",DateTimeUtils.getDateTimeExp(ts,"yyyy-MM-dd HH:mm:ss"))
      var expStart="$tx_date("
      var expEnd=")"
      while(ignoreCaseIndexOf(result,expStart)>=0){
        val startIndex=ignoreCaseIndexOf(result,expStart)
        val endIndex=ignoreCaseIndexOf(result,expEnd,startIndex)
        val exp=result.substring(startIndex,endIndex+1)
        val content=result.substring(startIndex+expStart.length,endIndex)
        val tokens=content.split(",")
        if(tokens.length==1){
          result=result.replace(exp,getTxDateTime(content,"0s",ts))
        }else if(tokens.length==2){
          if(tokens(0).trim.length==0){
            tokens(0)="yyyy-MM-dd"
          }
          result=result.replace(exp,getTxDateTime(tokens(0),tokens(1),ts))
        }else{
          throw new RuntimeException("Unable to parse "+exp)
        }
      }
      expStart="$tx_datetime("
      expEnd=")"
      while(ignoreCaseIndexOf(result,expStart)>=0){
        val startIndex=ignoreCaseIndexOf(result,expStart)
        val endIndex=ignoreCaseIndexOf(result,expEnd,startIndex)
        val exp=result.substring(startIndex,endIndex+1)
        val content=result.substring(startIndex+expStart.length,endIndex)
        val tokens=content.split(",")
        if(tokens.length==1){
          result=result.replace(exp,getTxDateTime(content,"",ts))
        }else if(tokens.length==2){
          if(tokens(0).trim.length==0){
            tokens(0)="yyyy-MM-dd HH:mm:ss"
          }
          result=result.replace(exp,getTxDateTime(tokens(0),tokens(1),ts))
        }else{
          throw new RuntimeException("Unable to parse "+exp)
        }
      }
      result=ignoreCaseReplace(result,"$tx_datetime",DateTimeUtils.getDateTimeExp(ts,"yyyy-MM-dd HH:mm:ss"))
      result=ignoreCaseReplace(result,"$tx_date",DateTimeUtils.getDateTimeExp(ts,"yyyy-MM-dd"))
      result
    }catch{
      case ex:Exception=>throw new RuntimeException("Error ocurred when parsing $tx_date/$tx_datetime",ex)
    }
  }

  private def ignoreCaseIndexOf(inputString:String,find:String, start:Int): Int ={
    val lowerInputString=inputString.toLowerCase
    val lowerFind=find.toLowerCase
    lowerInputString.indexOf(lowerFind,start)
  }

  private def ignoreCaseIndexOf(inputString:String,find:String): Int ={
    val lowerInputString=inputString.toLowerCase
    val lowerFind=find.toLowerCase
    lowerInputString.indexOf(lowerFind)
  }

  private def ignoreCaseReplace(inputString:String, find:String, replace:String):String={
    ignoreCaseReplace(inputString,find,replace,true,0)
  }

  private def ignoreCaseReplace(inputString:String, find:String, replace:String, replaceAll:Boolean, nextStart:Int): String ={
    val lowerInputString=inputString.toLowerCase
    val lowerFind=find.toLowerCase
    val start=lowerInputString.indexOf(lowerFind,nextStart)
    if(start<0){
      inputString
    }else{
      val length=lowerFind.length
      val result=inputString.substring(0,start)+replace+inputString.substring(start+length)
      if(replaceAll && result.toLowerCase.indexOf(find,nextStart+1)>=0){
        ignoreCaseReplace(result,find,replace,replaceAll,nextStart+1)
      }else{
        result
      }
    }
  }

  val shiftPattern="(\\+[0-9]+|\\-[0-9]+|[0-9]+)[A-z]".r
  private def getTxDateTime(pattern:String,shift:String,ts:Long):String={
    val shiftList=shiftPattern.findAllIn(shift).toList
    if(shiftList.length>1){
      var tmpTs=ts
      for(i<-0 to shiftList.length-1){
        tmpTs=DateTimeUtils.getMillisFromDateTimeExp(getTxDateTime("yyyy-MM-dd HH:mm:ss",shiftList(i),tmpTs),"yyyy-MM-dd HH:mm:ss")
      }
      DateTimeUtils.getDateTimeExp(getMillisFromTxDateTime("0d",tmpTs),pattern)
    }else{
      DateTimeUtils.getDateTimeExp(getMillisFromTxDateTime(shiftList(0),ts),pattern)
    }
  }


  def getMillisFromTxDateTime(shift:String,ts:Long):Long={

    if(shift==null || shift.trim.length==0){
      ts
    }else{
      val trimShift=shift.trim
      val unit:Char=shift.charAt(shift.length-1)
      val num=trimShift.substring(0,trimShift.length-1).toInt
      unit match{
        case 's'=> ts+num*SECOND_IN_MILLIS
        case 'm'=> ts+num*MIN_IN_MILLIS
        case 'H'=> ts+num*HOUR_IN_MILLIS
        case 'D'=> ts+num*DAY_IN_MILLIS
        case 'd'=> ts+num*DAY_IN_MILLIS
        case 'w'=> ts+num*WEEK_IN_MILLIS
        case 'W'=> ts+num*WEEK_IN_MILLIS
        case 'y'=> getMillisFromTxDateTime((num*12)+"M",ts)
        case 'Y'=> getMillisFromTxDateTime((num*12)+"M",ts)
        case 'M'=> {
          var year=DateTimeUtils.getDateTimeExp(ts,"yyyy").toInt
          var month=DateTimeUtils.getDateTimeExp(ts,"MM").toInt
          val rest=DateTimeUtils.getDateTimeExp(ts,"dd HH:mm:ss")
          if(num>0){
            for(i<-1 to num){
              month+=1
              if(month==13) {
                month=1
                year += 1
              }
            }
          }else{
            for(i<-(-1) to num by -1){
              month-=1
              if(month==0) {
                month=12
                year -= 1
              }
            }
          }
          var yearString=year+""
          while(yearString.length<4){
            yearString="0"+yearString
          }
          val monthString=if(month.toString.length==1)"0"+month else month
          val newTs=DateTimeUtils.getMillisFromDateTimeExp(yearString+"-"+monthString+"-"+rest,"yyyy-MM-dd HH:mm:ss")
          newTs
        }
        case _ => throw new RuntimeException("Error ocurred while parsing "+shift)
      }
    }
  }
}
