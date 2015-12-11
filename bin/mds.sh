#!/bin/bash

if [ $# -eq 0 ]
  then
    echo "Directory must be specified as argument"
    exit 1
fi

#uuid=$2
#email=$3
#account=$4
#noglobal=$5

# change these to change the directories
BASE_DIR=$1
PREPROC_DIR=$BASE_DIR/preproc

# no need to change the below lines
YEARLY_PREPROC_DIR=$PREPROC_DIR/yearly
GLOBAL_PREPROC_DIR=$PREPROC_DIR/global

MDS_DIR=$BASE_DIR/mds/unweighted
YEARLY_MDS_DIR=$MDS_DIR/yearly
GLOBAL_MDS_DIR=$MDS_DIR/global

POINTS_DIR=$YEARLY_MDS_DIR
MATRIX_DIR=$YEARLY_PREPROC_DIR/distances
VECTOR_DIR=$YEARLY_PREPROC_DIR/vectors
DAMDS_SUMMARY_DIR=$YEARLY_MDS_DIR/summary

GLOBAL_POINTS_DIR=$GLOBAL_MDS_DIR
GLOBAL_MATRIX_DIR=$GLOBAL_PREPROC_DIR/distances
GLOBAL_VECTORS_DIR=$GLOBAL_PREPROC_DIR/vectors
GLOBAL_DAMDS_SUMMARY_DIR=$GLOBAL_MDS_DIR/summary

mkdir -p $DAMDS_SUMMARY_DIR
mkdir -p $GLOBAL_DAMDS_SUMMARY_DIR

MATRIX_FILES=$MATRIX_DIR/*
VECTOR_BASE=$VECTOR_DIR/
for f in $MATRIX_FILES
do
  filename="${f##*/}"
  filenameWithoutExtension="${filename%.*}"
  if [ ! -f $POINTS_DIR/${filenameWithoutExtension}.txt ]
  then
    echo $filename
    vf=$VECTOR_BASE$filename
    echo $vf
    no_of_lines=`sed -n '$=' $vf`
    echo $no_of_lines
    #sbatch --job-name $uuid --mail-user $email --account $account internal_mds.sh $f $no_of_lines $POINTS_DIR/$filenameWithoutExtension $DAMDS_SUMMARY_DIR/$filenameWithoutExtension
    echo sbatch internal_mds.sh $f $no_of_lines $POINTS_DIR/$filenameWithoutExtension $DAMDS_SUMMARY_DIR/$filenameWithoutExtension >> commands.sh
    sbatch internal_mds.sh $f $no_of_lines $POINTS_DIR/$filenameWithoutExtension $DAMDS_SUMMARY_DIR/$filenameWithoutExtension
  else
    echo $POINTS_DIR/${filenameWithoutExtension}.txt already exists, skipping
  fi
done

#if [ "$noglobal" != true ]
#then
MATRIX_FILES=$GLOBAL_MATRIX_DIR/*
VECTOR_BASE=$GLOBAL_VECTORS_DIR/
for f in $MATRIX_FILES
do
  filename="${f##*/}"
  filenameWithoutExtension="${filename%.*}"
  if [ ! -f $GLOBAL_POINTS_DIR/${filenameWithoutExtension}.txt ]
  then
    echo $filename
    vf=$VECTOR_BASE$filename
    echo $vf
    no_of_lines=`sed -n '$=' $vf`
    echo $no_of_lines
    #sbatch --job-name $uuid --mail-user $email --account $account internal_mds.sh $f $no_of_lines $GLOBAL_POINTS_DIR/$filenameWithoutExtension $GLOBAL_DAMDS_SUMMARY/$filenameWithoutExtension
    echo sbatch internal_mds.sh $f $no_of_lines $GLOBAL_POINTS_DIR/$filenameWithoutExtension $GLOBAL_DAMDS_SUMMARY/$filenameWithoutExtension >> commands.sh
    sbatch internal_mds.sh $f $no_of_lines $GLOBAL_POINTS_DIR/$filenameWithoutExtension $GLOBAL_DAMDS_SUMMARY/$filenameWithoutExtension
  else
    echo $GLOBAL_POINTS_DIR/${filenameWithoutExtension}.txt already exists, skipping
  fi
done
#fi
