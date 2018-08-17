echo "Start filling"
$(bash fillQueue.sh)

echo "Link merge queue"
$(bash linkMergeQueue.sh)

echo "Count done"
$(bash countQueueDone.sh)

echo "Cache"
$(bash cache.sh)

echo "Link Bytecode"
$(bash linkWithMemoryMonitor.sh)

echo "Link AST"
$(bash linkWithMemoryMonitorAST.sh)

