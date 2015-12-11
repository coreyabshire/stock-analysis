#!/bin/sh

BASE_DIR=/home/supun/dev/projects/dsspidal/data/C2004_2014
GLOBAL_VECS=$BASE_DIR/global_vectors
GLOBAL=$BASE_DIR/global
ORIGINAL_STOCK_FILE=$GLOBAL/2004_2014.csv
#CAT_FILE=$BASE_DIR/all_companylist.csv
CAT_FILE=$BASE_DIR/histogram
GLOBAL_POINTS=$BASE_DIR/w_g_points
CONT_VECS=$BASE_DIR/vectors
CONT_POINTS=$BASE_DIR/w_points
CONT_COMMON_POINTS=$BASE_DIR/w_common_points
HIST_DIR=$BASE_DIR/histogram

mkdir -p $CONT_COMMON_POINTS

# generate the common points
# --------------------------
java -cp mpi/target/stocks-1.0-ompi1.8.1-jar-with-dependencies.jar PointTransformer -g $GLOBAL_VECS/2004_2014.csv -gp $GLOBAL_POINTS/2004_2014.txt -v $CONT_VECS -p $CONT_POINTS -d $CONT_COMMON_POINTS
# generate histogram
# --------------------
java -cp mpi/target/stocks-1.0-ompi1.8.1-jar-with-dependencies.jar Histogram -v $CONT_VECS -s $ORIGINAL_STOCK_FILE -d $HIST_DIR -b 10
# rotate the points
# ******************
MANXCAT_JAR=/home/supun/dev/projects/dsspidal/rotate/target/mdsaschisq-1.0-ompi1.8.1-jar-with-dependencies.jar

ROTATE_POINTS=$CONT_COMMON_POINTS/*
ROTATE_POINT_DIR=$CONT_COMMON_POINTS
ROTATE_BASE_FILE=$CONT_COMMON_POINTS/2004_2014.csv
ROTATE_OUT=$BASE_DIR/W_rotate
ROTATE_CONTROL=$ROTATE_OUT/w_rotate_control
FULL_POINTS=$CONT_POINTS

mkdir -p $ROTATE_OUT
mkdir -p $ROTATE_CONTROL

for f in $ROTATE_POINTS
do
  common_filename="${f##*/}"
  common_filename_ext="${common_filename%.*}"
  common_file=$CONT_COMMON_POINTS/$common_filename
  echo 'common file' $common_file
  no_of_common_lines=`sed -n '$=' $common_file`
  echo $no_of_common_lines

  ext='.txt'
  full_file=$FULL_POINTS/$common_filename_ext$ext
  echo 'full file' $full_file
  no_of_full_lines=`sed -n '$=' $full_file`
  echo $no_of_full_lines

  full='full'
  java -DBaseResultDirectoryName=$ROTATE_OUT \
  -DControlDirectoryName=$ROTATE_CONTROL \
  -DInitializationFileName=$ROTATE_BASE_FILE -DReducedVectorOutputFileName=$ROTATE_OUT/$common_filename_ext$ext \
  -DRotationLabelsFileName=$common_file \
  -DfinalRotationFileName=$full_file \
  -DfinalRotationPointCount=$no_of_full_lines \
  -DDataPoints=$no_of_common_lines \
  -cp $MANXCAT_JAR salsa.mdsaschisq.ManxcatCentral -c config.properties -n 1 -t 1
done

# apply labels to points
# ------------------------
# copy the fully rotated files to directory
FINAL_ROTATE=$BASE_DIR/w_rotate_final
mkdir -p $FINAL_ROTATE
cp $ROTATE_OUT/*full.txt $FINAL_ROTATE
dir=`pwd`
cd $FINAL_ROTATE
for i in SIMPLE*
do
    mv "$i" "`echo $i | sed 's/SIMPLE//'`"
done
for i in *full.txt
do
    mv "$i" "`echo $i | sed 's/full\.txt//'`"
done
cd $dir
LABEL_OUT=$BASE_DIR/w_label_points_hist
mkdir -p $LABEL_OUT

java -cp mpi/target/stocks-1.0-ompi1.8.1-jar-with-dependencies.jar LabelApply \
-v $CONT_VECS \
-p $FINAL_ROTATE \
-d $LABEL_OUT \
-o $ORIGINAL_STOCK_FILE \
-s $HIST_DIR -h