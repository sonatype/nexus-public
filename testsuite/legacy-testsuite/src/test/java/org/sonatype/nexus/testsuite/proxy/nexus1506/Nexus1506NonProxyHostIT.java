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
package org.sonatype.nexus.testsuite.proxy.nexus1506;

import org.sonatype.nexus.configuration.model.CRemoteProxySettings;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettingsDTO;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;

public class Nexus1506NonProxyHostIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void checkNonProxyHosts()
      throws Exception
  {
    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();

    settings.setRemoteProxySettings(new RemoteProxySettingsDTO());
    settings.getRemoteProxySettings().setHttpProxySettings(new RemoteHttpProxySettingsDTO());
    settings.getRemoteProxySettings().getHttpProxySettings().setProxyHostname("proxyHost");
    settings.getRemoteProxySettings().getHttpProxySettings().setProxyPort(3211);
    settings.getRemoteProxySettings().addNonProxyHost("foo");
    settings.getRemoteProxySettings().addNonProxyHost("bar");

    Assert.assertEquals(204, SettingsMessageUtil.save(settings).getCode());

    settings = SettingsMessageUtil.getCurrentSettings();
    Assert.assertEquals(2, settings.getRemoteProxySettings().getNonProxyHosts().size());

    CRemoteProxySettings proxySettings = getNexusConfigUtil().getNexusConfig().getRemoteProxySettings();
    assertThat(proxySettings.getNonProxyHosts(), containsInAnyOrder("foo", "bar"));
    Assert.assertEquals(2, proxySettings.getNonProxyHosts().size());
  }

  @Test
  public void checkNonProxyHostsEmptyAndNulls()
      throws Exception
  {
    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();

    settings.setRemoteProxySettings(new RemoteProxySettingsDTO());
    settings.getRemoteProxySettings().setHttpProxySettings(new RemoteHttpProxySettingsDTO());
    settings.getRemoteProxySettings().getHttpProxySettings().setProxyHostname("proxyHost");
    settings.getRemoteProxySettings().getHttpProxySettings().setProxyPort(3211);
    settings.getRemoteProxySettings().addNonProxyHost("");
    settings.getRemoteProxySettings().addNonProxyHost("foo");
    settings.getRemoteProxySettings().addNonProxyHost(null);

    Assert.assertEquals(204, SettingsMessageUtil.save(settings).getCode());

    settings = SettingsMessageUtil.getCurrentSettings();
    Assert.assertEquals(1, settings.getRemoteProxySettings().getNonProxyHosts().size());

    CRemoteProxySettings proxySettings = getNexusConfigUtil().getNexusConfig().getRemoteProxySettings();
    Assert.assertEquals(proxySettings.getNonProxyHosts().get(0), "foo");
    Assert.assertEquals(1, proxySettings.getNonProxyHosts().size());
  }

  @Test
  public void checkNonProxyHostsEmpty()
      throws Exception
  {
    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();

    settings.setRemoteProxySettings(new RemoteProxySettingsDTO());
    settings.getRemoteProxySettings().setHttpProxySettings(new RemoteHttpProxySettingsDTO());
    settings.getRemoteProxySettings().getHttpProxySettings().setProxyHostname("proxyHost");
    settings.getRemoteProxySettings().getHttpProxySettings().setProxyPort(3211);
    settings.getRemoteProxySettings().getNonProxyHosts().clear();

    Assert.assertEquals(204, SettingsMessageUtil.save(settings).getCode());

    settings = SettingsMessageUtil.getCurrentSettings();
    Assert.assertEquals(0, settings.getRemoteProxySettings().getNonProxyHosts().size());

    CRemoteProxySettings proxySettings = getNexusConfigUtil().getNexusConfig().getRemoteProxySettings();
    Assert.assertEquals(0, proxySettings.getNonProxyHosts().size());
  }
}
