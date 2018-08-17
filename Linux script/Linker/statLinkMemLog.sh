mkdir -p log/stats/
logd=$(date +%s)
bash statLinkMem.sh > log/stats/$logd.txt
