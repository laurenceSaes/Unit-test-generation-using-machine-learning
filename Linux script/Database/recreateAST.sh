mysqldump --no-data -u root -pinfosupport unittests > unittest.structure.sql
mysqldump --no-create-info -u root -pinfosupport unittests queue > queue.sql
mysql -u root -pinfosupport unittests_ast < unittest.structure.sql
mysql -u root -pinfosupport unittests_ast < queue.sql
mysql -u root -pinfosupport unittests_ast -e "UPDATE queue SET bussy = 0, complete = 0"
