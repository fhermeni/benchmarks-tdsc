#!/bin/sh
#Script to reformat the results before generating the graphs.
EXE="./reformat.pl"
TICS="15,20,25,30"
RATIOS="3 4 5"
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
    DATA="$DATA -d $lbl" #The axis label
    for r in $RATIOS; do #The datas
	DATA="$DATA $IN/r${r}c0p5000-$set"
    done
done

$EXE solvDuration+generation -xtics VMs $TICS $DATA > $OUT/rebuild-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/rebuild-apply.data
$EXE nbStarts+nbRelocations+nbMigrations -xtics VMS $TICS $DATA > $OUT/rebuild-actions.data

#Placement constraints
DATA=""
for set in li nr; do
    for PCT in 0 33 66 100; do
	DATA="$DATA -d $set-$PCT" #The axis label
	for r in $RATIOS; do #The datas
		DATA="$DATA $IN/r${r}c${PCT}p5000-$set"
	done
    done
done
echo $DATA
$EXE solvDuration+generation -xtics VMs $TICS $DATA > $OUT/constraints-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/constraints-apply.data
$EXE nbStarts+nbRelocations+nbMigrations -xtics VMS $TICS $DATA > $OUT/constraints-actions.data

#Partitions
DATA=""
for set in li nr; do
    for part in 5000 2500 1000 500 250; do
	DATA="$DATA -d $set-$part" #The axis label
	for r in $RATIOS; do #The datas
	    DATA="$DATA $IN/r${r}c100p$part-$set"
	done
    done
done

$EXE solvDuration+generation -xtics VMs $TICS $DATA > $OUT/partitions-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/partitions-apply.data
