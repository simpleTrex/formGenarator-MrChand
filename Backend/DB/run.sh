#!/bin/bash
touch log
mkdir -p data
mongod --config=mongodb.conf -verbose
