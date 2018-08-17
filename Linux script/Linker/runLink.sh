name="$1"
type="$2"
database="$3"
mkdir -p log/link/


couter=0
lastError=$(date)
maxErrors="100"
inSeconds="500"

while true; do 
	java -jar Linking/main.jar $type $database 2>> "log/link/${type}_error_${name}.log" >> "log/link/${type}_output_${name}.log"
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

