# servers-maven-extension

Maven 3+ extension for exposing settings.xml/servers to ${}. What this means is that you can reference content of
`<servers>...</servers>` section (in form of ${settings.servers.&lt;server id&gt;.&lt;property&gt;}) from any pom.xml file
within your project. Also, starting from 1.2.0 release any part of `<servers>...</servers>` can be overridden with user
specified properties (-Dsettings.servers.&lt;server id&gt;.&lt;property&gt;=&lt;value&gt;).

> ${settings.servers.server.&lt;server id&gt;.&lt;property&gt;} format is also supported for the backwards compatibility with 1.0.0 release.

(see [#7](https://github.com/shyiko/servers-maven-extension/issues/7#issuecomment-405325619) for security implications)

Usage
---------------

Include following extension declaration into the (root) pom.xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    ...
    <build>
        ...
        <extensions>
            ...
            <extension>
                <groupId>com.github.shyiko.servers-maven-extension</groupId>
                <artifactId>servers-maven-extension</artifactId>
                <version>1.3.1</version>
            </extension>
            ...
        </extensions>
        ...
    </build>

</project>
```

Example
---------------

settings.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <servers>
        <server>
            <id>ssh-server</id>
            <username>username</username>
            <privateKey>${user.home}/.ssh/id_rsa</privateKey>
        </server>
    </servers>
    ...

</settings>
```

pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    ...
    <build>
        <plugins>
            <plugin>
                <groupId>...</groupId>
                <artifactId>...</artifactId>
                <configuration>
                    <location>
                        scp://${settings.servers.ssh-server.username}:from-key-file@${ssh-server.url}
                    </location>
                    <keyfile>${settings.servers.ssh-server.privateKey}</keyfile>
                    ...
                </configuration>
            </plugin>
        </plugins>
        <extensions>
            <extension>
                <groupId>com.github.shyiko.servers-maven-extension</groupId>
                <artifactId>servers-maven-extension</artifactId>
                <version>1.3.1</version>
            </extension>
        </extensions>
    </build>

</project>
```

License
---------------

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
