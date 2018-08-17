for dir in *; do (cd "$dir" && printf "Tests: " && echo -n $(grep -R "@Test" | wc -l) && echo "\t$dir"); done

