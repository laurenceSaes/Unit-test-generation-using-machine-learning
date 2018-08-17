mysql -u root -pinfosupport "$1" -e "SELECT count(*) FROM (SELECT count(*) from registration WHERE method_code IS NOT NULL and unit_test_code IS NOT NULL GROUP BY method_code, unit_test_code) q"
