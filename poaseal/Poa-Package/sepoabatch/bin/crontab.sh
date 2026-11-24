echo -e crontab is running....
##############################################
# 보통의 경우 이부분 설정을 바꿔야 합니다.
##############################################

export ROOT_PATH=/home/easyseal/applications/poaseal/Poa-Package
export JAVA_HOME=/usr/local/java1.8
export WEB_HOME=/home/easyseal/applications/WebContent

##############################################
# 여기까지
##############################################

export BATCH_PATH=$ROOT_PATH/sepoabatch/
export WEB_LIB_HOME=$WEB_HOME/WEB-INF/lib/
export EXTERNAL_LIB_HOME=$BATCH_PATH/lib/

export CLASSPATH=$BATCH_PATH
export CLASSPATH=$CLASSPATH:$ROOT_PATH/sepoaserviceclasses/

export CLASSPATH=$CLASSPATH:$EXTERNAL_LIB_HOME/*
export CLASSPATH=$CLASSPATH:$WEB_LIB_HOME/*

export JAVA_OPTS="-Dcom.sun.management.jmxremote.port=9003 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Xms512m -Xmx512m -Dsepoa.config.file=$BATCH_PATH/config/sepoa_batch.properties"

"$JAVA_HOME/bin/java" $JAVA_OPTS org.jcrontab.Jcrontab $BATCH_PATH/config/jcrontab.properties
