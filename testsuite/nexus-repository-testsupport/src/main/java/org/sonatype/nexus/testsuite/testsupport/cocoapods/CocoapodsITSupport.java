/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.testsuite.testsupport.cocoapods;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

public class CocoapodsITSupport
    extends RepositoryITSupport
{
  protected static final String COCOAPODS_ROOT_PATH = "/";

  protected static final String POD_CONTENT_TYPE = "application/x-gzip";

  protected static final String GITHUB_API_URL = "http://localhost";

  protected static final int REMOTE_PORT_GITHUB = 57575;

  protected static final int REMOTE_PORT_HTTP = 57576;

  protected static final String GITHUB_API_HOST = "localhost:" + REMOTE_PORT_GITHUB;

  protected static final String GITHUB_SPECS_MASTER_REPO = "https://github.com/cocoapods/specs/";

  protected static final String NEXUS_PROPERTIES_FILE = "etc/nexus-default.properties";

  protected static final String POD_FILENAME = "test_pod-1.0.0.tar.gz";

  protected static final String SPEC_FILENAME = "test_pod.podspec.json";

  protected static final String INVALID_SPEC_FILENAME = "invalid_test_pod.podspec.json";

  protected static final String POD_GITHUB_API_PATH = "repos/test_vendor/test_pod/tarball/1.0.0";

  protected static final String POD_REMOTE_HTTP_PATH = "some/any/1.0.0.tar.gz";

  protected static final String POD_NAME = "test_pod";

  protected static final String POD_VERSION = "1.0.0";

  protected static final String POD_GITHUB_PATH = "pods/"
      + POD_NAME
      + "/"
      + POD_VERSION
      + "/http/localhost:%s/"
      + POD_GITHUB_API_PATH
      + ".tar.gz";

  protected static final String POD_HTTP_PATH = "pods/"
      + POD_NAME
      + "/"
      + POD_VERSION
      + "/http/localhost:%s/"
      + POD_REMOTE_HTTP_PATH;

  protected static final String SPEC_PATH = "Specs/";

  protected static final String NESTED_PROXY_REPO_NAME = "nested-cocoapods-proxy";

  protected static final String PROXY_REPO_NAME = "cocoapods-proxy";

  protected Repository createCocoapodsProxyRepository(
      final String name,
      final String remoteUrl)
  {
    return repos.createCocoapodsProxy(name, remoteUrl);
  }
}
