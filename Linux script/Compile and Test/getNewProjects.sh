input=$(cat)

while read p; do
  link="https://github.com/$p.git"

  isFound=$(grep -c "$link" "tech-repos.txt")
  isFound2=$(grep -c "$p" "allProjects.txt")
  if [ $isFound -eq 0 ] && [ $isFound2 -eq 0 ]; then
	echo "$link"
  fi
done < "${1:-/dev/stdin}"
