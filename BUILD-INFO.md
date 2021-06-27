```bash
mvn package -Dspotless.check.skip=true -pl :org.openhab.binding.ebus
mvn clean package -Dspotless.check.skip=true -pl :org.openhab.binding.ebus
mvn verify -Dspotless.check.skip=true -pl :org.openhab.binding.ebus
mvn karaf:kar -Dspotless.check.skip=true -pl :org.openhab.binding.ebus
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dspotless.check.skip=true
```