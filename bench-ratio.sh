#!/bin/sh
if [ $# -ne 3 ]; then
    echo "Usage: $0 workload_folder ratio output_folder"
    exit 1
fi

for type in rebuild constraints partitions; do
    ./bench-ratio-single.sh $1 $2 ${type} $3 || exit 1
done
