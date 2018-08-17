max=3
loopTill=`expr $max - 1`
rm log/link/AST_*
for i in `seq 0 $loopTill`
do
    echo "$i-$max"
    bash runLink.sh "AST_$i" "AST" "unittests_ast" &
done
wait



