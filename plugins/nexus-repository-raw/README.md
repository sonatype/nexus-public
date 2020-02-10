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
# Configure Repositories

    orient:connect plocal:data/db/config admin admin
    orient:insert 'into repository_configuration SET repository_name="rawhosted1", recipe_name="raw-hosted", online=true, attributes={"storage":{"writePolicy":"ALLOW"}}'
    orient:insert 'into repository_configuration SET repository_name="rawhosted2", recipe_name="raw-hosted", attributes={"rawContent":{"strictContentTypeValidation":true}}'
    orient:insert 'into repository_configuration SET repository_name="rawproxy1", recipe_name="raw-proxy", attributes={"rawContent":{"strictContentTypeValidation":false},"proxy":{"remoteUrl":"https://repo1.maven.org/maven2/junit/junit/","contentMaxAge":120}, "httpclient":{"connection":{"timeout":20000, "retries":2}},"negativeCache":{"enabled":true}}'
    orient:insert 'into repository_configuration SET repository_name="rawgroup1", recipe_name="raw-group", attributes={"group": { "memberNames": ["rawhosted1", "rawhosted1", "rawproxy1"] }}'
    system:shutdown --force --reboot

# Interact

## Hosted

    curl -v --user 'admin:admin123' -H 'Content-Type: text/plain' --upload-file ./README.md http://localhost:8081/repository/rawhosted1/README.md
    curl -v --user 'admin:admin123' --upload-file ./README.md http://localhost:8081/repository/rawhosted1/no-type-README.md
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawhosted1/README.md
    curl -v --user 'admin:admin123' -X DELETE http://localhost:8081/repository/rawhosted1/README.md
    curl -v --user 'admin:admin123' -X DELETE http://localhost:8081/repository/rawhosted1/no-type-README.md

## Group

    curl -v --user 'admin:admin123' -H 'Content-Type: text/plain' --upload-file ./README.md http://localhost:8081/repository/rawhosted1/A
    curl -v --user 'admin:admin123' -H 'Content-Type: text/plain' -X PUT http://localhost:8081/repository/rawhosted1/B -d "B from rawhosted1"
    curl -v --user 'admin:admin123' -H 'Content-Type: text/plain' -X PUT http://localhost:8081/repository/rawhosted2/B -d "B from rawhosted2"
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawgroup1/A
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawgroup1/B
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawgroup1/4.12/junit-4.12.pom

## Proxy

    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawproxy1/4.12/junit-4.12.pom
    curl -v --user 'admin:admin123' -X GET http://localhost:8081/repository/rawproxy1/maven-metadata.xml

## Unproxied Equivalents

    curl -v -X GET https://repo1.maven.org/maven2/junit/junit/4.12/junit-4.12.pom
    curl -v -X GET https://repo1.maven.org/maven2/junit/junit/maven-metadata.xml

## Partial Fetch Example

    curl -v --user 'admin:admin123' -H 'Range: bytes=100-' -X GET http://localhost:8081/repository/rawproxy1/maven-metadata.xml
