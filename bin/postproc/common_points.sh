#!/bin/sh

# generate the common points
# --------------------------
GLOBAL_VECS=$BASE_DIR/$GLOBAL_VEC_DIR_NAME
GLOBAL_POINTS=$BASE_DIR/$GLOBAL_POINTS_DIR_NAME
CONT_VECS=$BASE_DIR/$VECS_DIR_NAME
CONT_POINTS=$BASE_DIR/$POINTS_DIR_NAME
CONT_COMMON_POINTS=$BASE_DIR/$COMMON_POINTS_DIR_NAME
GLOBAL_CONT_COMMON_POINTS=$BASE_DIR/$GLOBAL_COMMON_POINTS_DIR_NAME

mkdir -p $CONT_COMMON_POINTS
mkdir -p $GLOBAL_CONT_COMMON_POINTS

# copy the global points
mkdir -p $BASE_DIR/$GLOBAL_FINAL_POINTS_DIR
cp -r $GLOBAL_POINTS/* $BASE_DIR/$GLOBAL_FINAL_POINTS_DIR

java -cp ../mpi/target/stocks-1.0-ompi1.8.1-jar-with-dependencies.jar PointTransformer -g $GLOBAL_VECS/$STOCK_FILE_NAME -gp $GLOBAL_POINTS/$GLOBAL_POINTS_FILE_NAME -v $CONT_VECS -p $CONT_POINTS -d $CONT_COMMON_POINTS | tee $BASE_DIR/$POSTPROC_INTERMEDIATE_DIR_NAME/common.points.out.txt
mv $CONT_COMMON_POINTS/2004_2014.csv $GLOBAL_CONT_COMMON_POINTS
