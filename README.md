# photo-gallery-query

## Build

This component requires the `photo-gallery-common` library. Make sure you build that library first.

You can build this project using:

```
mvn clean install package
```

## Database

This component requires access to a PostgreSQL database. You can create it using:

```
psql -c 'CREATE DATABASE querydb'
psql -c "CREATE USER queryuser WITH ENCRYPTED PASSWORD 'password'"
psql -c 'GRANT ALL PRIVILEGES ON DATABASE querydb TO queryuser'
```

## Run

You can run this component as a standalone service using:

```
java -jar target/photo-gallery-query-1.0-SNAPSHOT-runner.jar
```

After the service starts up you can test it using curl.

To retrieve all photos from a specific category ordered by the number of likes:

```
curl localhost:8082/query?category=animals
```

## Deploying to Minishift

Create a new project if it doesn't exist:

```
oc new-project photo-gallery-distributed
```

Deploy a PostgreSQL database:

```
oc new-app \
--template postgresql-persistent \
--param DATABASE_SERVICE_NAME=postgresql-query \
--param POSTGRESQL_USER=queryuser \
--param POSTGRESQL_PASSWORD=password \
--param POSTGRESQL_DATABASE=querydb
```

Prepare to connect to the Docker daemon running within the Minishift virtual machine:

```
eval $(minishift docker-env)
```

Build the application image:

```
docker build \
-f src/main/docker/Dockerfile.jvm \
-t 172.30.1.1:5000/photo-gallery-distributed/query \
.
```

Push the application image into the Minishift's integrated Docker registry:

```
docker login -u `oc whoami` -p `oc whoami -t` 172.30.1.1:5000
docker push 172.30.1.1:5000/photo-gallery-distributed/query
```

Deploy the application:

```
oc new-app \
--image-stream query \
--name query \
--env QUARKUS_DATASOURCE_URL=jdbc:postgresql://postgresql-query:5432/querydb
```

Expose the application to the outside world:

```
oc expose svc query
```
