read -r -d '' projectOverview << EndOfMessage
SELECT "BYTE" as type, tm.project, count(*) as total 
FROM unittests.registration r
INNER JOIN unittests.test_method tm ON r.test_method_id = tm.id
WHERE r.method_code IS NOT NULL and r.unit_test_code IS NOT NULL
GROUP BY tm.project
UNION
SELECT "AST" as type, tm.project, count(*) as total
FROM unittests.registration r
INNER JOIN unittests_ast.test_method tm ON r.test_method_id = tm.id
WHERE r.method_code IS NOT NULL and r.unit_test_code IS NOT NULL
GROUP BY tm.project
EndOfMessage



echo "Projects with links"
mysql -B -u root -pinfosupport -e "
SELECT q.type, count(*) as projects FROM (
	$projectOverview
) q
GROUP BY q.type" 2> /dev/null

echo -e "\n\n"

echo "Duplicate projects"
mysql -B -u root -pinfosupport -e "
SELECT \"ALL\" as type, count(*) FROM (
SELECT 1 FROM (
	$projectOverview
) q
GROUP BY q.project HAVING count(*) > 1
) q2" 2> /dev/null

echo -e "\n\n"

echo "Links"

mysql -u root -pinfosupport -e "
SELECT q.project, q.type, IF(q.link, \"Link\", \"No link\") as link, count(*) as amount
FROM (
	SELECT tm.project, IF(SUM(method_code IS NULL), 0, 1) AS link, \"BYTE\" as type
	FROM unittests.registration r
	INNER JOIN unittests.test_method tm ON r.test_method_id = tm.id
	WHERE r.unit_test_code IS NOT NULL
	GROUP BY tm.project, r.unit_test_method_class, r.unit_test_method_name, r.method_code, r.unit_test_code
	UNION ALL
	SELECT tm.project, IF(SUM(method_code IS NULL), 0, 1) AS link, \"AST\" as type
	FROM unittests_ast.registration r
	INNER JOIN unittests_ast.test_method tm ON r.test_method_id = tm.id
	WHERE r.unit_test_code IS NOT NULL
	GROUP BY tm.project, r.unit_test_method_class, r.unit_test_method_name, r.method_code, r.unit_test_code
) q
GROUP BY q.project, q.link, q.type
ORDER BY q.project, q.link, q.type" 2> /dev/null


echo -e "\n\n"

echo "Duplicate links per project"

mysql -B -u root -pinfosupport -e "
SELECT q.project, \"ALL\" as type, \"link\" as link, count(*) amount
FROM (
	SELECT tm.project
	FROM unittests.registration r
	INNER JOIN unittests.test_method tm ON r.test_method_id = tm.id
	WHERE	r.method_code IS NOT NULL AND 
		r.unit_test_code IS NOT NULL AND 
		1 = (
		SELECT count(*) 
		FROM unittests_ast.registration r2 
		INNER JOIN unittests_ast.test_method tm2 ON r2.test_method_id = tm2.id
		WHERE tm2.project = tm.project AND
		      r2.method_code = r1.method_code AND
		      r2.unit_test_code = r1.unit_test_code AND
		      r2.unit_test_method_class = r.unit_test_method_class AND 
		      r2.unit_test_method_name = r.unit_test_method_name 
		      LIMIT 1
	)
	GROUP BY tm.project, r.unit_test_method_class, r.unit_test_method_name, r.method_code, r.unit_test_code
) q
GROUP BY q.project" 2> /dev/null
