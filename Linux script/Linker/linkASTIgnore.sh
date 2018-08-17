max=25
loopTill=`expr $max - 1`
rm log/link/AST_ignore_*
for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    bash runLink.sh "AST_ignore_$i" "AST" "unittests_all" &
done
wait
