Benchmarks for TDSC
===================
author: Fabien Hermenier
contact: fabien.hermenier@unice.fr

This directory contains everything is needed to replay the micro benchmarks of
BtrPlace.

Compilation instruction
-----------------------
The benchmark code is managed by maven. You must [install it](http://maven.apache.org) to compile the benchmark.
Once install, go it this directory and type:

   $ mvn assembly:assembly

Once compiled and assembled, the distribution `benchmarks-tdsc-VERSION.tar.gz` is available inside the `target`
repository

Running the benchmarks
----------------------

You must first get a workload and uncompress it into the benchmark folder.
The workload used by the paper is available at this [address](http://btrp.inria.fr/tdsc/workload.tar.bz2).


The script `dispatcher.sh` is available to spread the RPs in a workload among slaves while the script `handler.sh`
is used by the slave to process the workload. Launch them directly to get the options.

If this benchmark is run on the Grid'5000 platform, the script `bench-g5k.sh` can be used on a master node to process
the workload automatically. The following command run the benchmark related to the filter optimization. It uses
the workload inside `wkld-tdsc` and stores the result inside the folder `output`. The other benchmarks are _constraints_,
_parts_, _availability_.

   $ ./bench-g5k.sh filter wkld-tdsc output

Exploiting the benchmarks
-------------------------

Inside the output folder, Each sub-folder contains the resulting reconfiguration plans and a file called `result.data`
that aggregates the results. To compress only the benchmark results:

  $ tar cfz output-results.tar.gz output/*/result.data

To generate the datafiles that are used to generate the graphs. Use the following command. `datafiles` will be the folder
where the files will be stored.

  $ ./datafiles.sh output datafiles

Finally, to generate the graphs, use the following command. `graphs` will be the folder where the graphs will be stored.
Graphs are generated using [R](http://www.r-project.org/)

  $ ./graphs.sh datafiles graphs

