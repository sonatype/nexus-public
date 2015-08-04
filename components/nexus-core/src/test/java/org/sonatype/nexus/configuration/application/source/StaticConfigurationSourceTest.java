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
package org.sonatype.nexus.configuration.application.source;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.configuration.source.ApplicationConfigurationSource;

import org.junit.Test;

public class StaticConfigurationSourceTest
    extends AbstractApplicationConfigurationSourceTest
{

  @Override
  protected ApplicationConfigurationSource getConfigurationSource()
      throws Exception
  {
    return lookup(ApplicationConfigurationSource.class, "static");
  }

  @Override
  protected InputStream getOriginatingConfigurationInputStream()
      throws IOException
  {
    return getClass().getResourceAsStream("/META-INF/nexus/nexus.xml");
  }

  @Test
  public void testStoreConfiguration()
      throws Exception
  {
    configurationSource = getConfigurationSource();

    configurationSource.loadConfiguration();

    try {
      configurationSource.storeConfiguration();

      fail();
    }
    catch (UnsupportedOperationException e) {
      // good
    }
  }

  @Test
  public void testIsConfigurationUpgraded()
      throws Exception
  {
    configurationSource = getConfigurationSource();

    configurationSource.loadConfiguration();

    assertEquals(false, configurationSource.isConfigurationUpgraded());
  }

  @Test
  public void testIsConfigurationDefaulted()
      throws Exception
  {
    configurationSource = getConfigurationSource();

    configurationSource.loadConfiguration();

    assertEquals(false, configurationSource.isConfigurationDefaulted());
  }

  @Test
  public void testGetDefaultsSource()
      throws Exception
  {
    configurationSource = getConfigurationSource();

    assertEquals(null, configurationSource.getDefaultsSource());
  }

}
