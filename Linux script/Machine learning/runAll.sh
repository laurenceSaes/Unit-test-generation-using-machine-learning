#!/bin/bash
#models=("nmt_medium.yml" "nmt_large.yml" "nmt_conv.yml");
#models=("nmt_conv_lstm_adam.yml" "nmt_conv_lstm_sgd.yml" "nmt_medium.yml_lstm_adam.yml" "nmt_medium.yml_lstm_sgd.yml");
#models=("nmt_medium.yml_lstm_adam.yml" "nmt_conv_lstm_adam.yml" );
rounds=100000
mkdir -p log

#for mode in ${models[*]} 
for modeFile in ~/storage/seq2seq/example_configs/nmt_*.yml
do
	for dir in ~/storage/nmt_data/*/
	do
#		for modeFile in ~/storage/seq2seq/example_configs/nmt_*.yml
#		do
			mode=$(basename $modeFile)
			project=$(basename $dir)
			echo "$project $mode"

			logLocation="log/$project-$mode-$rounds.txt"

#			if [ ! -f "$logLocation" ]
#			then
				start=$(date +%s)
				bash run.sh "$project" "$mode" "$rounds" >> $logLocation 2>&1
				stop=$(date +%s)
				echo "seconds: " $((stop-start)) >> $logLocation
#			fi
#		done
	done
done
