echo "Remove arduino and android projects"
bash setAndroidArduinoOnDone.sh
echo "Done"

max=18
for i in `seq 0 $max`
do
    echo "$i-$max"
    #gnome-terminal -x sh -c "xterm -e \"while true; do php compileAndTest.php $i $max && break; done\""
    bash compileAndTest.sh $i $max &
done
wait
