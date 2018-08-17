#!/bin/bash
tac "$1" > readPredTmp.txt
> tokenTmpReversed.txt

foundSeperation=false

while IFS='' read -r line || [[ -n "$line" ]]; do
    if [[ -z "${line// }" ]]; then
       continue 
    fi

    if ! echo $line | grep -q -E "={10,}"; then
        if [ "$foundSeperation" = false ]; then
	   continue
        else
	   tokens=${line// SEQUENCE_END/}
           echo "$tokens" >> tokenTmpReversed.txt
	   continue
	fi
    fi

    if [ "$foundSeperation" = true ]; then
        break
    fi

    foundSeperation=true
done < readPredTmp.txt

rm readPredTmp.txt

#It was in reverse order
tac tokenTmpReversed.txt > tokenTmp.txt

dataSet=$(basename "$1")
dataSet=${dataSet%-nmt_*}

java -jar decode.jar tokenTmp.txt ../nmt_data/$dataSet/compress.dict 1 1

rm tokenTmpReversed.txt
rm tokenTmp.txt
 
