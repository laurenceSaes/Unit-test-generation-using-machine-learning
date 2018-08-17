instance="$1"
max="$2"

counter=0
for d in /home/laurence/storage/tech-projects/*/ ; do
   counter=$((counter+1))
   if [ $(($counter%$max)) -ne "$instance" ]; then
	continue
   fi

   if [[ -f $d/tests.txt ]]; then
	continue
   fi

   echo $(basename $d)
   result=$(find "$d" -name *.java -type f  -exec grep -l '@Test' {} \; | uniq)
   echo "$result" > "$d/tests.txt"
done


