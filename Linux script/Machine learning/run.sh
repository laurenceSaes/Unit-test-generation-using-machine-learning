export DATA_SET="$1"
export MODEL="$2"
export TRAIN_STEPS="$3"

export MAIN_FOLDER=/home/laurence/storage
export DATA_FOLDER=$MAIN_FOLDER/nmt_data/
export MODEL_FOLDER=$MAIN_FOLDER/models/

export VOCAB_SOURCE=$DATA_FOLDER/$DATA_SET/train/vocab.sources.txt
export VOCAB_TARGET=$DATA_FOLDER/$DATA_SET/train/vocab.targets.txt
export TRAIN_SOURCES=$DATA_FOLDER/$DATA_SET/train/sources.txt
export TRAIN_TARGETS=$DATA_FOLDER/$DATA_SET/train/targets.txt
export DEV_SOURCES=$DATA_FOLDER/$DATA_SET/dev/sources.txt
export DEV_TARGETS=$DATA_FOLDER/$DATA_SET/dev/targets.txt
export DEV_TARGETS_REF=$DATA_FOLDER/$DATA_SET/dev/targets.txt


export MODEL_DIR=$MODEL_FOLDER/$DATA_SET/
#rm -r $MODEL_DIR
mkdir -p $MODEL_DIR

TEXT_MATRIC="text_metrics_sp.yml"
if [[ $DATA_SET = *"BPE"* ]]; then
  TEXT_MATRIC="text_metrics_bpe.yml"
  echo "BPE mode"
fi

python -m bin.train \
  --config_paths="
      ./example_configs/$MODEL,
      ./example_configs/train_seq2seq.yml,
      ./example_configs/$TEXT_MATRIC" \
  --model_params "
      vocab_source: $VOCAB_SOURCE
      vocab_target: $VOCAB_TARGET" \
  --input_pipeline_train "
    class: ParallelTextInputPipeline
    params:
      source_files:
        - $TRAIN_SOURCES
      target_files:
        - $TRAIN_TARGETS" \
  --input_pipeline_dev "
    class: ParallelTextInputPipeline
    params:
       source_files:
        - $DEV_SOURCES
       target_files:
        - $DEV_TARGETS" \
  --batch_size 32 \
  --train_steps $TRAIN_STEPS \
  --output_dir $MODEL_DIR 
