# photo-gallery-query

Query component

This component requires the `photo-gallery-common` library. Make sure you build that library first.

You can build this project using:

```
mvn clean install package
```

You can run this component as a standalone service using:

```
java -jar target/photo-gallery-query-1.0-SNAPSHOT-runner.jar
```

After the service starts up you can test it using curl.

To retrieve all photos from a specific category ordered by the number of likes:

```
curl localhost:8082/query?category=animals
```
