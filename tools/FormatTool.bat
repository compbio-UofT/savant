REM Expects to be run in the same directory
SET JAVAROOT=.
SET CLASSPATH=%JAVAROOT%\Savant.jar;%JAVAROOT%\commons-logging-1.1.1.jar;%JAVAROOT%\sam-1.31.jar
SHIFT
java -cp $CLASSPATH savant.tools.FormatTool %*
