wget https://d37drm4t2jghv5.cloudfront.net/distributions/24.7.1/linux/linux_x64_64_sfx.exe && \
    chmod 755 linux_x64_64_sfx.exe && \
    ./linux_x64_64_sfx.exe > /dev/null && \
    rm linux_x64_64_sfx.exe  

export LD_GAMS_PATH=gams24.7_linux_x64_64_sfx

cp $LD_GAMS_PATH/apifiles/Java/api/GAMSJavaAPI.jar backend-gams/lib-repository/com/gams/gamsjavaapi/24.7.1/gamsjavaapi-24.7.1.jar
cp $LD_GAMS_PATH/apifiles/Java/api/GAMSJavaAPI.jar backend-server/lib-repository/com/gams/gamsjavaapi/24.7.1/gamsjavaapi-24.7.1.jar

mkdir -p gams-native/src/main/resources/
cp $LD_GAMS_PATH/apifiles/Java/api/*.so gams-native/src/main/resources/
cd gams-native/ && mvn clean install && cd ../..
