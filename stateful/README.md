## Purpose of this repo
In this repo I show different stages of working with a mysql database in Kubernetes.
The stages each display a different resilience problem and the fix for the flaw.
The issues are kept very simple to be able to demonstrate it to colleagues.

The main components of the setup are a mysql pod, a service to expose it, a matching pvc to make data persistent, and a java pod running jdbc CRUD on the database.

## Prerequisites to execute the examples
- Kubernetes cluster, I use minikube
- docker to build jdbc containers and a way to pull them from a registry
  (I run minikube with vm-driver=none, so I can build the docker image on the host and push it to localhost:5000 registry. From there it can be pulled inside minikube.)

## Deploy the application
0. If wanted, create a namespace called `mysql` to deploy into. I did so, and this is why `mysql` is used as a namespace in all the files.
1. execute `kubectl apply -f *.yml` on all the files in the yaml folder. Be sure to deploy the pvc before the mysql-deploy.
2. Check the state of the deployments with `kubectl get pods`.
3. You are now ready to induce some chaos in this seemingly well running aplication!


## Chaos Experiments
To induce several faults into the application, chaos engineering is used. LitmusChaos is a tool to run several predefined chaos experiments on different cluster resources.

### Install LitmusChaos into the cluster
Please follow the [official guide](https://docs.litmuschaos.io/docs/getstarted/). Execute only the steps `Install Litmus` and `Install Chaos Experiments`.

### Execute an experiment
In LitmusChaos, every experiments needs several resources:
- ServiceAccount
- Role
- RoleBinding
- Chaosengine

First, the AUT (application unter test) needs to be annotated in order to gain maximum controllability over which deployments are affected. Annotate mysql and jdbc deployments with `kubectl annotate deploy/<name> litmuschaos.io/chaos="true"`.
Deploy the fitting rbac.yml into the cluster, then deploy the chaos.yml.

## Scenarios

1. Failed jdbc restart: table already exists

Please take a look at Jdbc_scenario1.java.

Chaos experiment: kill jdbc pod. Hypothesis: the pod will restart and pick up the database writing.
Chaosengine and rbac: pod-delete, deployment name = mysql

|mysql deploy| jdbc deploy|
|------------|------------|
|            | restarts   |
|            | error because database already exists, cannot execute `create database test;`|

**solution:** query if table already exists at beginning of jdbc code -> see Jdbc_scenario2.java


2. DB unavailable when mysql pod is down

Jdbc_scenario2.java is now used.

Chaos experiment: kill mysql pod. Hypothesis: jdbc pod will wait and retry until the database is up again (a bit naive).
Chaosengine and rbac: pod-delete, deployment name = mysql

|mysql deploy| jdbc deploy|
|------------|------------|
|            | connection lost, pod throws error and restarts |
|            | potential data loss |

**solution:** Create multiple replicas of the database pod OR/AND rewrite code so that it retries to connect to db (not done here)  


3. Multiple replicas of db cause crash

|mysql deploy| jdbc deploy|
|------------|------------|
| scale replicas up to 3 ||
| only one pod is actively offering the database, because they try to use the same pv at the same time | |

**solution:** follow [this](https://kubernetes.io/docs/tasks/run-application/run-replicated-stateful-application/) example and set up 3 mysql pods in a statefulset, with one being the master and the others doing HA stuff to replicate the db


4. Kill the master db pod (tbd)

A StatefulSet with 3 mysql replicas is now used, see above link.

Chaos experiment: kill mysql-0 (the master db replica)

|mysql deploy| jdbc deploy|
|------------|------------|
|            |            |
|            |            |


5. 100% network loss for mysql StatefulSet (tbd)



6. 100% network latency for mysql StatefulSet (tbd)



7. 70% network corruption for mysql StatefulSet (tbd)
