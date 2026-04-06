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
package org.sonatype.nexus.repository.content.browse.capability;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;

public class BrowseTrimCapabilityTest
    extends TestSupport
{
  @Mock
  private BrowseTrimService browseTrimService;

  private BrowseTrimCapability underTest;

  @Before
  public void setup() {
    underTest = new BrowseTrimCapability(browseTrimService);
  }

  @Test
  public void testCreateConfig() {
    Map<String, String> properties = Map.of(
        BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED, "true",
        BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED, "false");

    BrowseTrimCapabilityConfiguration config = underTest.createConfig(properties);

    assertThat(config, is(notNullValue()));
    assertThat(config, instanceOf(BrowseTrimCapabilityConfiguration.class));
    assertThat(config.isPostgresqlTrimEnabled(), is(true));
    assertThat(config.isBatchTrimEnabled(), is(false));
  }

  @Test
  public void testOnActivate() throws Exception {
    Map<String, String> properties = Map.of(
        BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED, "true",
        BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED, "true");
    BrowseTrimCapabilityConfiguration config = new BrowseTrimCapabilityConfiguration(properties);

    underTest.onActivate(config);

    verify(browseTrimService).setPostgresqlTrimEnabled(true);
    verify(browseTrimService).setBatchTrimEnabled(true);
  }

  @Test
  public void testOnActivateDisabled() throws Exception {
    Map<String, String> properties = Map.of(
        BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED, "false",
        BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED, "false");
    BrowseTrimCapabilityConfiguration config = new BrowseTrimCapabilityConfiguration(properties);

    underTest.onActivate(config);

    verify(browseTrimService).setPostgresqlTrimEnabled(false);
    verify(browseTrimService).setBatchTrimEnabled(false);
  }

  @Test
  public void testOnPassivate() throws Exception {
    BrowseTrimCapabilityConfiguration config = new BrowseTrimCapabilityConfiguration();

    underTest.onPassivate(config);

    verify(browseTrimService).setPostgresqlTrimEnabled(false);
    verify(browseTrimService).setBatchTrimEnabled(false);
  }

  @Test
  public void testOnRemove() throws Exception {
    BrowseTrimCapabilityConfiguration config = new BrowseTrimCapabilityConfiguration();

    underTest.onRemove(config);

    verify(browseTrimService).setPostgresqlTrimEnabled(false);
    verify(browseTrimService).setBatchTrimEnabled(false);
  }

  @Test(expected = NullPointerException.class)
  public void testNullBrowseTrimServiceRejected() {
    new BrowseTrimCapability(null);
  }
}
