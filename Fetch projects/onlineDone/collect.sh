keys=(APIKEY1 APIKEY2 APIKEY13 APIKEY4... HowMoreHowFaster)

for ((n=0;n<5;n++))
do
 php collect.php $n 15 ${keys[$n]} &
done
wait
