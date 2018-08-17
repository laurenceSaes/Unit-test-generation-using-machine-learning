echo "Set 1:"
echo "Use 'bash saveUnitTestsToDatabase.sh'"

echo "Set 2 (All in set 1 that that does not crash bytecode):"
mysql -u root -pinfosupport -e "INSERT INTO analyze.set2 (
	select NULL, tm.project, unit_test_method_class, unit_test_method_name, IF(SUM(method_code IS NOT NULL), 1, 0), IF(SUM(unit_test_code IS NOT NULL), 1, 0) 
	FROM unittests.registration 
	INNER JOIN unittests.test_method tm ON tm.id = test_method_id  
	GROUP BY tm.project, unit_test_method_class, unit_test_method_name);"

echo "Set 3: (All in set 2 that does not crash AST)"
mysql -u root -pinfosupport -e "INSERT INTO analyze.set3 (
	select NULL, 
	tm.project, 
	ar.unit_test_method_class, 
	ar.unit_test_method_name, 
	IF(SUM(br.has_method_code OR ar.method_code IS NOT NULL), 1, 0), 
	IF(SUM(br.has_unit_test_code OR ar.unit_test_code IS NOT NULL), 1, 0) 
	FROM unittests_ast.registration ar
INNER JOIN unittests_ast.test_method tm ON tm.id = ar.test_method_id 
INNER JOIN ( 
	select unit_test_method_class, unit_test_method_name, tm.project, 
	IF(SUM(method_code IS NOT NULL), 1, 0) as has_method_code, 
	IF(SUM(unit_test_code IS NOT NULL), 1, 0) as has_unit_test_code 
	FROM unittests.registration 
	INNER JOIN unittests.test_method tm ON tm.id = test_method_id 
	GROUP BY tm.project, test_method_id, unit_test_method_class, unit_test_method_name) br ON br.unit_test_method_class = ar.unit_test_method_class AND br.unit_test_method_name = ar.unit_test_method_name AND br.project = tm.project
GROUP BY tm.project, ar.unit_test_method_class, ar.unit_test_method_name
);"

echo "Set 4 (All in set 1 that that does not crash AST):"
mysql -u root -pinfosupport -e "INSERT INTO analyze.set4 (
	select NULL, tm.project, unit_test_method_class, unit_test_method_name, IF(SUM(method_code IS NOT NULL), 1, 0), IF(SUM(unit_test_code IS NOT NULL), 1, 0)
	FROM unittests_ast.registration 
	INNER JOIN unittests_ast.test_method tm ON tm.id = test_method_id 
	GROUP BY tm.project, unit_test_method_class, unit_test_method_name);"


