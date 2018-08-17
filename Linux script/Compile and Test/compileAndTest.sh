couter=0
lastError=$(date)
maxErrors="10"
inSeconds="100"

while true; do 
	php compileAndTest.php "$1" "$2" 
	echo "Restart $1 att:$counter" 
	failedOn=$(date)
	if [ "$counter" == "$maxErrors" ]; then
		newDate=$(date)
		minNewDate=$(date -d "$lastError +$inSeconds second")
		first=$(date -d "$minNewDate" +%s)
		second=$(date -d "$newDate" +%s)
		echo "$first -ge $second"
		if [ $first -ge $second ]; then
			echo "Too many errors"
			break
		fi
		lastError=$(date)
		counter=0
		echo "Retry"
	fi
	sleep 1
	counter=$((counter+1))
done

