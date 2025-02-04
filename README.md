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
# Sonatype Nexus Repository Core source

Sonatype Nexus Repository Core are the EPL 1.0 licensed sources for the core of the Sonatype Nexus Repository.
This repository is a read-only export, Sonatype cannot accept contributions.

## Downloadable Bundles

Sonatype does not bundle the Nexus Repository Core sources for download.

Sonatype Nexus Repository Community and Pro distributions are available for download at:

https://www.sonatype.com/download-oss-sonatype

## Support

Using Sonatype Nexus Repository Community Edition and need to report an issue? [Open an issue here](https://github.com/sonatype/nexus-public/issues)

Sonatype Nexus Repository Pro customers can use https://support.sonatype.com/.

## Build Requirements

Builds use Apache Maven and require Java 17. Apache Maven wrapper scripts are included in the source tree.

## Building From Source

Released versions are tagged and branched using a name of the form `release-{version}`. For example: `release-3.78.0-04`

To build a tagged release, first fetch all tags:

```shell
git fetch --tags
```

Then checkout the remote branch you want. For example:

```shell
git checkout -b release-3.78.0-04 origin/release-3.78.0-04 --
```

Then build using the included Maven wrapper script. For example:

```shell
./mvnw clean install
```

## Running

To run Nexus Repository Core after building:

1. Navigate to `assemblies/nexus-repository-core/target/assembly`
2. Run `java -jar bin/nexus-repository-core-*.jar`

The application will create a sonatype-work directory at `assemblies/nexus-repository-core/target/sonatype-work`,
which will contain the default administrator credentials, database, and file blobstore.

## License

This project is licensed under the Eclipse Public License - v 1.0, you can read the full text [here](LICENSE.txt)

Sonatype Nexus Repository Core is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon between Sonatype, Inc. and Sencha Inc.
Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a closed source work.
