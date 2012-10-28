#!/bin/sh
if [ $# -ne 2 ]; then
    echo "Usage: $0 workload_folder output_folder"
    exit 1
fi

for ratio in 3 4 5 6; do
    ./g5k-bench-ratio.sh $1 ${ratio} $2 || exit 1
done
