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
package org.sonatype.nexus.testsuite.p2.nxcm1691;

import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.proxy.repository.RemoteStatus;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NXCM1691P2ProxyTestStatusIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1691P2ProxyTestStatusIT() {
    super("nxcm1691-content-xml");
  }

  @Test
  public void test()
      throws Exception
  {
    final RepositoryMessageUtil repoUtil = new RepositoryMessageUtil(
        this, getXMLXStream(), MediaType.APPLICATION_XML
    );

    for (String s : P2Constants.METADATA_FILE_PATHS) {
      s = s.replaceAll("/", "").replaceAll("\\.", "-");
      testStatus(repoUtil, "nxcm1691-" + s, RemoteStatus.AVAILABLE);
    }

    testStatus(repoUtil, "nxcm1691-not-p2", RemoteStatus.UNAVAILABLE);
  }

  private void testStatus(final RepositoryMessageUtil repoUtil, final String repoId,
                          final RemoteStatus expectedStatus)
      throws Exception
  {
    final int timeout = 30000; // 30 secs
    final long start = System.currentTimeMillis();
    String status = RemoteStatus.UNKNOWN.toString();
    while (RemoteStatus.UNKNOWN.toString().equals(status) && (System.currentTimeMillis() - start) < timeout) {
      final RepositoryStatusResource statusResource = repoUtil.getStatus(repoId);
      status = statusResource.getRemoteStatus();
      Thread.sleep(100);
    }
    assertThat(expectedStatus.toString(), is(status));
  }
}
