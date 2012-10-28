#!/bin/sh
#Script to reformat the results before generating the graphs.
EXE="./reformat.pl"
TICS="15,20,25,30"
if [ $# -ne 2 ]; then
    echo "Usage: $0 raw summed"
    echo "raw: directory containing the benchmarks result"
    echo "summed: directory to store the result files"
    exit 1
fi
IN=$1
OUT=$2
mkdir -p $OUT > /dev/null

#Filter run
DATA=""
for set in li nr; do
    DATA="$DATA -d $lbl" #The axe label
    for r in $RATIO; do #The datas
	DATA="$DATA $IN/r${RATIO}c0p5000-$set"
    done
done

$EXE solvDuration+generation -xtics VMs $TICS $DATA > $OUT/rebuild-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/rebuild-apply.data
$EXE nbStarts+nbRelocations+nbMigrations -xtics VMS $TICS $DATA > $OUT/rebuild-actions.data

#Placement constraints
DATA=""
for set in li nr; do
    for lbl in $set-0 $set-33 $set-66 $set-100; do
	DATA="$DATA -d $lbl" #The axe label
	for r in $RATIO; do #The datas
	    DATA="$DATA $IN/r${RATIO}c${PCT}p5000-$set"
	done
    done
done

$EXE solvDuration+generation -xtics VMs $TICS $DATA > $OUT/constraints-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/constraints-apply.data
$EXE nbStarts+nbRelocations+nbMigrations -xtics VMS $TICS $DATA > $OUT/constraints-actions.data

#Partitions
DATA=""
for set in li nr; do
    for part in 5000 2500 1000 500 250; do
	DATA="$DATA -d $set-$part" #The axe label
	for r in $RATIO; do #The datas
	    DATA="$DATA $IN/r${RATIO}c100p$part-$set"
	done
    done
done

$EXE solvDuration+generation -xtics Part 5000,2500,1000,500250 $DATA > $OUT/partitions-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/partitions-apply.data
