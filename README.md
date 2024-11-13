<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2008-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# Sonatype Nexus Repository Open Source Codebase 

## Downloadable Bundles

 See: https://www.sonatype.com/download-oss-sonatype

## Support

Using Sonatype Nexus Repository OSS and need to report an issue? [Open an issue here](https://github.com/sonatype/nexus-public/issues)

Sonatype Nexus Repository Pro customers can use https://support.sonatype.com/.
 
## Build Requirements

Builds use Apache Maven and require Java 17. Apache Maven wrapper scripts are included in the source tree.

### Configuring Maven for SNAPSHOT Dependencies

Following best practices, the nexus-public POM does not include any root `<repositories>` elements.

## Building From Source

Released versions are tagged and branched using a name of the form `release-{version}`. For example: `release-3.72.0-04`

To build a tagged release, first fetch all tags:

```shell
git fetch --tags
```

Then checkout the remote branch you want. For example:

```shell
git checkout -b release-3.72.0-04 origin/release-3.72.0-04 --
```

Then build using the included Maven wrapper script. For example:

```shell
./mvnw clean install -Dpublic
```

The `public` property is required outside of Sonatype's internal infrastructure.

## Running

To run Nexus Repository, after building, unzip the assembly and start the server:

    unzip -d target assemblies/nexus-base-template/target/nexus-base-template-*.zip
    ./target/nexus-base-template-*/bin/nexus console

The `nexus-base-template` assembly is used as the basis for the official Sonatype Nexus Repository distributions.

## License

This project is licensed under the Eclipse Public License - v 1.0, you can read the full text [here](LICENSE.txt)

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information or our attention:

* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
* Connect with [@sonatypeDev](https://twitter.com/sonatypeDev) on Twitter
