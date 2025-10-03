#keytool -genkey -alias selfsigned_localhost_sslserver -keyalg RSA -keysize 2048 -validity 700 -keypass changeit -storepass changeit -keystore ssl-server.jks
apt update
apt upgrade
apt install apache2
apt install git
#git clone git@bitbucket.org:erpturbo/poc.git
#ifconfig eth0 mtu 1200
apt install openjdk-21-jdk
apt install maven
./run.sh 
