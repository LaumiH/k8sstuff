## Purpose of this repo
In this repo I show different stages of working with a mysql database in Kubernetes.
The stages each display a different resilience problem and the fix for the flaw.

The main components of the setup are a mysql pod, a service to expose it, and a java pod running jdbc CRUD on the database.

## Stages
1. Failed restart: table already exists
|mysql deploy| jdbc deploy|
|------------|------------|
|------------|------------|
|------------|------------|
|------------|------------|
|------------|------------|
|------------|------------|

