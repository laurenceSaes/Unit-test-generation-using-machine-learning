name="$1"
database="$2"
query=$(cat registration.sql | sed "s/\`registration\`/\`$name\`/g")
mysql -u root -pinfosupport "$2" -e "$query"
echo "Done"
