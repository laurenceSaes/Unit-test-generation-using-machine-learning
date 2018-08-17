TOTAL=0
for file in /home/laurence/storage/tech-projects/*/report.txt; do
	Tests=$(cat $file | grep -A 3 "Results :" | grep "Tests run: " | grep "Failures: 0" | grep "Errors: 0" | sed 's/\^\[\[0;1;32m//g' | sed 's/\^\[\[m//g' | sed -r 's/^[^0-9]*([0-9]+).*$/\1/' | paste -s -d+ - | bc)
	TSTS=$(($Tests + 0))
	if [ $TSTS -ne 0 ]; then
		echo "$TSTS\tfor: $file"
		TOTAL=$((TOTAL+TSTS))
		echo $file >> "goodlist.txt"
	else
		echo "$file failed"
#		rm $file
	fi done
echo "Total $TOTAL"
