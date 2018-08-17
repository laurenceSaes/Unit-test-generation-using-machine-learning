#rm /home/laurence/storage/tech-projects/*/addedToQueue*.txt
dir=log/queue/
mkdir -p $dir
max=92

loopTill=`expr $max - 1`
for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    bash runQueue.sh $i $max 0 "unittests" "" "$dir/error_$i.log" "$dir/output_$i.log" &
done
wait

