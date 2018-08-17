echo "SELECT (select count(*) from queue where bussy = 0 AND complete = 0) as todo, (select count(*) from queue where bussy != 0 OR complete != 0) as done;" | mysql -u root -pinfosupport unittests
