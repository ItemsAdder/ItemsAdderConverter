# Building

Edit `build.gradle.kts` and change the path to your server plugins folder.
```kts
tasks.register<Copy>("copyToServer") {
    from(tasks.named("shadowJar"))
    into("C:\\Progetti\\Minecraft\\TestServer\\1.21.5\\plugins")
    // ...
```

Use this command to build the JAR.

`mvn build shadowJar`