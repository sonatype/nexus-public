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
# Configure Repositories via UI

Use this config to create Maven2 hosted repository, use name `maven2-hosted` (or as you wish, but update accordingly the group configuration below):

```
{
  "maven" : {
    "versionPolicy" : "SNAPSHOT",
    "checksumPolicy" : "WARN",
    "strictContentTypeValidation" : false
  },
  "storage" : {
    "writePolicy" : "ALLOW"
  }
}
```

Use this config to create Maven proxy repository, use name `maven2-proxy` (or as you wish, but update accordingly the group configuration below):
```
{
  "maven" : {
    "versionPolicy" : "MIXED",
    "checksumPolicy" : "WARN",
    "strictContentTypeValidation" : false
  },
  "proxy" : {
    "remoteUrl" : "http://repo1.maven.org/maven2/",
    "contentMaxAge" : 3600
  },
  "httpclient" : {
    "connection" : {
      "timeout" : 1500,
      "retries" : 3
    },
    "authentication" : {
        "type" : "username",
        "username" : "admin",
        "password" : "admin123"
    }
  },
  "storage" : {
    "writePolicy" : "ALLOW"
  }
}
```

Group "maven2-group":
```
{
  "maven" : {
    "versionPolicy" : "MIXED",
    "checksumPolicy" : "WARN",
    "strictContentTypeValidation" : false
  },
  "group" : {
    "memberNames" : ["maven2-hosted", "maven2-proxy"]
  },
  "storage" : {
    "writePolicy" : "ALLOW"
  }
}
```

## Building Maven master branch and deploying it to NX3 itself

* create "maven2-hosted" repository with snippet above (to host mavan snapshot deploy)
* create "maven2-proxy" repository with snippet above to proxy Central
* create "maven2-group" repository with snippet above to group the two
* check out Apache Maven (or you can use any other prj that will have satisfied dependencies): https://github.com/apache/maven
* prepare environment, by putting this settings.xml below in project root directory (note: license header is a MUST due to eager RAT plugin, or use '-Drat.ignoreErrors=true')
```
<?xml version="1.0" encoding="UTF-8"?>

  <!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to you under the Apache License, Version
    2.0 (the "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0 Unless required by
    applicable law or agreed to in writing, software distributed under
    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
    OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and
    limitations under the License.
  -->

<settings>

    <localRepository>../maven-m2-repository</localRepository>

	<servers>
		<server>
			<id>local-nexus-admin</id>
			<username>admin</username>
			<password>admin123</password>
		</server>
	</servers>
	<mirrors>
		<mirror>
			<id>local-nexus-admin</id>
			<mirrorOf>external:*</mirrorOf>
			<url>http://localhost:8081/repository/maven2-group/</url>
		</mirror>
	</mirrors>
</settings>
```
* kick off a build and deploy:
```
mvn -s settings.xml clean deploy -Dtest=void -DfailIfNoTests=false -DaltDeploymentRepository=local-nexus-admin::default::http://localhost:8081/repository/maven2-hosted/ -U
```
Note: the `-U` is needed only to ensure Maven goes remote always (to recover from cached 404), if local repo empty as in "first run", it may be omitted.

