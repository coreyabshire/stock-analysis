#!/bin/bash

BASE_DIR=/N/u/dcabshir/data/wrds/crsp
POINTS_DIR=$BASE_DIR/mds/unweighted/yearly

echo $1

filenameWithoutExtension=$1

echo $filenameWithoutExtension

echo $POINTS_DIR/${filenameWithoutExtension}.txt

if [ ! -f $POINTS_DIR/${filenameWithoutExtension}.txt ]
then
    echo "its not"
else
    echo "its there"
fi


