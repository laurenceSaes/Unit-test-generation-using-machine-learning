for dir in tech-projects/*/
do
	if [ ! -f $dir/pom.xml ]; then
		if [ ! -f $dir/report.txt ]; then
			echo "$dir has no report"
			rm -rf "$dir"
		else
			isFound=$(grep -c "Tests run: " "$dir/report.txt")
			if [ $isFound -eq 0 ]; then
				echo "$dir has no tests"
				rm -rf "$dir"
			else
				echo "!!! $dir has tests !!"
			fi
		fi
	fi
done
