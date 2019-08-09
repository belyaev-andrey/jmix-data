Experimental library for RnD project on adding CUBA features to Spring Boot. It is just a PoC, not for production use.

Features in the PoC:
* Soft Delete
* Filtering
* Cross-Datasource references
 
To use the library in your experiments install it to your maven local repository by executing 
```
gradlew publishToMavenLocal
```

And then add it as a dependency to your project:
```
<dependency>
    <groupId>io.jmix.data</groupId>
    <artifactId>jmix-data-rnd</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

See [Pet Clinic Example]() and [CUBA Internal Confluence]() for usage details. 
