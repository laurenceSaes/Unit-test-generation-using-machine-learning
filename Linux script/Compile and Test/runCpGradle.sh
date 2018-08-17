max=25

for i in `seq 0 $max`
do
    echo "$i-$max"
    php classPathGradle.php $i $max &
done
wait

