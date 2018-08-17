keys=(APIKEY1 APIKEY2 APIKEY13 APIKEY4... HowMoreHowFaster)

for ((n=5;n<9;n++))
do
 php collect.php $n 8 ${keys[$n]} &
done
wait
