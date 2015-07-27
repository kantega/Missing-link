# Missing link
In project with many dependencies it is hard to keep track of the concequenses of removing or upgrading 
a dependency.
For instance if you have two direct dependecies A and B, and B also depends on A, you may experience runtime 
errors if the version of A(A2) declared in your project is incompatible with the version of A(A2) that B was compiled with. 
It may be the case that classes and methods used by B has changed between A1 and A2, causing 
*java.lang.NoClassDefFoundError* or *java.lang.NoSuchMethodError* when B tries to use classes and methods present in A2
but not in A1.

# Usage
The missing link tool can be run through its Maven plugin. 
```xml
<plugin>
    <groupId>org.kantega.missinglink</groupId>
    <artifactId>missing-link-maven-plugin</artifactId>
    <version>1.0</version>
    <configuration>
        <some configuration/>
    </configuration>
    <executions>
        <execution>
            <phase>process-classes</phase>
            <goals>
                <goal>findmissinglinks</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Configuration
The plugin can be configured with the following parameters:
* *failOnMissing* (default: false) - if set to true the plugin will fail the build if missing classes or methods are detected.
* *ignoreServletApi* (default: true) - ignore that references to javax.servlet are not present on the class path.
* *ignorePortletApi* (default: true) - ignore that references to javax.porlet are not present on the class path.
* *ignoreElApi* (default: true) - ignore that references to javax.el are not present on the class path.
* *ignoreAnnotationReferences* (default: true) - ignore references to annotations.
* *reportDirectory* (default:${project.build.directory}/missing-link)
* [*writeSeenAndVisitedToFile*](#writeseenandvisitedtofile) (default:false) - write all seen, and all referenced classes and methods to files in *reportDirectory*. 
* *ignoredPackages* (no default) - packages that should be ignored when generating report over missing classes and methods.
```xml
        <ignoredPackages>
            <ignoredPackage>javax/jms</ignoredPackage>
        </ignoredPackages>
```
* *ignoreReferencesInPackages* (no default) - packages whose references should be ignored when generating report over missing classes and methods.
```xml
        <ignoreReferencesInPackages>
            <ignoreReferencesInPackage>org/springframework/web/jsf</ignoreReferencesInPackage>
        </ignoreReferencesInPackages>
```

## Output produced by the tool
When the tool is finished analyzing the classes it will print «No missing methods» and «No missing classes» if all referenced classes and methods 
was found when doing to analysis.
Otherwise it will print «Missing classes detected. Reports can be found in *reportDirectory*», and the likewise for missing methods. 
If the configuration parameter *failOnMissing* is activated missing classes and methods will fail the build.

The files that are produced by default are:
* *missing-links-report.txt* - This is a summary report listing what packages was ignored, 
which classes was missing and what methods was missing.
* *missing-classes.json* and *missing-methods.json* - All classes and methods respectively considered missing. The JSON structure 
is a object with the missing class/method as key, and its value is an array of all other classes/methods referencing it.

### writeSeenAndVisitedToFile
When the configuration parameter *writeSeenAndVisitedToFile* is activated the following files are also created in *reportDirectory*: 
* *classes-referenced.json* - FQN of all classes and methods referenced, and the classes where they where referenced.
* *methods-referenced.json* - FQN of all methods refrenced, and in which methods they where referenced.
* *methods-visited.txt* - FQN of all methods visited.
* *classes-visited.txt* - FQN of all methods visited.
* *call-paths.txt* - All call paths ending in a missing method.
  
## Notes
* When running the tool with a class path containg frameworks like Spring, you will most likely get lots of missing classes and methods. 
For instance the *spring-web* dependency is compiled with view technologies like Velocity, Tiles, JSF, and many more. These are marked as optional, 
and will not get resolved transitively. 
* When declaring the plugin it does not really matter which phase it is run. It uses the dependecy information resolved by 
Maven, and run the tool with all the resolved jar and war-files.

