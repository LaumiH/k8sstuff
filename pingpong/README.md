# Pingpong - an example setup

## Purpose

The setup was created to have a minimal Kubernetes cluster that generates UDP traffic. The application can then be tested in several chaos experiments.

## Deployments

1. **deploy-1: busy1**
   - **busy-1**: debian container that sends UDP packets with the output of *date* via netcat-openbsd to busy-2
2. **deploy-2: busy2**
   - **busy-2**: debian container that receives above packets via netcat-openbsd and writes them into *index.html* file
   - **nginx**: can display the *index.html* in a browser
   - **busybox**: deletes *index.html* every 3 minutes
   
## Configmap
Defines netcat commands for busy-1 and busy-2 and the command for the nginx server.

## Service
Defines port for incoming traffic for busy2. Port 8080 is exposed for the busy-2 container and port 80 for nginx.
