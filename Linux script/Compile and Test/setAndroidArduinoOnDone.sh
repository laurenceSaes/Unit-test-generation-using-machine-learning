for dir in tech-projects/*/
do
    dep=""
    if [[ -f $dir/pom.xml ]]; then
	dep=$(cat $dir/pom.xml)
    fi
 
    if [[ -f $dir/build.gradle ]]; then
        dep=$(cat $dir/build.gradle)
    fi

    if echo $dep | grep -E --quiet "android|arduino"; then
	echo "Android or arduino project" > $dir/report.txt
	echo $dir
    fi

done
