find log/link/ -name *_output.log -print0 | while IFS= read -r -d '' file; do 
	
	isFound=$(grep -c "There are no projects to process" "$file")
  	lines=$(cat $file | wc -l)
	if [ "$isFound" -ne 0 ] && [ "$lines" -eq 3 ]; then
		outputFile="$file"
		memFile=$(echo "$outputFile" | sed 's/_output.log/_mem.log/g')
		errorFile=$(echo "$outputFile" | sed 's/_output.log/_error.log/g')
		rm "$outputFile"
		rm "$memFile"
		rm "$errorFile"
		echo "Del"
  	fi
done
