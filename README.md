Benchmarks for TDSC
===================
* author: Fabien Hermenier
* contact: fabien.hermenier@unice.fr

This directory contains everything is needed to replay micro benchmarks of BtrPlace.

Compilation instruction
-----------------------

The application is managed by maven. You must [install it](http://maven.apache.org) to compile the benchmark.
Once install, go it this directory and type:

    $ mvn assembly:assembly

The distribution `benchmarks-tdsc-VERSION.tar.gz` will be available in the `target` directory.

Running benchmarks
----------------------

This archive does not contain a workload but you can download the default one
at this [address](https://github.com/downloads/fhermeni/benchmarks-tdsc/wkld-tdsc.tar.bz2).
Once downloaded, un-compress it inside the application root directory.

Basic scripts are available to process each part of the workload.
The script `dispatcher.sh` read the RPs on the workload and will distribute them on the slaves running the `handler.sh`
application. Launch them to get their parameters.

High-level scripts (`bench-all.sh`, `bench-ratio.sh`, and `bench-ratio-single.sh`) made the processing of the workload
easy. They all rely on a `*-process.sh` script to distribute the workload. By default, `g5k-process.sh` is used to run
process the workload on the Grid'5000 platforms. `local-process.sh` allows the execution of the benchmark on a single
machine. To set the process script, edit `bench-ratio-single.sh`.

The following command run all the benchmarks. It uses
the workload inside `wkld-tdsc` and stores the result inside the folder `output`.

    $ ./bench-all.sh  wkld-tdsc output

Executing this benchmark will take hours. Especially the benchmark without the filter option. Edit the scripts to adapt
the benchmark to your needs.

Inside the output folder, Each sub-folder contains the resulting reconfiguration plans and a file called `result.data`
that aggregates the results.

Exploiting the benchmarks results
-------------------------

 To compress only the benchmark results:

    $ tar cfz output-results.tar.gz output/*/result.data

To generate a HTML report of the benchmarks, use the following command to generate the `output.html` file:

    $ ./report-HTML.pl output/* > output.html

To store in the directory `datafiles` the data that will be used to generate graphics:

    $ ./make_datafiles.sh output datafiles

To generate the graphics inside the `graphs` folder, you have to install [R](http://www.r-project.org/)
then run the following command:

    $ ./graphs.sh datafiles graphs


Additional tools
------------------------

* to generate a workload, use `generate.sh`
* to compute the number of applications impacted by a load increase in a given workload, use `impacted.sh`



