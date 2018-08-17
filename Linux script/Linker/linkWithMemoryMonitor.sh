logt=$(date +%s)
max=5
logTime=$(date +%s)
loopTill=`expr $max - 1`
#rm log/link/MEM_BYTECODE*
LOG_LOCATION=log/linkMemory
mkdir -p $LOG_LOCATION

for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    bash runLinkPassMemory.sh "MEM_BYTECODE_onepass_${logt}_$i" "BYTECODE" "unittests" &
done
wait
