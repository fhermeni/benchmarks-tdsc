#!/bin/sh


MY=`dirname $0`

JAVA_OPTS="-Dlogback.configurationFile=$MY/config/logback.xml"
#Define the classpath
JARS=`ls $MY/jar/*.jar`
 
for JAR in $JARS; do
 CLASSPATH=$JAR:$CLASSPATH
done

java $JAVA_OPTS -cp $CLASSPATH microDSN.MicroBenchDispatcher $*
