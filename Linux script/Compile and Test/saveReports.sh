mkdir -p reports
date=$(date +%s)
find . -maxdepth 3 -name 'report.txt' -exec cp --parents \{\} reports \;
zip -r "reports$date.zip" reports
