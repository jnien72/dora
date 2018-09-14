package com.eds.sql.udf;


import org.apache.spark.sql.types.DataType;

public interface DoraUDF {

    public DataType getDataType();
    public Class getUDFType();

    public String getUsage();
    public String getDescription();
}
