compareTo="$1"

tac projects.txt > projects.rev.txt
#while read p; do
	#$(cd $p && find . -name *.java -type f -printf "%f\n" | sort > javaFiles.txt)
#done < projects.rev.txt

selfFound=false
while read p; do
    if [ "$compareTo" = "$p" ]; then
		selfFound=true
		continue
	fi
	
	if [ "$selfFound" = false ]; then
		continue
	fi
	sim=$(grep -Fxf "$p/javaFiles.txt" "$compareTo/javaFiles.txt" | wc -l)
	total=$(cat $p/javaFiles.txt | wc -l)
	percent=$(awk "BEGIN { pc=100*${sim}/${total}; i=int(pc); print (pc-i<0.5)?i:i+1 }")
	if [ $percent -ge 50 ]; then
		echo "$percent $sim $total $p"
	fi
done < projects.rev.txt

