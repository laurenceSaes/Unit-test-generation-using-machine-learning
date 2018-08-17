#rm /home/laurence/storage/tech-projects/*/addedToQueue.txt
dir=log/queueAll/
mkdir -p $dir
max=52

loopTill=`expr $max - 1`
for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    java -jar FillQueue/main.jar /home/laurence/storage/tech-projects/ $i $max "1" "unittests_all" "" 2> "$dir/error_$i.log" > "$dir/output_$i.log" &
done
wait

