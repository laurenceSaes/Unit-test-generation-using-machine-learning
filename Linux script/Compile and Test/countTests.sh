foundFile="goodlist.txt"
notFoundFile="notFound.txt"
>notFoundFile
>foundFile
for file in tech-projects/*/report.txt; do
	Tests=$(cat $file | grep -A 3 "Results :" | grep "Tests run: " | grep "Failures: 0" | grep "Errors: 0" | sed 's/\^\[\[0;1;32m//g' | sed 's/\^\[\[m//g' | sed -r 's/^[^0-9]*([0-9]+).*$/\1/' | paste -s -d+ - | bc)
	TSTS=$(($Tests + 0))
	if [ $TSTS -ne 0 ]; then
		echo "$TSTS\tfor: $file"
		TOTAL=$((TOTAL+TSTS))
		echo $file >> $foundFile
	else
		echo $file >> $notFoundFile 
		echo "$file failed"
#		rm $file
	fi 
done

 echo "Total $TOTAL"
#grep -v "Time elapsed" tech-projects/*.txt | grep "Tests run: " | grep "Failures: 0" | grep "Errors: 0" | sed -r 's/^[^0-9]*([0-9]+).*$/\1/' | paste -s -d+ - | bc
