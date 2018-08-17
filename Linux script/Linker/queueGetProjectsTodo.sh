mkdir -p reportsTodo

for dir in /home/laurence/storage/tech-projects/*/
do
    if [[ ! -f $dir/addedToQueue.txt && -f $dir/report.txt ]]; then
	printf "$dir has test classes: "
	dirName=$(basename $dir)
	cat $dir/report.txt | grep "Tests run:" | grep "Failures: 0" | grep "Errors: 0" | grep "Skipped: 0" | wc -l

	if [[ -f "$dir/cp.txt" ]]; then
		echo "There is a CP!"
	fi

	
	mkdir -p  reportsTodo/$dirName/
	cp $dir/report.txt reportsTodo/$dirName/report.txt
	
    fi
done
