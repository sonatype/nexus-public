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

[![CircleCI](https://circleci.com/gh/sonatype/nexus-public.svg?style=svg)](https://circleci.com/gh/sonatype/nexus-public) [![Join the chat at https://gitter.im/sonatype/nexus-developers](https://badges.gitter.im/sonatype/nexus-developers.svg)](https://gitter.im/sonatype/nexus-developers?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Requirements

* Apache Maven 3.3.3+
* Java 8+
* Network access to https://repository.sonatype.org/content/groups/sonatype-public-grid

## Building

To build the project and generate the template assembly use the included Maven wrapper:

    ./mvnw clean install

## Running

To run Nexus Repository, after building, unzip the assembly and start the server:

    unzip -d target assemblies/nexus-base-template/target/nexus-base-template-*.zip
    ./target/nexus-base-template-*/bin/nexus console

The `nexus-base-template` assembly is used as the basis for the official Sonatype Nexus distributions.

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information or our attention:

* File an issue in [our public JIRA](https://issues.sonatype.org/browse/NEXUS)
* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
* Connect with [@sonatypeDev](https://twitter.com/sonatypeDev) on Twitter
