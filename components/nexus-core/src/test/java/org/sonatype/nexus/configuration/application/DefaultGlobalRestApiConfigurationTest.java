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
package org.sonatype.nexus.configuration.application;

import org.sonatype.nexus.NexusAppTestSupport;

import junit.framework.Assert;
import org.junit.Test;

public class DefaultGlobalRestApiConfigurationTest
    extends NexusAppTestSupport
{

  @Test
  public void testNoConfiguration()
      throws Exception
  {
    final DefaultGlobalRestApiSettings settings = (DefaultGlobalRestApiSettings) lookup(GlobalRestApiSettings.class);
    ApplicationConfiguration cfg = lookup(ApplicationConfiguration.class);
    cfg.getConfigurationModel().setRestApi(null);
    settings.configure(cfg);

    Assert.assertNull(settings.getBaseUrl());
    Assert.assertEquals(0, settings.getUITimeout());

    settings.setUITimeout(1000);
    settings.setBaseUrl("http://invalid.url");
    Assert.assertTrue(settings.commitChanges());

    Assert.assertEquals("http://invalid.url", settings.getBaseUrl());
    Assert.assertEquals(1000, settings.getUITimeout());
  }

}
