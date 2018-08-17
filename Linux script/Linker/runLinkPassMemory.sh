name="$1"
type="$2"
database="$3"
mkdir -p log/link/

couter=0
lastError=$(date)
maxErrors="50"
inSeconds="300"

logCounter=0
while true; do

# 4th paramete r1 = only one project. Same long sfor memory! Than scan working on!
	logCounter=$((logCounter+1))
        java -jar Linking/main.jar $type $database 1 1 2>> "log/link/${name}_${logCounter}_error.log" >> "log/link/${name}_${logCounter}_output.log" &
	echo "Start:\t$(date +%s)" >> "log/link/${name}_${logCounter}_mem.log"
	#sleep 1 &
	PID=$!
	MAX_MEM=0
	while true; do
		MEM=$(ps --pid $PID -o rss=)
		if (( MEM > MAX_MEM )); then
			MAX_MEM=$MEM
			echo "Usage\t$MEM" >> "log/link/${name}_${logCounter}_mem.log"
		fi

		if [ -z "$MEM" ]; then
			break;
		fi

		sleep 1
	done

	echo "STOP\t$(date +%s)" >> "log/link/${name}_${logCounter}_mem.log"
	

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



