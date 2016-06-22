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
package org.sonatype.nexus.testsuite.p2;

import org.sonatype.nexus.test.http.RemoteRepositories;
import org.sonatype.nexus.test.http.RemoteRepositories.AuthInfo;
import org.sonatype.nexus.test.http.RemoteRepositories.RemoteRepository;
import org.sonatype.nexus.test.utils.TestProperties;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;

public abstract class AbstractNexusProxyP2SecureIT
    extends AbstractNexusProxyP2IT
{

  protected AbstractNexusProxyP2SecureIT(final String testRepositoryId) {
    super(testRepositoryId);
  }

  @Before
  public void startProxy() throws Exception {
    if (remoteRepositories == null) {
      remoteRepositories = RemoteRepositories.builder()
          .repo(
              RemoteRepository.repo("remote")
                  .resourceBase(TestProperties.getString("proxy-repo-target-dir"))
                  .authInfo(
                      new AuthInfo(
                          "BASIC",
                          ImmutableMap.of("admin", "admin")
                      )
                  ).build()
          )
          .build();
      remoteRepositories.start();
    }
  }

}
