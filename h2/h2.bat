@echo off
set "dir=%~dp0"
java -cp "%dir%target\h2-2.3.239-SNAPSHOT.jar;%H2DRIVERS%;%CLASSPATH%" org.h2.tools.Console %*