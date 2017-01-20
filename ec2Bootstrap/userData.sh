#!/bin/bash -ex

# Debian apt-get install function
apt_get_install()
{
        DEBIAN_FRONTEND=noninteractive apt-get -y \
        -o DPkg::Options::=--force-confdef \
        -o DPkg::Options::=--force-confold \
        install $@
}

# Mark execution start
echo "STARTING" > /root/user_data_run

# Some initial setup
set -e -x
export DEBIAN_FRONTEND=noninteractive
apt-get update && apt-get upgrade -y

# Install required packages
apt_get_install git
add-apt-repository ppa:webupd8team/java
apt-get update
apt_get_install oracle-java8-installer

mkdir /opt/elasticsearch-migration
cd /opt/elasticsearch-migration
wget https://github.com/NewsWhip/es-actors/archive/v1.3.7.tar.gz
tar -xvf v1.3.7.tar.gz
cd es-actors-1.3.7
SERVER_CMD="sbt -J-Xmx32G -J-Xms32G \"; project server; run-main com.broilogabriel.Server\""
eval $SERVER_CMD </dev/null &>/dev/null &
/opt/elasticsearch-migration/es-actors-1.3.7/ec2Bootstrap/nightly.sh /opt/elasticsearch-migration/es-actors-1.3.7/ NewsWhipCluster NewsWhipStagingCluster 10.0.1.10,10.0.3.10,10.0.7.10,10.0.9.10 10.0.1.110,10.0.3.110,10.0.7.110,10.0.9.110 9300 8 >/dev/null 2>&1 &

# Mark execution end
echo "DONE" > /root/user_data_run

