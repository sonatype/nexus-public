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
# Nexus Rapture Baseapp

This module provides the baseapp muck for ExtJS 4+ pre-compiled with Sencha CMD.

Requires Sencha CMD 4.x (latest 4.x should work newer 5.x has problems).

May completely revisit this solution later.

## Regenerating

    mvn clean install -Pregenerate

If the content has changed, then the result needs to be committed:

    git commit . -m "regenerated baseapp"

## Regenerating for development

Slightly faster, will only generate the 'debug' flavors and will re-use extjs distribution and baseapp: 

    mvn install -Pregenerate -Dflavors=debug

## Locally install ExtJS distribution

    mvn -Pinstall-ext -Dext.dist=ext-4.2.3-commercial.zip -Dext.version=4.2.3
