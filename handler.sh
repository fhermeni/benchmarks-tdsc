#!/bin/sh

MY=`dirname $0`

JAVA_OPTS="-da -server -Xmx8G -Xms8G -Dlogback.configurationFile=$MY/config/logback.xml"
#Define the classpath
JARS=`ls $MY/jar/*.jar`
 
for JAR in $JARS; do
 CLASSPATH=$JAR:$CLASSPATH
done

java $JAVA_OPTS -cp $CLASSPATH microDSN.MicroBenchHandler $*
