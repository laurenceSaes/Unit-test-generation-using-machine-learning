select r.type, r.method_class, r.method_name, r.unit_test_method_class, r.unit_test_method_name, r.unit_test_code, r.method_code
FROM (SELECT "BYTE" as type, r.*, tm.project FROM unittests.registration r INNER JOIN unittests.test_method tm ON tm.id = r.test_method_id UNION ALL SELECT "AST" as type, r.*, tm.project FROM unittests_ast.registration r INNER JOIN unittests_ast.test_method tm ON tm.id = r.test_method_id) r
INNER JOIN (
SELECT * FROM (
SELECT r.`unit_test_method_class`, r.`unit_test_method_name`, r.project FROM (
	SELECT r.*, tm.project 
	FROM unittests.registration r
	INNER JOIN unittests.test_method tm ON tm.id = test_method_id
	WHERE method_code IS NOT NULL and unit_test_code IS NOT NULL 
	UNION ALL 
	SELECT r.*, tm.project 
	FROM unittests_ast.registration r
	INNER JOIN unittests_ast.test_method tm ON tm.id = test_method_id
	WHERE method_code IS NOT NULL and unit_test_code IS NOT NULL
) r 
GROUP BY r.`unit_test_method_class`, r.`unit_test_method_name`, r.project HAVING count(distinct method_code) != 1 AND count(distinct unit_test_code) = 1
) q
) as incorrect ON incorrect.unit_test_method_class = r.unit_test_method_class AND incorrect.unit_test_method_name = r.unit_test_method_name AND incorrect.project = r.project;
