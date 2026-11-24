@echo off
echo crontab is running....

SETLOCAL

rem ##############################################
rem # 보통의 경우 이부분 설정을 바꿔야 합니다.
rem ##############################################
set ROOT_PATH=C:\sepoa\applications\poaseal_cbo\013\CBO-Package
set JAVA_HOME=C:\sepoa\jdk1.8.0_202
set WEB_HOME=C:\sepoa\applications\WebContent
set CBO_PATH=C:\sepoa\applications\poaseal_cbo\013\CBO-Package

rem ##############################################
rem # 여기까지
rem ##############################################

set BATCH_PATH=%CBO_PATH%\sepoabatch
set BATCH_LIB_HOME=%BATCH_PATH%\lib
set WEB_LIB_HOME=%WEB_HOME%\WEB-INF\lib

set CLASSPATH=%BATCH_PATH%
set CLASSPATH=%CLASSPATH%;%CBO_PATH%\sepoaserviceclasses
set CLASSPATH=%CLASSPATH%;%BATCH_LIB_HOME%\*
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\*
"%JAVA_HOME%\bin\java" -Xms512m -Xmx512m -Dsepoa.config.file=%BATCH_PATH%\config\sepoa_batch.properties org.jcrontab.Jcrontab %BATCH_PATH%\config\jcrontab.properties

ENDLOCAL
