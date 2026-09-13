AE2 Security Terminal
=======

Ports the ME Security Terminal and Biometric Card from AE2 Unofficial Extended Life to Applied Energistics 2
26.1 as an addon. Wireless terminal linking is not part of the port; wireless terminals stay linked through the
Wireless Access Point.

Building
=======

AE2 26.1 builds are not published to a public maven yet. Publish a local AE2 build first:

```sh
cd Applied-Energistics-2
./gradlew publishMavenPublicationToMavenLocal -x test -x spotlessCheck
```

Then set `ae2_version` in `gradle.properties` to the published version and run `./gradlew build`.
