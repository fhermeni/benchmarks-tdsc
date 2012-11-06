#!/bin/sh
#Script to reformat the results before generating the graphs.
EXE="./reformat.pl"
TICS="15,20,25,30"
RATIOS="3 4 5 6"
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
    DATA="$DATA -d $set-repair" #The axis label
    for r in $RATIOS; do #The datas
	DATA="$DATA $IN/r${r}c0p5000-$set"
    done
done
$EXE solvDuration -xtics VMs $TICS $DATA > $OUT/filter-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/filter-apply.data
$EXE nbStarts+nbRelocations+nbMigrations -xtics VMS $TICS $DATA > $OUT/filter-actions.data
DATA=""
for set in li nr; do
    DATA="$DATA -d $set-rebuild" #The axis label
	DATA="$DATA $IN/r3c0p5000-$set-rebuild"
done
$EXE solvDuration -xtics VMs 15 $DATA > $OUT/wofilter-duration.data
$EXE apply -xtics VMs 15 $DATA > $OUT/wofilter-apply.data
$EXE nbStarts+nbRelocations+nbMigrations -xtics VMs 15 $DATA > $OUT/wofilter-actions.data

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

$EXE solvDuration -xtics VMs $TICS $DATA > $OUT/constraints-duration.data
$EXE apply -xtics VMs $TICS $DATA > $OUT/constraints-apply.data
$EXE nbStarts+nbRelocations+nbMigrations -xtics VMS $TICS $DATA > $OUT/constraints-actions.data

#Partitions
DATA=""
for r in $RATIOS; do
for set in li nr; do
    DATA="$DATA -d $set-r${r}" #The axis label
    for part in 5000 2500 1000 500 250; do
	DATA="$DATA $IN/r${r}c100p$part-$set"
    done
done
done
$EXE solvDuration -xtics Part 5000,2500,1000,500,250 $TICS $DATA > $OUT/partitions-duration.data
$EXE apply -xtics Part 5000,2500,1000,500,250 $DATA > $OUT/partitions-apply.data

