#!/bin/sh
#Make the graphs from the datafile and the R scripts inside the 'R' directory
if [ $# -ne 2 ]; then
    echo "Usage: $0 datafile_folders output"
    echo "datafile_folders: directory containing the datafiles"
    echo "output: directory where graphs will be stored"
    exit 1
fi
in=$1
out=$2

mkdir -p $out > /dev/null
for r in filter-duration filter-actions NR-cstr-duration LI-cstr-duration part-duration availability solveImpact alteredApps; do
    ./R/$r.R $in $out
done