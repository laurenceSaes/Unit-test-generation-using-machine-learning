find -mindepth 1 -maxdepth 2 -type f -name main.jar -print0 | xargs -0 rm 
wget https://lsaes.com/owncloud/index.php/s/POpc2mY0GRJfUYa/download
unzip download
cd Last\ artifacts
for dir in */
do
    dir=${dir%*/} 
    cp "$dir/main.jar" "../$dir/main.jar"
done
cd ..
rm -R Last\ artifacts/
rm download

