@echo off

setlocal
set SERVICE_NAME=Poa-App-CRON
set HOME_PATH=C:\sepoa\applications\poaseal_cbo\013\CBO-Package\sepoabatch\bin
set PR_INSTALL=%HOME_PATH%\prunsrv.exe
set CBO_PATH=C:\sepoa\applications\poaseal_cbo\013\CBO-Package

@REM Service Log Configuration
set PR_LOGPREFIX=%SERVICE_NAME%
set PR_LOGPATH=C:\sepoa\applications\logs\batch
set PR_STDOUTPUT=auto
set PR_STDERROR=auto
set PR_LOGLEVEL=Debug

@REM Path to Java Installation
set JAVA_HOME=C:\sepoa\jdk1.8.0_202
set PR_JVM=%JAVA_HOME%\jre\bin\server\jvm.dll

@REM Path to Java classpath
set BATCH_PATH=%CBO_PATH%\sepoabatch
set BATCH_LIB_HOME=%HOME_PATH%\..\lib
set WEB_HOME=C:\sepoa\applications\WebContent
set WEB_LIB_HOME=%WEB_HOME%\WEB-INF\lib

set CLASSPATH=%WEB_HOME%\WEB-INF\classes
set CLASSPATH=%CLASSPATH%;%HOME_PATH%\..\..\sepoaserviceclasses
set CLASSPATH=%CLASSPATH%;%BATCH_LIB_HOME%\*
set CLASSPATH=%CLASSPATH%;%WEB_LIB_HOME%\*
set PR_CLASSPATH=%CLASSPATH%

@REM JVM Configuration
set PR_JVMMS=512
set PR_JVMMX=512
set PR_JVMSS=4000
set PR_JVMOPTIONS=
set PR_JVMOPTIONS=%PR_JVMOPTIONS%-Djava.io.tmpdir=%HOME_PATH%\TEMP
set PR_JVMOPTIONS=%PR_JVMOPTIONS%;-Dsepoa.config.file=%HOME_PATH%\..\config\sepoa_batch.properties
set PR_JVMOPTIONS=%PR_JVMOPTIONS%;-Xms512m
set PR_JVMOPTIONS=%PR_JVMOPTIONS%;-Xmx512m
set PR_JVMOPTIONS=%PR_JVMOPTIONS%;-Dsepoa.log.prefix=Poa-CRON
set PR_JVMOPTIONS=%PR_JVMOPTIONS%;-Dcom.sun.management.jmxremote.port=9003
set PR_JVMOPTIONS=%PR_JVMOPTIONS%;-Dcom.sun.management.jmxremote.ssl=false
set PR_JVMOPTIONS=%PR_JVMOPTIONS%;-Dcom.sun.management.jmxremote.authenticate=false

@REM Startup Configuration
set PR_STARTUP=auto
set PR_STARTMODE=jvm
set PR_STARTCLASS=org.jcrontab.Jcrontab
set PR_STARTPARAMS=%HOME_PATH%\..\config\jcrontab.properties

@REM Shutdown Configuration
set PR_STOPCLASS=org.jcrontab.Jcrontab
set PR_STOPPARAMS=stop

"%PR_INSTALL%" //IS/%SERVICE_NAME% ^
  --DisplayName="%SERVICE_NAME%" ^
  --Install="%PR_INSTALL%" ^
  --Startup="%PR_STARTUP%" ^
  --LogPath="%PR_LOGPATH%" ^
  --LogPrefix="%PR_LOGPREFIX%" ^
  --LogLevel="%PR_LOGLEVEL%" ^
  --StdOutput="%PR_STDOUTPUT%" ^
  --StdError="%PR_STDERROR%" ^
  --JavaHome="%JAVA_HOME%" ^
  --Jvm="%PR_JVM%" ^
  --JvmMs="%PR_JVMMS%" ^
  --JvmMx="%PR_JVMMX%" ^
  --JvmSs="%PR_JVMSS%" ^
  --JvmOptions="%PR_JVMOPTIONS%" ^
  --Classpath="%PR_CLASSPATH%" ^
  --StartMode="%PR_STARTMODE%" ^
  --StartClass="%PR_STARTCLASS%" ^
  --StartParams="%PR_STARTPARAMS%" ^
  --StopMode="%PR_STARTMODE%" ^
  --StopClass="%PR_STOPCLASS%" ^
  --StopParams="%PR_STOPPARAMS%"

endlocal