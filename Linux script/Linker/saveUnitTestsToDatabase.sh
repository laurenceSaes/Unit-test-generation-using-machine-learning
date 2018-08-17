queue="unittests"
database="analyze"
table="set1"

mysql -u root -pinfosupport "$database" -e "truncate $database.$table;"

cd Linking
java -jar ../SaveUnitTestsToDatabase/main.jar "$database" "$queue" "$table"

