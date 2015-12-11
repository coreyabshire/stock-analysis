#!/bin/bash

which java
java -version

#find /usr -name "libibverbs.so"
#find /opt -name "libibverbs.so"

echo LD_LIBRARY_PATH $LD_LIBRARY_PATH
echo PATH $PATH
echo BUILD $BUILD

LD_LIBRARY_PATH=/N/u/dcabshir/opt/openmpi-1.8.1/build/lib:/usr/local/lib:/opt/torque-2.5.5/lib:
export LD_LIBRARY_PATH
echo LD_LIBRARY_PATH $LD_LIBRARY_PATH

PATH=/N/u/dcabshir/opt/openmpi-1.8.1/build/bin:/N/u/dcabshir/opt/apache-maven-3.3.9/bin:/N/u/dcabshir/opt/jdk1.8.0/bin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin:/opt/slurm/bin:/opt/slurm/sbin:/N/u/dcabshir/bin
export PATH
echo PATH $PATH

set >env_`hostname`.txt

echo max memory
ulimit -l
ulimit -l unlimited
echo new max memory
ulimit -l

