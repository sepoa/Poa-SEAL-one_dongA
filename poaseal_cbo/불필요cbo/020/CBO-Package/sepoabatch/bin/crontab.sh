echo -e crontab is running....
##############################################
# 보통의 경우 이부분 설정을 바꿔야 합니다.
##############################################

export ROOT_PATH=/apps/applications/poaseal/Poa-Package
export JAVA_HOME=/apps/tomcat/java/jdk1.8.0_202
export WEB_HOME=/apps/tomcat/source/tomcat1/ROOT
export CBO_PATH=/apps/applications/poaseal_cbo/020/CBO-Package

##############################################
# 여기까지
##############################################

export BATCH_PATH=$CBO_PATH/sepoabatch/
export WEB_LIB_HOME=$WEB_HOME/WEB-INF/lib/
export EXTERNAL_LIB_HOME=$BATCH_PATH/lib/

export CLASSPATH=$BATCH_PATH
#export CLASSPATH=$CLASSPATH:$ROOT_PATH/sepoaserviceclasses/
export CLASSPATH=$CLASSPATH:$CBO_PATH/sepoaserviceclasses/

export CLASSPATH=$CLASSPATH:$EXTERNAL_LIB_HOME/*
export CLASSPATH=$CLASSPATH:$WEB_LIB_HOME/*

export JAVA_OPTS="-Xms512m -Xmx512m -Dsepoa.config.file=$BATCH_PATH/config/sepoa_batch.properties"

"$JAVA_HOME/bin/java" $JAVA_OPTS org.jcrontab.Jcrontab $BATCH_PATH/config/jcrontab.properties
