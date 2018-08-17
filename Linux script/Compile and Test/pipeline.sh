# File with github names should be given (apache/hbase) as 1ste parameter

#echo "delete from registration; delete from test_method; DELETE FROM queue;" | mysql -u root -pinfosupport unittests
#echo "delete from registration; delete from test_method; DELETE FROM queue;" | mysql -u root -pinfosupport unittests_ast

#Steps to download projects:
cd ~/storage

cat "$1" > extraRepo.txt

echo "Add all new repositories in extraRepo.txt"
$(bash addExtraToTech.sh)

echo "Download new projects"
$(bash runDownload.sh)

echo "To extract all class paths"
$(bash runCp.sh)

echo "To get all unit tests"
#TODO: fix gradle
$(bash run.sh)

echo "To see how many tests are inside the test reports"
$(bash countTests.sh)

echo "Create a backup"
$(bash saveReports.sh)




echo "Steps to link unit tests to methods"
cd ~/storage/linker

echo "Download the latest jars"
$(bash update.sh)

echo "Queue should be reanalyzed when there is a new report"
$(bash removeQueueDoneWhenReportNewer.sh)

echo "Add all projects in the queue"
$(bash fillQueue.sh)

echo "Link merge queue"
$(bash linkMergeQueue.sh)

echo "Determine how many projects are inside when done"
$(bash countQueueDone.sh)

echo "Create the cache so that linking is faster"
$(bash cache.sh)

echo "Do the linking"
$(bash link.sh)

echo "Do the linking"
$(bash linkAST.sh)

echo "Report status"
$(bash stats.sh)
$(bash linkProgress.sh) unittests

echo "Backup results"
$(bash backup.sh)

echo "Export training examples"
$(bash trainingData.sh)
