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
package org.sonatype.nexus.testsuite.config.nexus2641;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RestApiSettings;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Nexus2641RestApiConfigIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void checkRestApiConfig()
      throws Exception
  {
    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();

    // *NEXUS-3840
    assertEquals(settings.getGlobalRestApiSettings().getUiTimeout(), 60);
    // *

    Assert.assertNotNull(settings.getGlobalRestApiSettings());

    // enable it, not that even the baseUrl is not set, it will be filled with a defaut one
    RestApiSettings restApiSettings = new RestApiSettings();
    settings.setGlobalRestApiSettings(restApiSettings);
    SettingsMessageUtil.save(settings);
    settings = SettingsMessageUtil.getCurrentSettings();

    Assert.assertNotNull(settings.getGlobalRestApiSettings());
    Assert.assertTrue(StringUtils.isNotEmpty(settings.getGlobalRestApiSettings().getBaseUrl()));
    Assert.assertEquals(settings.getGlobalRestApiSettings().isForceBaseUrl(), false);

    // now edit it
    restApiSettings.setBaseUrl("http://myhost/nexus");
    restApiSettings.setForceBaseUrl(true);
    settings.setGlobalRestApiSettings(restApiSettings);
    SettingsMessageUtil.save(settings);
    settings = SettingsMessageUtil.getCurrentSettings();

    Assert.assertNotNull(settings.getGlobalRestApiSettings());
    Assert.assertEquals(settings.getGlobalRestApiSettings().getBaseUrl(), "http://myhost/nexus");
    Assert.assertEquals(settings.getGlobalRestApiSettings().isForceBaseUrl(), true);

    //now unset it
    settings.setGlobalRestApiSettings(null);
    SettingsMessageUtil.save(settings);

    Assert.assertNull(settings.getGlobalRestApiSettings());
  }
}
