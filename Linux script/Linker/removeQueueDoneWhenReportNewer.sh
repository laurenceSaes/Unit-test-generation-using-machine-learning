for dir in /home/laurence/storage/tech-projects/*/
do
    if [[ -f "$dir/report.txt" && -f "$dir/addedToQueue.txt" ]]; then
	#echo "$dir has report and queue done"
	
	if [ "$dir/report.txt" -nt "$dir/addedToQueue.txt" ]; then
		echo "$dir has a newer report"
  		rm "$dir/addedToQueue.txt"
		projectName=$(basename $dir)
		mysql -u root -pinfosupport unittests -e 'DELETE FROM queue WHERE project_name = "$projectName"'
	fi
	
    fi
done
