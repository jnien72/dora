package com.eds.dora.util

import java.io._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

object HdfsUtils {
  val fs=FileSystem.get(new Configuration())

  def readFilesContentFromDir(path: String): InputStream = {
    if(fs.isDirectory(new Path(path))) {
      val status = fs.listStatus(new Path(path)).sortWith((x, y) => x.getPath.compareTo(y.getPath) < 0)
      val vector = new java.util.Vector[InputStream]()
      status.foreach(s => {
        vector.add(fs.open(s.getPath))
      })
      new SequenceInputStream(vector.elements())
    } else {
      throw new RuntimeException("Path [" + path + "] is not a directory")
    }
  }

  def readTextFile(folderPath:String,fileName:String):String={
   scala.io.Source.fromInputStream(fs.open(new Path(folderPath+"/"+fileName)),"utf-8").mkString
  }

  def writeTextFile(folderPath:String,fileName:String,content:String)={
    fs.mkdirs(new Path(folderPath))
    val out=fs.create(new Path(folderPath+"/"+fileName))
    val writer=new OutputStreamWriter(out)
    writer.write(content)
    writer.flush()
    writer.close()
    out.close()
  }

  def isDirectory(path:String)={
    fs.isDirectory(new Path(path))
  }

  def mkdirs(path:String)={
    fs.mkdirs(new Path(path))
  }

  def delete(path:String)={
    fs.delete(new Path(path),true)
  }

  def move(srcPath:String, dstPath:String)={
    val dst=new Path(dstPath)
    fs.mkdirs(dst.getParent)
    fs.rename(new Path(srcPath),dst)
  }

  def list(srcPath:String):Array[String]={
    fs.listStatus(new Path(srcPath)).map(x=>x.getPath.getName)
  }

}
