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
The workload used by the paper is available at this [address](https://github.com/downloads/fhermeni/benchmarks-tdsc/wkld-tdsc.tar.bz2).


The script `dispatcher.sh` is available to spread the RPs in a workload among slaves while the script `handler.sh`
is used by the slaves to process the workload. Launch them directly to get their options.

By default, the benchmarks is made for being used over the Grid'5000 platform. The script that is dedicated to
that is `g5k-process.sh`. It is used by the `bench-*` scripts. To adapt the execution to another environment,
create a script similar to `g5k-process.sh` and declare it into `bench-ratio-single.sh`. `local-process.sh`
can be used to process the workload on a single node.

The following command run all the benchmarks. It uses
the workload inside `wkld-tdsc` and stores the result inside the folder `output`.

   $ ./bench-all.sh  wkld-tdsc output

Exploiting the benchmarks results
-------------------------


Inside the output folder, Each sub-folder contains the resulting reconfiguration plans and a file called `result.data`
that aggregates the results. To compress only the benchmark results:

  $ tar cfz output-results.tar.gz output/*/result.data

To generate a HTML report of the benchmarks, Use the following command to browse every subfolder and generate
the HTML file `output.html`

  $ ./report-HTML.pl output/* > output.html

To generate the datafiles that are used to generate the graphs. Use the following command. `datafiles` will be the folder
where the files will be stored.

  $ ./make_datafiles.sh output datafiles

Finally, to generate the graphs, use the following command. `graphs` will be the folder where the graphs will be stored.
Graphs are generated using [R](http://www.r-project.org/)

  $ ./graphs.sh datafiles graphs

