DELETE FROM unittests_all_clean.registration WHERE id IN (


SELECT id FROM unittests_all_clean.registration rf
INNER JOIN (select r.method_code, r.unit_test_code
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
) as incorrect ON incorrect.unit_test_method_class = r.unit_test_method_class AND incorrect.unit_test_method_name = r.unit_test_method_name AND incorrect.project = r.project) inc
ON inc.method_code = rf.method_code AND inc.unit_test_code = rf.unit_test_code

)