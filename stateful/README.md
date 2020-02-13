## Purpose of this repo
In this repo I show different stages of working with a mysql database in Kubernetes.
The stages each display a different resilience problem and the fix for the flaw.
The issues are kept very simple to be able to demonstrate it to colleagues.

The main components of the setup are a mysql pod, a service to expose it, a matching pvc to make data persistent, and a java pod running jdbc CRUD on the database.

## Prerequisites to execute the stages
- Kubernetes cluster, I use minikube
- docker to build jdbc containers and a way to use them (I mount the jdbc jar into minkube and use docker build in the vm to have the images locally on minikube)
- jdk to build jar

## Chaos Experiments
The flaws of each steps are shown by chaos experiments using LitmusChaos, if adequate.

## Stages
1. Failed jdbc restart: table already exists

|mysql deploy| jdbc deploy|
|------------|------------|
|kill pod    | error because db connection fails |
|restarts    | restarts   |
|            | error because table already exists|

**solution:** query if table already exists at beginning of jdbc code

2. DB unavailable when pod is down

|mysql deploy| jdbc deploy|
|------------|------------|
| kill pod   | connection lost|
|| application fails, dataloss |

**solution:** Create replicas of the database pod (or use some form of cache)  

3. Multiple replicas of db cause crash

|mysql deploy| jdbc deploy|
|------------|------------|
| scale replicas up to 3 ||
| pods crash taking turns, because they try to use the same pv at the same time ||

**solutions:** follow [this](https://kubernetes.io/docs/tasks/run-application/run-replicated-stateful-application/) example and set up 3 mysql pods in a statefulset, with one being the master and the others doing HA stuff to replicate the db

4. Kill the master db pod

|mysql deploy| jdbc deploy|
|------------|------------|
| kill mysql-0, which is the master db pod||
|||

5. Network loss

6. Network latency

7. Network corruption
