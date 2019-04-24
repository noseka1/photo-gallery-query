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

## Deploying to OpenShift

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

Define a binary build (this will reuse the Java artifacts will built previously):

```
oc new-build \
--name query \
--binary \
--strategy docker
```

Correct the Dockerfile location in the build config:

```
oc patch bc query -p '{"spec":{"strategy":{"dockerStrategy":{"dockerfilePath":"src/main/docker/Dockerfile.jvm"}}}}'
```

Start the binary build:

```
oc start-build \
query \
--from-dir . \
--follow
```

Deploy the application:

```
oc new-app \
--image-stream query \
--name query \
--env QUARKUS_DATASOURCE_URL=jdbc:postgresql://postgresql-query:5432/querydb \
--env QUARKUS_VERTX_CLUSTER_HOST=0.0.0.0
```

Edit the generated query deploymentconfig:

```
oc edit dc query
```

And add this variable definition that allows Vert.x cluster nodes to correctly communicate with each other:

```
        - name: QUARKUS_VERTX_CLUSTER_PUBLIC_HOST
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
```

Expose the application to the outside world:

```
oc expose svc query
```
