date=$(date +%s)
mysqldump -u root -pinfosupport unittests > backup/unittests$date.sql
mysqldump -u root -pinfosupport unittests_ast > backup/unittests_ast_$date.sql
mysqldump -u root -pinfosupport unittests_all > backup/unittests_all_$date.sql
mysqldump -u root -pinfosupport analyze > backup/analyze_$date.sql
