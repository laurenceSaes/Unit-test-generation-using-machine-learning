echo -e "Type\tName\tTime\tClasses\tMemory\tCode found\tNo link"

type="$1" # MEM for normal mode or AST for ast mode

for f in log/link/$type*_output.log; do 

	output=$f
	mem=$(echo $f | sed "s/_output/_mem/g")

	classes=$(grep "Linking done for" $output | wc -l)
	memory=$(( $(tac $mem | grep "Usage" | grep -Eo "[0-9]+" | head -1) / 1024 ))
	start=$(grep "Start:" $mem | grep -Eo "[0-9]+")
	stop=$(grep "STOP" $mem | grep -Eo "[0-9]+")
	timeE=$((stop-start))
	codeFound=$(grep "Code found" $output | wc -l)
	noLink=$(grep "No link for:" $output | wc -l)

	if [ -z "$stop" ]; then
		continue
	fi

	name=$(grep -Po "Linking done for \K.*? " $output | head -1)
        if [ -z "$name" ]; then
                continue
        fi

	echo -e "${type}\t${name}\t$timeE\t$classes\t$memory\t$codeFound\t$noLink"
done

