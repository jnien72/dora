package com.eds.dora.util

object MemoryCalculator {

  private final val UNITS = Array[Char]('g', 'm')

  def sum(memories: List[String]): (Double, Char) = {
    val smallestUnitSum = memories
      .map(_.toLowerCase())
      .foldLeft(0.0)((sum, memory) => {
        val matchedUnit = UNITS.zipWithIndex.find(x => {
          x._1 == memory.charAt(memory.length - 1)
        })
        if(matchedUnit.isDefined) {
          val index = matchedUnit.get._2
          val value = Math.pow(1024, UNITS.length - 1 - index)
          sum + memory.substring(0, memory.length - 1).toDouble * value
        } else {
          throw new RuntimeException("the format of memory value [" + memory + "] is not correct")
        }
      })
    var largestUnitSum = smallestUnitSum
    for(i <- 0 to UNITS.length - 2) {
      largestUnitSum = largestUnitSum/1024.0
    }
    ((largestUnitSum * 100).toInt / 100.0,  UNITS(0))
  }

}
