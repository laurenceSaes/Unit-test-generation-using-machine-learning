#!/bin/bash
export MODEL_NAME="$1"
if [ ! -n "$MODEL_NAME" ]; then
    echo "Model is not defined ( first param) "
    exit 1
fi

export MODEL_DIR="../models/$MODEL_NAME/"
export DEV_SOURCES="../nmt_data/$MODEL_NAME/dev/sources.txt"
export TARGET_REFS="../nmt_data/$MODEL_NAME/dev/targets.txt"
export PRED_DIR=${MODEL_DIR}/predict/


echo ${PRED_DIR}
#rm -r ${PRED_DIR}

mkdir -p ${PRED_DIR}

python -m bin.infer \
  --tasks "
    - class: DecodeText" \
  --model_dir $MODEL_DIR \
  --input_pipeline "
    class: ParallelTextInputPipeline
    params:
      source_files:
        - $DEV_SOURCES" \
  >  ${PRED_DIR}/predictions.txt

cat ${PRED_DIR}/predictions.txt
echo " BLUE: "

#Merge predict and exptect
paste -d \\n ${PRED_DIR}/predictions.txt ${TARGET_REFS} > ${PRED_DIR}/mergedAlternately.txt

java -jar decode.jar ${PRED_DIR}/mergedAlternately.txt ../nmt_data/$MODEL_NAME/compress.dict 1 1 > ${PRED_DIR}/codeAlternately.txt

java -jar decode.jar ${PRED_DIR}/predictions.txt ../nmt_data/$MODEL_NAME/compress.dict 1 0 > ${PRED_DIR}/predictCode.txt

./bin/tools/multi-bleu.perl ${TARGET_REFS} < ${PRED_DIR}/predictions.txt > ${PRED_DIR}/blueScore.txt

