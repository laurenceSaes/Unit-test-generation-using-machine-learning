for log in log/*.txt 
do
   logName=$(basename $log)
   valScore=$(grep -oP "INFO:tensorflow:.*?loss = \K[0-9\.]*" $log | grep -v "Saving dict" |  tr '\n' '\t')
   testScore=$(grep -oP "INFO:tensorflow:Saving dict.*?loss = \K[0-9\.]*" $log)
   echo -e "name\t$logName\ttest\t$testScore\tvalidation\t$valScore"  | tr "." ','
done

