__WARNING: DO NOT USE PowerShell to execute commands__

```bash
mvn package -Dspotless.check.skip=true -pl :org.openhab.binding.ebus
mvn clean package -Dspotless.check.skip=true -pl :org.openhab.binding.ebus -DskipChecks
mvn verify -Dspotless.check.skip=true -pl :org.openhab.binding.ebus
mvn karaf:kar -Dspotless.check.skip=true -pl :org.openhab.binding.ebus -DskipChecks
mvn install -Dspotless.check.skip=true -pl :org.openhab.binding.ebus -DskipChecks
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dspotless.check.skip=true

mvn clean install karaf:kar -DskipChecks=true -DskipTests -Dcheckstyle.skip -pl :org.openhab.binding.ebus -DskipChecks=true -DskipTests -Dcheckstyle.skip
mvn license:format
```