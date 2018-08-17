SELECT * FROM unittests.`registration` r WHERE r.`unit_test_code` IS NOT NULL AND r.`method_code`IS NOT NULL AND 0 = (
    SELECT COUNT(*) from unittests_ast.registration WHERE `unit_test_code` = r.`unit_test_code` AND  `method_code` = r.`method_code`
)
