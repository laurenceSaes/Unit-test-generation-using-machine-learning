set -e

grep -L "Tests run:" tech-projects/*/report.txt | while read report; do 
	folder=$(dirname "$report")
	tagHuntFile="$folder/tagHunt.txt"
	if [ -f $tagHuntFile ]; then
		continue
	fi

        if ! grep --quiet "BUILD FAILURE" "$folder/report.txt" ; then
		continue
	fi

	tags=$(cd $folder && git for-each-ref --sort=taggerdate --format '%(refname)' refs/tags | tac)

	counter=0
	echo "$tags" | while read tag; do

		counter=$((counter + 1))
		if [ counter -ge 3 ]; then
			break
		fi

		echo "$folder - $tag"
		echo "Checkout"
		$(cd $folder && git checkout "$tag")

		echo "Test"
		$(cd $folder && mvn test --fae -Dmaven.test.failure.ignore=true -Dlicense.skip=true -Dcheckstyle.skip > reportTmp.txt)
		echo "Test done"

		if grep --quiet "Tests run:" "$folder/reportTmp.txt"; then
			echo "New report!"
			cp "$folder/reportTmp.txt" "$folder/report.txt"
			echo "$tag" > "$folder/tag.txt"
			break
		fi
	done

	echo "Tag search done"

	echo "done" > $tagHuntFile
	exit 1
done
