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

[![CircleCI Build Status](https://circleci.com/gh/sonatype/nexus-public.svg?style=shield "CircleCI Build Status")](https://circleci.com/gh/sonatype/nexus-public) [![Join the chat at https://gitter.im/sonatype/nexus-developers](https://badges.gitter.im/sonatype/nexus-developers.svg)](https://gitter.im/sonatype/nexus-developers?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Downloadable Bundles

 See: https://www.sonatype.com/download-oss-sonatype
 
## Build Requirements

Builds use Apache Maven and require Java 8. Apache Maven wrapper scripts are included in the source tree.

All release versioned dependencies should be available from the [Central](https://repo1.maven.org/maven2/) repository.

For SNAPSHOT sources, SNAPSHOT versioned dependencies may only be available from https://repository.sonatype.org/content/groups/sonatype-public-grid repository.

### Configuring Maven for SNAPSHOT Dependencies

Following best practices, the nexus-public POM does not include any root `<repositories>` elements.
    
Instead you are advised to [configure Apache Maven to point at single repository mirror URL](https://maven.apache.org/guides/mini/guide-mirror-settings.html#using-a-single-repository) that is a group repository containing both Central proxy repository with Release version policy and sonatype-public-grid with a SNAPSHOT version policy. You can use a [repository manager](https://www.sonatype.org/nexus/go/) to set up a group repository that contains both of these remotes.

Alternately, [add a custom profile to a settings.xml](https://maven.apache.org/guides/mini/guide-multiple-repositories.html) for repository manager development that includes both repositories.

## Building From Source

Released versions are tagged and branched using a name of the form `release-{version}`. For example: `release-3.29.2-02`

To build a tagged release, first fetch all tags:

```shell
git fetch --tags
```

Then checkout the remote branch you want. For example:

```shell
git checkout -b release-3.29.2-02 origin/release-3.29.2-02 --
```

Then build using the included Maven wrapper script. For example:

```shell
./mvnw clean install
```

For building SNAPSHOT versions, follow the same process, except your build may require access to [Sonatype Public Grid](https://repository.sonatype.org/content/groups/sonatype-public-grid) to successfully resolve dependencies.

## Running

To run Nexus Repository, after building, unzip the assembly and start the server:

    unzip -d target assemblies/nexus-base-template/target/nexus-base-template-*.zip
    ./target/nexus-base-template-*/bin/nexus console

The `nexus-base-template` assembly is used as the basis for the official Sonatype Nexus Repository distributions.

## License

This project is licensed under the Eclipse Public License - v 1.0, you can read the full text [here](LICENSE.txt)

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information or our attention:

* File an issue in [our public JIRA](https://issues.sonatype.org/browse/NEXUS)
* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
* Connect with [@sonatypeDev](https://twitter.com/sonatypeDev) on Twitter
