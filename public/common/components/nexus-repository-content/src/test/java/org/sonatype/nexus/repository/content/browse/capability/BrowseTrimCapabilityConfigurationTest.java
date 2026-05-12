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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class BrowseTrimCapabilityConfigurationTest
{
  @Test
  public void testDefaultConstructorHasDefaultValues() {
    BrowseTrimCapabilityConfiguration underTest = new BrowseTrimCapabilityConfiguration();

    assertThat(underTest.isPostgresqlTrimEnabled(), is(false));
    assertThat(underTest.isBatchTrimEnabled(), is(false));
  }

  @Test
  public void testConstructorWithEmptyMap() {
    BrowseTrimCapabilityConfiguration underTest = new BrowseTrimCapabilityConfiguration(Map.of());

    assertThat(underTest.isPostgresqlTrimEnabled(), is(false));
    assertThat(underTest.isBatchTrimEnabled(), is(false));
  }

  @Test
  public void testConstructorWithEnabledProperties() {
    Map<String, String> props = Map.of(
        BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED, "true",
        BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED, "true");

    BrowseTrimCapabilityConfiguration underTest = new BrowseTrimCapabilityConfiguration(props);

    assertThat(underTest.isPostgresqlTrimEnabled(), is(true));
    assertThat(underTest.isBatchTrimEnabled(), is(true));
  }

  @Test
  public void testConstructorWithDisabledProperties() {
    Map<String, String> props = Map.of(
        BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED, "false",
        BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED, "false");

    BrowseTrimCapabilityConfiguration underTest = new BrowseTrimCapabilityConfiguration(props);

    assertThat(underTest.isPostgresqlTrimEnabled(), is(false));
    assertThat(underTest.isBatchTrimEnabled(), is(false));
  }

  @Test
  public void testSetters() {
    BrowseTrimCapabilityConfiguration underTest = new BrowseTrimCapabilityConfiguration();

    underTest.setPostgresqlTrimEnabled(true);
    assertThat(underTest.isPostgresqlTrimEnabled(), is(true));

    underTest.setBatchTrimEnabled(true);
    assertThat(underTest.isBatchTrimEnabled(), is(true));
  }

  @Test
  public void testAsMap() {
    BrowseTrimCapabilityConfiguration underTest = new BrowseTrimCapabilityConfiguration();
    underTest.setPostgresqlTrimEnabled(true);
    underTest.setBatchTrimEnabled(false);

    Map<String, String> result = underTest.asMap();
    assertThat(result.get(BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED), is("true"));
    assertThat(result.get(BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED), is("false"));
  }

  @Test
  public void testToString() {
    BrowseTrimCapabilityConfiguration underTest = new BrowseTrimCapabilityConfiguration();
    String result = underTest.toString();

    assertThat(result, containsString("BrowseTrimCapabilityConfiguration"));
    assertThat(result, containsString("postgresqlTrimEnabled=false"));
    assertThat(result, containsString("batchTrimEnabled=false"));
  }

  @Test
  public void testConstants() {
    assertThat(BrowseTrimCapabilityConfiguration.POSTGRESQL_TRIM_ENABLED, is("postgresqlTrimEnabled"));
    assertThat(BrowseTrimCapabilityConfiguration.BATCH_TRIM_ENABLED, is("batchTrimEnabled"));
  }
}
