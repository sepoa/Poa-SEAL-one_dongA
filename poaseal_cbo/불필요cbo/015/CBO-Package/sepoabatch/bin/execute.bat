@echo off

SETLOCAL

rem ##############################################
rem # 보통의 경우 이부분 설정을 바꿔야 합니다.
rem ##############################################
set ROOT_PATH=C:\sepoa\applications\poaseal\Poa-Package
set JAVA_HOME=C:\sepoa\jdk1.8.0_202
set WEB_HOME=C:\sepoa\applications\WebContent

rem ##############################################
rem # 여기까지
rem ##############################################

set BATCH_PATH=%ROOT_PATH%\sepoabatch
set BATCH_LIB_HOME=%BATCH_PATH%\lib
set WEB_LIB_HOME=%WEB_HOME%\WEB-INF\lib

set CLASSPATH=%WEB_HOME%\WEB-INF\classes
set CLASSPATH=%CLASSPATH%;%ROOT_PATH%\sepoaserviceclasses
set CLASSPATH=%CLASSPATH%;%BATCH_LIB_HOME%\*
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\*

"%JAVA_HOME%\bin\java" -Dcom.sun.management.jmxremote.port=9003 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Xms256m -Xmx256m -Dsepoa.config.file=%BATCH_PATH%\config\sepoa_batch.properties %*

ENDLOCAL