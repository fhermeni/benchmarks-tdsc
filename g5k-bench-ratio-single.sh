#!/bin/sh
#Launch a single bench for a given consolidation ratio

prg=`dirname $0`

if [ $# -ne 4 ]; then
    echo "Usage: $0 workload_folder ratio type output_folder"
    exit 1
fi

FOLDER=$1
RATIO=$2
TYPE=$3
OUTPUT=$4

INPUT=${FOLDER}/r${RATIO}
if [ ! -d ${INPUT} ]; then
    echo "Unkown directory '${INPUT}'";
    exit 1
fi

echo "--- Benching with a consolidation ratio of ${RATIO}:6 ---"
case $TYPE in
    rebuild)
	echo "- Without the filter optimization -"
	for SET in nr li; do
	    o="$OUTPUT/r${RATIO}c0p5000-${SET}-rebuild"
	    ./process.sh ${INPUT}/${SET} ${INPUT}/c0p5000  ${FOLDER}/entropy-rebuild.properties $o || exit 1
	done
	;;
    constraints)
	echo "- Impact of constraints -"
	for PCT in 0 33 66 100; do
            for SET in nr li; do
		o="$OUTPUT/r${RATIO}c${PCT}p5000-${SET}"
		./process.sh ${INPUT}/${SET} ${INPUT}/c${PCT}p5000 $FOLDER/entropy-repair.properties $o || exit 1   
	    done
	done
	;;
    partitions)
	echo "- Impact of partitions size -"
	for SIZE in 250 500 1000 2500; do  #5,000 has been made before
	    for SET in nr li; do
		o="$OUTPUT/r${RATIO}c100p${SIZE}-${SET}"
		./process.sh ${INPUT}/${SET} $FOLDER/c100p${SIZE} $FOLDER/entropy-part.properties $o || exit 1	       
	    done
	done
	;;
    *) echo "Unknown bench type '${TYPE}'"
esac
