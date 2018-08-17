max=5
loopTill=`expr $max - 1`
rm log/link/BYTE_*
for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    bash runLink.sh "BYTE_$i" "BYTECODE" "unittests" &
done
wait
