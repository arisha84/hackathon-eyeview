#!/bin/bash
mvn clean package
scp target/hackathon-gingis-1.0.0-SNAPSHOT-jar-with-dependencies.jar ec2-54-172-207-167.compute-1.amazonaws.com:/home/ubuntu/
scp *.sh ec2-54-172-207-167.compute-1.amazonaws.com:/home/ubuntu/
