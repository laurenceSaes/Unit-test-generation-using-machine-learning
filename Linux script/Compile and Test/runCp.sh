max=27

for i in `seq 0 $max`
do
    echo "$i-$max"
    php classPath.php $i $max &
done
wait

