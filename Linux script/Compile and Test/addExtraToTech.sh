while read p; do
  link="https://github.com/$p.git"
  grep $link tech-repos.txt
  isFound=$(grep -c "$link" "tech-repos.txt")
  if [ $isFound -eq 0 ]; then
	echo "Add!"
	echo $link >> tech-repos.txt
  else
	echo "Duplicate!"
  fi
done < extraRepo.txt
