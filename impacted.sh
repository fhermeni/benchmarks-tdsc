#!/bin/sh

MY=`dirname $0`

JAVA_OPTS="-da -server -Xmx4G -Xms4G -Dlogback.configurationFile=$MY/config/logback.xml"
#Define the classpath
JARS=`ls $MY/jar/*.jar`
 
for JAR in $JARS; do
 CLASSPATH=$JAR:$CLASSPATH
done

if [ $# -ne 1 ]; then
    echo "Usage: $0 path"
    echo "path: the directory containing the workload"
    exit 1
fi
java $JAVA_OPTS -cp $CLASSPATH microDSN.ImpactedApplications $*
