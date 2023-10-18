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
package org.sonatype.nexus.common.oss.circuit;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.oss.circuit.ContentUsageLevel.FREE_TIER;
import static org.sonatype.nexus.common.oss.circuit.ContentUsageLevel.HARD_LIMIT;
import static org.sonatype.nexus.common.oss.circuit.ContentUsageLevel.SOFT_LIMIT;
import static org.sonatype.nexus.common.oss.circuit.ContentUsageLevel.UNLIMITED;

public class ContentUsageCircuitBreakerTest
    extends TestSupport
{
  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private ContentUsageCircuitBreakerFeatureFlag circuitBreakerFeatureFlag;

  private ContentUsageCircuitBreaker underTest;

  @Test
  public void testInitOss() {
    initEdition("OSS");

    assertThat(underTest.getUsageLevel(), is(FREE_TIER));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testSetFreeTier() {
    initEdition("OSS");

    underTest.setUsageLevel(FREE_TIER);

    assertThat(underTest.getUsageLevel(), is(FREE_TIER));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void getOssDefaultLevel_featureFlagIsOff() {
    initEdition("OSS", false);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testOssSetFreeTier_featureFlagIsOff() {
    initEdition("OSS", false);

    underTest.setUsageLevel(FREE_TIER);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testOssSetSoftLimit_featureFlagIsOff() {
    initEdition("OSS", false);

    underTest.setUsageLevel(SOFT_LIMIT);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testOssSetHardLimit_featureFlagIsOff() {
    initEdition("OSS", false);

    underTest.setUsageLevel(HARD_LIMIT);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void getProDefaultLevel_featureFlagIsOff() {
    initEdition("PRO", false);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testProSetFreeTier_featureFlagIsOff() {
    initEdition("PRO", false);

    underTest.setUsageLevel(FREE_TIER);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testPROSetSoftLimit_featureFlagIsOff() {
    initEdition("PRO", false);

    underTest.setUsageLevel(SOFT_LIMIT);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testPROSetHardLimit_featureFlagIsOff() {
    initEdition("PRO", false);

    underTest.setUsageLevel(HARD_LIMIT);

    assertThat(underTest.getUsageLevel(), nullValue());
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testSetSoftLimit() {
    initEdition("OSS");

    underTest.setUsageLevel(SOFT_LIMIT);

    assertThat(underTest.getUsageLevel(), is(SOFT_LIMIT));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testSetHardLimit() {
    initEdition("OSS");

    underTest.setUsageLevel(HARD_LIMIT);

    assertThat(underTest.getUsageLevel(), is(HARD_LIMIT));
    assertThat(underTest.isClosed(), is(false));
  }

  @Test
  public void testImpossibleToSetUnlimitedForOss() {
    initEdition("OSS");

    underTest.setUsageLevel(UNLIMITED);

    assertThat(underTest.getUsageLevel(), is(FREE_TIER));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testInitPro() {
    initEdition("PRO");

    assertThat(underTest.getUsageLevel(), is(UNLIMITED));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testSetUnlimited() {
    initEdition("PRO");

    underTest.setUsageLevel(UNLIMITED);

    assertThat(underTest.getUsageLevel(), is(UNLIMITED));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testImpossibleToSetFreeTierForPro() {
    initEdition("PRO");

    underTest.setUsageLevel(FREE_TIER);

    assertThat(underTest.getUsageLevel(), is(UNLIMITED));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testImpossibleToSetSoftLimitForPro() {
    initEdition("PRO");

    underTest.setUsageLevel(SOFT_LIMIT);

    assertThat(underTest.getUsageLevel(), is(UNLIMITED));
    assertThat(underTest.isClosed(), is(true));
  }

  @Test
  public void testImpossibleToSetHardLimitForPro() {
    initEdition("PRO");

    underTest.setUsageLevel(HARD_LIMIT);

    assertThat(underTest.getUsageLevel(), is(UNLIMITED));
    assertThat(underTest.isClosed(), is(true));
  }

  private void initEdition(String edition) {
    when(applicationVersion.getEdition()).thenReturn(edition);
    when(circuitBreakerFeatureFlag.isEnabled()).thenReturn(true);
    underTest = new ContentUsageCircuitBreaker(applicationVersion, circuitBreakerFeatureFlag);
  }

  private void initEdition(String edition, boolean featureFlag) {
    when(applicationVersion.getEdition()).thenReturn(edition);
    when(circuitBreakerFeatureFlag.isEnabled()).thenReturn(featureFlag);
    underTest = new ContentUsageCircuitBreaker(applicationVersion, circuitBreakerFeatureFlag);
  }
}

