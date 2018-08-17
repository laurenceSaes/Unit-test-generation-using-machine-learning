#rm /home/laurence/storage/tech-projects/*/addedToQueue.txt
#cd Linking #remove when runCache.sh
max=4
loopTill=`expr $max - 1`
for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    #java -jar ../PreCache/main.jar $i $max "unittests" &
    bash runCache.sh $i $max "unittests" &
done
wait
