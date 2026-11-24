#!/bin/sh

##############################################
# 보통의 경우 이부분 설정을 바꿔야 합니다.
##############################################

export ROOT_PATH=/home/sepoasoft/poabase
export JAVA_HOME=/home/sepoasoft/jdk1.8.0_191

##############################################
# 여기까지
##############################################

export BATCH_PATH=$ROOT_PATH/Poa-Package/sepoabatch/
export WEB_LIB_HOME=$ROOT_PATH/WebContent/WEB-INF/lib/
export EXTERNAL_LIB_HOME=$BATCH_PATH/lib/

export CLASSPATH=$BATCH_PATH
export CLASSPATH=$CLASSPATH:$ROOT_PATH/Poa-Package/sepoaserviceclasses/

export CLASSPATH=$CLASSPATH:$EXTERNAL_LIB_HOME/*
export CLASSPATH=$CLASSPATH:$WEB_LIB_HOME/*

export JAVA_OPTS="-Dcom.sun.management.jmxremote.port=9003 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Xms512m -Xmx512m -Dsepoa.config.file=$BATCH_PATH/config/sepoa_batch.properties"

"$JAVA_HOME/bin/java" $JAVA_OPTS $@
