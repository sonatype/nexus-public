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
package org.sonatype.nexus.testsuite.p2.nxcm1960;

import java.io.IOException;

import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettingsDTO;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NXCM1960SetProxyIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1960SetProxyIT() {
    super("nxcm1960");
  }

  @Test
  public void test()
      throws Exception
  {
    setupProxyConfig();

    installAndVerifyP2Feature();
  }

  private void setupProxyConfig()
      throws IOException
  {
    final GlobalConfigurationResource resource = SettingsMessageUtil.getCurrentSettings();

    RemoteProxySettingsDTO proxy = resource.getRemoteProxySettings();

    if (proxy == null) {
      proxy = new RemoteProxySettingsDTO();
      resource.setRemoteProxySettings(proxy);
    }

    proxy.setHttpProxySettings(new RemoteHttpProxySettingsDTO());
    proxy.getHttpProxySettings().setProxyHostname("http://somejunkproxyurl");
    proxy.getHttpProxySettings().setProxyPort(555);

    proxy.getNonProxyHosts().clear();
    proxy.addNonProxyHost("localhost");

    final Status status = SettingsMessageUtil.save(resource);

    assertThat(status.isSuccess(), is(true));
  }

}
