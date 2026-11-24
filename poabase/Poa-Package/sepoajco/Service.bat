@echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem ---------------------------------------------------------------------------
rem NT Service Install/Uninstall script
rem
rem Options
rem install                Install the service using Tomcat8 as service name.
rem                        Service is installed using default settings.
rem remove                 Remove the service from the System.
rem
rem name        (optional) If the second argument is present it is considered
rem                        to be new service name
rem ---------------------------------------------------------------------------

set ROOT_PATH=D:\Dev\03.WorkSpace\Poa-ACCT-SKTELINK
set JAVA_HOME=D:\Dev\02.JAVA\jdk1.8.0_72
set WEB_HOME=D:\Dev\03.WorkSpace\Poa-ACCT-SKTELINK\poabase\WebContent
set WAS_HOME=D:\Dev\04.WAS\apache-tomcat-8.0.48

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

set JVM_OPTS=-Dsepoa.log.prefix=sepoa_jco_server_ -Dsepoa.config.file=%SEPOA_PROPERTIES% sepoa.fw.jco.server.SepoaJCoServer

setlocal

set "SELF=%~dp0%service.bat"
rem Guess SVR_HOME if not defined
set "CURRENT_DIR=%cd%"
set "SVR_HOME=%cd%"
if exist "%SVR_HOME%\procrun.exe" goto okHome
rem CD to the upper dir
cd ..
set "SVR_HOME=%cd%"
:gotHome
if exist "%SVR_HOME%\procrun.exe" goto okHome
echo The procrun.exe was not found...
echo The SVR_HOME environment variable is not defined correctly.
echo This environment variable is needed to run this program
goto end
:okHome
rem Make sure prerequisite environment variables are set
if not "%JAVA_HOME%" == "" goto gotJdkHome
if not "%JRE_HOME%" == "" goto gotJreHome
echo Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
echo Service will try to guess them from the registry.
goto okJavaHome
:gotJreHome
if not exist "%JRE_HOME%\bin\java.exe" goto noJavaHome
goto okJavaHome
:gotJdkHome
if not exist "%JAVA_HOME%\bin\javac.exe" goto noJavaHome
rem Java 9 has a different directory structure
if exist "%JAVA_HOME%\jre\bin\java.exe" goto preJava9Layout
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if not "%JRE_HOME%" == "" goto okJavaHome
set "JRE_HOME=%JAVA_HOME%"
goto okJavaHome
:preJava9Layout
if not "%JRE_HOME%" == "" goto okJavaHome
set "JRE_HOME=%JAVA_HOME%\jre"
goto okJavaHome
:noJavaHome
echo The JAVA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
echo NB: JAVA_HOME should point to a JDK not a JRE
goto end
:okJavaHome
set "SVR_BASE=%SVR_HOME%"
:gotBase

set "EXECUTABLE=%SVR_HOME%\procrun.exe"

rem Set default Service name
set SERVICE_NAME=SepoaJCoServer
set DISPLAYNAME=SepoaJCoServer %SERVICE_NAME%

rem Java 9 no longer supports the java.endorsed.dirs
rem system property. Only try to use it if
rem JAVA_ENDORSED_DIRS was explicitly set
rem or SVR_HOME/endorsed exists.
set ENDORSED_PROP=ignore.endorsed.dirs
if "%JAVA_ENDORSED_DIRS%" == "" goto noEndorsedVar
set ENDORSED_PROP=java.endorsed.dirs
goto doneEndorsed
:noEndorsedVar
if not exist "%SVR_HOME%\endorsed" goto doneEndorsed
set ENDORSED_PROP=java.endorsed.dirs
:doneEndorsed

if "x%1x" == "xx" goto displayUsage
set SERVICE_CMD=%1
shift
if "x%1x" == "xx" goto checkServiceCmd
:checkUser
if "x%1x" == "x/userx" goto runAsUser
if "x%1x" == "x--userx" goto runAsUser
set SERVICE_NAME=%1
set DISPLAYNAME=SepoaJCoServer %1
shift
if "x%1x" == "xx" goto checkServiceCmd
goto checkUser
:runAsUser
shift
if "x%1x" == "xx" goto displayUsage
set SERVICE_USER=%1
shift
runas /env /savecred /user:%SERVICE_USER% "%COMSPEC% /K \"%SELF%\" %SERVICE_CMD% %SERVICE_NAME%"
goto end
:checkServiceCmd
if /i %SERVICE_CMD% == install goto doInstall
if /i %SERVICE_CMD% == remove goto doRemove
if /i %SERVICE_CMD% == uninstall goto doRemove
echo Unknown parameter "%SERVICE_CMD%"
:displayUsage
echo.
echo Usage: service.bat install/remove [service_name] [/user username]
goto end

:doRemove
rem Remove the service
echo Removing the service '%SERVICE_NAME%' ...
echo Using SVR_BASE:    "%SVR_BASE%"

"%EXECUTABLE%" //DS//%SERVICE_NAME% ^
    --LogPath "%SVR_BASE%\logs"
if not errorlevel 1 goto removed
echo Failed removing '%SERVICE_NAME%' service
goto end
:removed
echo The service '%SERVICE_NAME%' has been removed
goto end

:doInstall
rem Install the service
echo Installing the service '%SERVICE_NAME%' ...
echo Using SVR_HOME:    "%SVR_HOME%"
echo Using SVR_BASE:    "%SVR_BASE%"
echo Using JAVA_HOME:        "%JAVA_HOME%"
echo Using JRE_HOME:         "%JRE_HOME%"

rem Try to use the server jvm
set "JVM=%JRE_HOME%\bin\server\jvm.dll"
if exist "%JVM%" goto foundJvm
rem Try to use the client jvm
set "JVM=%JRE_HOME%\bin\client\jvm.dll"
if exist "%JVM%" goto foundJvm
echo Warning: Neither 'server' nor 'client' jvm.dll was found at JRE_HOME.
set JVM=auto
:foundJvm
echo Using JVM:              "%JVM%"

"%EXECUTABLE%" //IS//%SERVICE_NAME% ^
    --Description "SepoaJCoServer" ^
    --DisplayName "%DISPLAYNAME%" ^
    --Install "%EXECUTABLE%" ^
    --LogPath "%SVR_BASE%\logs" ^
    --StdOutput auto ^
    --StdError auto ^
    --Classpath "%CLASSPATH%" ^
    --Jvm "%JVM%" ^
    --StartMode jvm ^
    --StopMode jvm ^
    --StartPath "%SVR_HOME%" ^
    --StopPath "%SVR_HOME%" ^
    --StartClass sepoa.fw.jco.server.SepoaJCoServer ^
    --StopClass sepoa.fw.jco.server.SepoaJCoServer ^
    --StartParams start ^
    --StopParams stop ^
    --JvmOptions "%JVM_OPTS%" ^
    --JvmMs 512 ^
    --JvmMx 512
if not errorlevel 1 goto installed
echo Failed installing '%SERVICE_NAME%' service
goto end
:installed
echo The service '%SERVICE_NAME%' has been installed.

:end
cd "%CURRENT_DIR%"
