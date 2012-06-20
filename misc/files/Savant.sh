#!/bin/sh

dir=`dirname $0`
cd $dir
java -jar -Xmx4096m Savant.jar
