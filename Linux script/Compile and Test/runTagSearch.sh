max=2
for i in `seq 0 $max`
do
    echo "$i-$max"
    php findWorkingTag.php $i $max &
done
wait

