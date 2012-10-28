#!/bin/sh

PORT=8080
MASTER="127.0.0.1"
prg=`dirname $0`
ROOT=`pwd`

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

if [ ${GOOD} -eq 0 ]; then
    rm -rf ${WORKERS}
    exit 1
fi

$prg/dispatcher.sh -p $PORT -cfgs ${SET} -vjobs ${VJOBS} -props ${PROP} -o ${OUTPUT} &
echo "Dispatcher launched"
echo "Starting workers"
./handler.sh -s $MASTER:$PORT 2>&1 >> log.txt
echo "Workers are terminated"
echo "Cleaning"
killall java 2>&1 > /dev/null
exit 0