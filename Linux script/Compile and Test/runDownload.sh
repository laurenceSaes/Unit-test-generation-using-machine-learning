max=10
for i in `seq 0 $max`
do
    echo "$i-$max"
    #gnome-terminal -x sh -c "xterm -e \"while true; do php download.php $i $max && break; done\""

    php download.php $i $max &
done
wait
