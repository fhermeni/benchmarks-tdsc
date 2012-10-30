#!/bin/sh

MY=`dirname $0`

JAVA_OPTS="-da -server -Xmx4G -Xms4G -Dlogback.configurationFile=$MY/config/logback.xml"
#Define the classpath
JARS=`ls $MY/jar/*.jar`
 
for JAR in $JARS; do
 CLASSPATH=$JAR:$CLASSPATH
done

if [ $# -ne 2 ]; then
    echo "Usage: $0 nb_instances output"
    echo "nb_instance: the number of RPs per datapoint"
    echo "output: the output directory"
    exit 1
fi
java $JAVA_OPTS -cp $CLASSPATH microDSN.TDSC $*
