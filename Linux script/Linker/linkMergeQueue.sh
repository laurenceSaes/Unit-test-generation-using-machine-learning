echo "INSERT INTO unittests_ast.queue (SELECT * FROM unittests.queue where id NOT IN (SELECT id FROM unittests_ast.queue))" | mysql -u root -pinfosupport
