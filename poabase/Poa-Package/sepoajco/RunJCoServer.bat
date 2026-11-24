@echo off

SETLOCAL

rem ##############################################
rem # 보통의 경우 이부분 설정을 바꿔야 합니다.
rem ##############################################
set ROOT_PATH=D:\Dev\03.WorkSpace\Poa-ACCT-SKTELINK
set JAVA_HOME=D:\Dev\02.JAVA\jdk1.8.0_72
set WEB_HOME=D:\Dev\03.WorkSpace\Poa-ACCT-SKTELINK\poabase\WebContent
set WAS_HOME=D:\Dev\04.WAS\apache-tomcat-8.0.48
rem ##############################################
rem # 여기까지
rem ##############################################
set SEPOA_PROPERTIES=%ROOT_PATH%\sktl\Poa-Package\sepoaconfig\sepoa.properties 
set JCO_LIB_HOME=%ROOT_PATH%\poabase\Poa-Package\sepoajco\lib
set WEB_LIB_HOME=%WEB_HOME%\WEB-INF\lib

set CLASSPATH=
set CLASSPATH=%CLASSPATH%;%WEB_HOME%\WEB-INF\classes
set CLASSPATH=%CLASSPATH%;%WAS_HOME%\lib\servlet-api.jar
set CLASSPATH=%CLASSPATH%;%JCO_LIB_HOME%\httpclient-4.3.2.jar
set CLASSPATH=%CLASSPATH%;%JCO_LIB_HOME%\httpcore-4.3.2.jar
set CLASSPATH=%CLASSPATH%;%JCO_LIB_HOME%\httpcore-nio-4.3.2.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\sux-fw.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\sux-webservice.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\sux-web.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\sapjco3.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\sapjco3.dll
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\slf4j-api-1.7.25.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\logback-classic-1.2.3.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\logback-core-1.2.3.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\jcl-over-slf4j-1.7.25.jar
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\commons-codec-1.3.jar

set JVM_OPTS=-Dsepoa.log.prefix=sepoa_jco_server_;-Dsepoa.config.file=%SEPOA_PROPERTIES%

"%JAVA_HOME%\bin\java" -Xms256m -Xmx256m %JVM_OPTS%

ENDLOCAL