#!/bin/bash -ex

# Debian apt-get install function
apt_get_install()
{
        DEBIAN_FRONTEND=noninteractive apt-get -y \
        -o DPkg::Options::=--force-confdef \
        -o DPkg::Options::=--force-confold \
        install $@
}

ES_ACTORS_VERSION=1.3.12

# Mark execution start
echo "STARTING" > /root/user_data_run

# Some initial setup
set -e -x
export DEBIAN_FRONTEND=noninteractive
apt-get update && apt-get upgrade -y

# Install required packages
apt_get_install git

# Instal java8 
echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
add-apt-repository ppa:webupd8team/java
apt-get update
apt_get_install oracle-java8-installer

# Instal SBT
echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
apt-get update
apt_get_install sbt

mkdir /opt/elasticsearch-migration
cd /opt/elasticsearch-migration
wget https://github.com/NewsWhip/es-actors/archive/v$ES_ACTORS_VERSION.tar.gz
tar -xvf v$ES_ACTORS_VERSION.tar.gz
cd es-actors-$ES_ACTORS_VERSION/es-actors
SERVER_CMD="sbt -J-Xmx30G -J-Xms30G \"; project server; run-main com.broilogabriel.Server\""
eval $SERVER_CMD </dev/null &>/dev/null &
sleep 100
chmod 755 /opt/elasticsearch-migration/es-actors-$ES_ACTORS_VERSION/ec2Bootstrap/nightly.sh
/opt/elasticsearch-migration/es-actors-$ES_ACTORS_VERSION/ec2Bootstrap/nightly.sh /opt/elasticsearch-migration/es-actors-$ES_ACTORS_VERSION/es-actors NewsWhipCluster NewsWhipStagingCluster 10.0.1.10,10.0.3.10,10.0.7.10,10.0.9.10 10.0.1.110,10.0.3.110,10.0.7.110,10.0.9.110 9300 8 >/dev/null 2>&1 &

# Mark execution end
echo "DONE" > /root/user_data_run
