@echo off

set "SERVICE_ID=Poa-CRON"

set INSTALL_DIR=%cd%
set INSTALLER=%INSTALL_DIR%\prunsrv.exe

%INSTALLER% //DS//%SERVICE_ID%