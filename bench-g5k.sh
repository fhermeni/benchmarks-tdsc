#!/bin/sh

if [ -f $OAR_NODEFILE ]; then
        WORKERS="/tmp/$(basename $0).$$.tmp"
        cat $OAR_NODEFILE|sort|uniq|tail -n +2 > $WORKERS
else
        echo "This script must be launched from the main node that contains the variable \$OAR_NODEFILE"
        exit 1
fi

if [ $1 = "clean" ]; then
    taktuk --connector /usr/bin/oarsh -o connector -o status -f $1 \
    broadcast exec [ "killall java" ] 2>&1
    rm -rf ${WORKERS}
    exit 0
fi

if [ $# -ne 3 ]; then
    echo "Usage: $0 workload_folder ratio output_folder"
    rm -rf ${WORKERS}
    exit 1
fi


ROOT=`pwd`
PORT=8080
MASTER=`hostname -s`
FOLDER=$1
RATIO=$2
OUTPUT=$3

#set -x
prg=`dirname $0`
function runJob {
    $prg/dispatcher.sh $1 &
echo "Dispatcher launched"
    sleep 5
echo "Starting workers"
   taktuk --connector /usr/bin/oarsh -o connector -o status -f $WORKERS \
broadcast exec [ "cd $ROOT/$prg; ./handler.sh -s $MASTER:$PORT" ] 2>&1 > taktuk.log
echo "Workers are terminated"
 sleep 10
echo "Cleaning"
    killall java
}

INPUT=${FOLDER}/${RATIO}
if [ ! -d ${INPUT} ]; then
    echo "Unkown directory '${INPUT}'";
    exit 1
fi

echo "--- Benching with a consolidation ratio of ${RATIO}:6 ---"
echo "- Rebuild -"
for SET in nr li; do
    o="$OUTPUT/r${RATIO}c${PCT}p5000-${SET}-rebuild"
    echo "set=${SET}; output=$o"
    if [ -d $o ]; then
       	echo "Skipping"
    else 
	runJob "-p $PORT -cfgs ${INPUT}/${SET} -vjobs $FOLDER/c0p5000  -props $FOLDER/entropy-rebuild.properties -o $o"
    fi
done

echo "- Impact of constraints -"
for PCT in 0 33 66 100; do
        for SET in nr li; do
	    o="$OUTPUT/r${RATIO}c${PCT}p5000-${SET}"
	    echo "percentage=${PCT}; set=${SET}; output=$o"
	    if [ -d $o ]; then
		echo "Skipping"
	    else 
		runJob "-p $PORT -cfgs ${INPUT}/${SET} -vjobs $FOLDER/c${PCT}p${SIZE}  -props $FOLDER/entropy-repair.properties -o $o"
	    fi
	done
done
echo "- Impact of partitions size -"
for SIZE in 250 500 1000 2500; do  #5,000 has been made before
	for SET in nr li; do
	    o="$OUTPUT/r${RATIO}c100p${SIZE}-${SET}"
	    echo "part size=${SIZE}; set=${SET}; output=$o"
	    if [ -d $o ]; then
		echo "Skipping"
	    else 
		runJob "-p $PORT -cfgs ${INPUT}/${SET} -vjobs $FOLDER/c100p${SIZE}  -props $FOLDER/entropy-part.properties -o $o"
	    fi
	done
done

rm -rf ${WORKERS}