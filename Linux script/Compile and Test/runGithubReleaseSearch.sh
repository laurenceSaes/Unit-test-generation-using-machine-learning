max=4
for i in `seq 0 $max`
do
    echo "$i-$max"
    bash githubReleaseSearch.sh $i $max &
done
wait

