mysql -u root -pinfosupport -e "UPDATE unittests.queue SET bussy = 0 AND complete = 1 WHERE test_class IN (SELECT distinct unit_test_method_class FROM unittests.registration);"
mysql -u root -pinfosupport -e "UPDATE unittests_ast.queue SET bussy = 0 AND complete = 1 WHERE test_class IN (SELECT distinct unit_test_method_class FROM unittests_ast.registration);"
