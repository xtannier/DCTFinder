#!/bin/bash

## the directory of this script
SCRIPT_DIR=$( cd $(dirname $0) ; pwd -P )

RULES_MAIN_DIR_ECLIPSE="$SCRIPT_DIR/resources/data/";
RULES_MAIN_DIR_STANDALONE="$SCRIPT_DIR/data/";

if [ -d $RULES_MAIN_DIR_ECLIPSE ] 
then
	RULES_MAIN_DIR=$RULES_MAIN_DIR_ECLIPSE
else
	RULES_MAIN_DIR=$RULES_MAIN_DIR_STANDALONE
fi

VOC_DIR_NAME=vocabulary
VOCABULARY_LIST_FILE_NAME=vocabulary_file_list.txt

lang_dirs=`ls -d $RULES_MAIN_DIR/*`

for dir in $lang_dirs 
do
	ls "$dir/$VOC_DIR_NAME" > "$dir/$VOCABULARY_LIST_FILE_NAME"
	echo "$dir/$VOCABULARY_LIST_FILE_NAME written"
done
