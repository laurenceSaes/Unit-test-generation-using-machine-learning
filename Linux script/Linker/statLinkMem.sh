bash stats.sh 
bash linkProgress.sh unittests
#printf "projects analyzed: "
#bash logLinkMemory.sh "MEM" | wc -l
ps aux | grep BYTECODE | grep main.jar
ls -tl log/link/MEM_BYTECODE_*_*_0_*_output.log | head -1
ls -tl log/link/MEM_BYTECODE_*_*_1_*_output.log | head -1
ls -tl log/link/MEM_BYTECODE_*_*_2_*_output.log | head -1
printf "Training data amount:"
bash trainingDataAmount.sh unittests
