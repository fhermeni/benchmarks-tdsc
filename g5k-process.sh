#!/bin/sh

PORT=8080
MASTER=`hostname -s`
prg=`dirname $0`
ROOT=`pwd`

if [ -n "$OAR_NODEFILE" ]; then
    WORKERS="/tmp/$(basename $0).$$.tmp"
    cat $OAR_NODEFILE|sort|uniq|tail -n +2 > $WORKERS
else
    echo "This script must be launched from the node that contains the variable \$OAR_NODEFILE"
    rm -rf ${WORKERS}
    exit 1
fi

if [ $1 = "clean" ]; then
    taktuk --connector /usr/bin/oarsh -o connector -o status -f $1 \
    broadcast exec [ "killall java" ] 2>&1
fi

if [ $# -ne 4 ]; then
    echo "Usage: $0 set vjobs properties output"
    echo "set: path to the set of configuration to process (nr or li)"
    echo "vjobs: path to the script to consider"
    echo "prop: path to the properties file"
    echo "output: the output directory"
    exit 1
fi

SET=$1
VJOBS=$2
PROP=$3
OUTPUT=$4

#Sum up
GOOD=1
echo "Sets: '${SET}'; vjobs: '${VJOBS}'; properties: '${PROP}' output: '$OUTPUT'"
if [ ! -d ${SET} ]; then
    echo "sets must point to an existing directory"
    GOOD=0;
fi
 
if [ ! -d ${VJOBS} ]; then
    echo "vjobs must point to an existing directory"
    GOOD=0;
fi

if [ ! -f ${PROP} ]; then
    echo "properties must point to an existing properties file"
    GOOD=0;
fi

if [ -d ${OUTPUT} ]; then
    echo "output directory ${OUTPUT} already exists";
    GOOD=0;
fi

if [ ! ${GOOD} ]; then
    rm -rf ${WORKERS}
    exit 1
fi

$prg/dispatcher.sh -p $PORT -cfgs ${SET} -vjobs ${VJOBS} -props ${PROP} -o ${OUTPUT} &
echo "Dispatcher launched"
sleep 5
echo "Starting workers"
taktuk --connector /usr/bin/oarsh -o connector -o status -f $WORKERS \
    broadcast exec [ "cd $ROOT/$prg; ./handler.sh -s $MASTER:$PORT" ] 2>&1 > taktuk.log
echo "Workers are terminated"
sleep 5
echo "Cleaning"
killall java 2>&1 > /dev/null

rm -rf ${WORKERS}
exit 0