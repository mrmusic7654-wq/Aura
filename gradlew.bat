@echo off
setlocal

set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

if defined JAVA_HOME (
    set JAVA=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA=java.exe
)

%JAVA% %DEFAULT_JVM_OPTS% -cp "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
