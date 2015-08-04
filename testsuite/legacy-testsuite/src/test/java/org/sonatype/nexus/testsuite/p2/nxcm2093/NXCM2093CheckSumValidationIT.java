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
package org.sonatype.nexus.testsuite.p2.nxcm2093;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class NXCM2093CheckSumValidationIT
    extends AbstractNexusProxyP2IT
{

  public NXCM2093CheckSumValidationIT() {
    super("nxcm2093-bad-checksum");
  }

  @Test
  public void test()
      throws Exception
  {
    final File installDir = new File("target/eclipse/nxcm2093");

    // the must work one
    installUsingP2(getNexusTestRepoUrl("nxcm2093-ok-checksum"), "org.mortbay.jetty.util",
        installDir.getCanonicalPath());

    try {
      final Map<String, String> sysProps = new HashMap<String, String>();
      sysProps.put("eclipse.p2.MD5Check", "false");

      installUsingP2(getNexusTestRepoUrl(), "com.sonatype.nexus.p2.its.feature.feature.group",
          installDir.getCanonicalPath(), sysProps);
      Assert.fail();
    }
    catch (final Exception e) {
      // NXCM-4501: this IT failed, but strangely, there WAS a checksum-invalid message for "feature" but
      // this test asserted presence of "plugin" line. Unsure why ordering (might) change, but test one thing sure
      // Nexus prevented P2 installation, as this assertion happens in a catch block, hence P2 did not install
      // artifacts with wrong checksums.
      // Changed to check for both variations of checksum-invalid loglines.
      final String nexusLogString = FileUtils.readFileToString(getNexusLogFile());
      final boolean passed =
          StringUtils.contains(nexusLogString,
              "Proxied item nxcm2093-bad-checksum:/plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar evaluated as INVALID")
              || StringUtils.contains(nexusLogString,
              "Proxied item nxcm2093-bad-checksum:/features/com.sonatype.nexus.p2.its.feature_1.0.0.jar evaluated as INVALID");

      assertThat(
          "Nexus log should contain log entry about plugin OR feature having invalid checksum (as both of them are invalid)",
          passed);
    }

    final RepositoryMessageUtil repoUtil =
        new RepositoryMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
    final RepositoryStatusResource repoStatusResource = repoUtil.getStatus(getTestRepositoryId());

    assertThat(repoStatusResource.getProxyMode(), is(equalTo(ProxyMode.ALLOW.name())));
    assertThat(repoStatusResource.getLocalStatus(), is(equalTo(LocalStatus.IN_SERVICE.name())));
  }
}
