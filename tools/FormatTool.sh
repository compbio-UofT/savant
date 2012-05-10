#!/bin/bash
JAVAROOT=.
if [[ `uname` == *Darwin* ]]; then
  JAVAROOT=/Applications/Savant.app/Contents/Resources/Java
fi
CLASSPATH=$JAVAROOT/Savant.jar:$JAVAROOT/commons-logging-1.1.1.jar:$JAVAROOT/log4j-1.2.16.jar:$JAVAROOT/sam-1.61.jar
java -Xmx4096m -cp $CLASSPATH savant.tools.FormatTool $*
