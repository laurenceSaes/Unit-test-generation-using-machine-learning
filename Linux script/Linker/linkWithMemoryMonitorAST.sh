logt=$(date +%s)
max=5
logTime=$(date +%s)
loopTill=`expr $max - 1`
LOG_LOCATION=log/linkMemory
mkdir -p $LOG_LOCATION

for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    bash runLinkPassMemory.sh "AST_MEM_BYTECODE_onepass_${logt}_$i" "AST" "unittests_ast" &
done
wait
