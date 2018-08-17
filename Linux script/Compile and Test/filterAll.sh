while read p; do
	result=$(bash filter.sh "$p")
	if [ "$result" = "" ]; then
		reportLines="-"
		if [[ -e "$p/report.txt" ]]; then
 		      reportLines=$(cat "$p/report.txt" | wc -l)
		fi

		echo "$p $reportLines"
	fi
done < projects.txt
