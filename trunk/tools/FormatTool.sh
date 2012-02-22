#!/bin/bash
JAVAROOT=.
if [[ `uname` == *Darwin* ]]; then
  JAVAROOT=/Applications/Savant.app/Contents/Resources/Java
fi
CLASSPATH=$JAVAROOT/Savant.jar:$JAVAROOT/commons-logging-1.1.1.jar:$JAVAROOT/sam-1.31.jar
java -cp $CLASSPATH savant.tools.FormatTool $*
