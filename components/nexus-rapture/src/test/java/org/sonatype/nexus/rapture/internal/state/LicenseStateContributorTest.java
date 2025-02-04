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
package org.sonatype.nexus.rapture.internal.state;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.common.app.ApplicationLicense;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LicenseStateContributorTest
{
  private static final List<String> LICENSE_ATTRIBUTES = Arrays.asList("feature1", "feature2");

  @Mock
  private ApplicationLicense applicationLicense;

  @InjectMocks
  private LicenseStateContributor licenseStateContributor;

  @Before
  public void setUp() {
    when(applicationLicense.isRequired()).thenReturn(true);
    when(applicationLicense.isInstalled()).thenReturn(true);
    when(applicationLicense.isValid()).thenReturn(true);
  }

  @Test
  public void testGetState_withExpirationDate_yesterday() {
    Date expirationDateYesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Map<String, Object> attributes = mockLicenseAttributes(expirationDateYesterday);
    when(applicationLicense.getAttributes()).thenReturn(attributes);

    Map<String, Object> state = licenseStateContributor.getState();
    LicenseXO licenseXO = (LicenseXO) Objects.requireNonNull(state).get("license");

    assertThat(licenseXO.isRequired(), is(true));
    assertThat(licenseXO.isInstalled(), is(true));
    assertThat(licenseXO.isValid(), is(true));
    assertThat(licenseXO.getDaysToExpiry(), is(-1));
    assertThat(licenseXO.getFeatures(), contains(LICENSE_ATTRIBUTES.toArray()));
  }

  @Test
  public void testGetState_withExpirationDate_25hours_from_now() {
    Date expirationDateTomorrow = Date.from(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));
    Map<String, Object> attributes = mockLicenseAttributes(expirationDateTomorrow);
    when(applicationLicense.getAttributes()).thenReturn(attributes);

    Map<String, Object> state = licenseStateContributor.getState();
    LicenseXO licenseXO = (LicenseXO) Objects.requireNonNull(state).get("license");

    assertThat(licenseXO.isRequired(), is(true));
    assertThat(licenseXO.isInstalled(), is(true));
    assertThat(licenseXO.isValid(), is(true));
    assertThat(licenseXO.getDaysToExpiry(), is(1));
    assertThat(licenseXO.getFeatures(), contains(LICENSE_ATTRIBUTES.toArray()));
  }

  @Test
  public void testGetState_withExpirationDate_less_than_24_hours_from_now() {
    Date expirationDateLessThan1Day = Date.from(Instant.now().plus(23, ChronoUnit.HOURS));
    Map<String, Object> attributes = mockLicenseAttributes(expirationDateLessThan1Day);
    when(applicationLicense.getAttributes()).thenReturn(attributes);

    Map<String, Object> state = licenseStateContributor.getState();
    LicenseXO licenseXO = (LicenseXO) Objects.requireNonNull(state).get("license");

    assertThat(licenseXO.isRequired(), is(true));
    assertThat(licenseXO.isInstalled(), is(true));
    assertThat(licenseXO.isValid(), is(true));
    assertThat(licenseXO.getDaysToExpiry(), is(0));
    assertThat(licenseXO.getFeatures(), contains(LICENSE_ATTRIBUTES.toArray()));
  }

  @Test
  public void testGetState_withoutExpirationDate() {
    Map<String, Object> attributes = mockLicenseAttributes(null);
    when(applicationLicense.getAttributes()).thenReturn(attributes);

    Map<String, Object> state = licenseStateContributor.getState();
    LicenseXO licenseXO = (LicenseXO) Objects.requireNonNull(state).get("license");

    assertThat(licenseXO.isRequired(), is(true));
    assertThat(licenseXO.isInstalled(), is(true));
    assertThat(licenseXO.isValid(), is(true));
    assertThat(licenseXO.getDaysToExpiry(), is(0));
    assertThat(licenseXO.getFeatures(), contains(LICENSE_ATTRIBUTES.toArray()));
  }

  @Test
  public void testGetState_withoutLicenseAttributes() {
    when(applicationLicense.getAttributes()).thenReturn(null);

    Map<String, Object> state = licenseStateContributor.getState();
    LicenseXO licenseXO = (LicenseXO) Objects.requireNonNull(state).get("license");

    assertThat(licenseXO.getFeatures().size(), is(0));
  }

  private Map<String, Object> mockLicenseAttributes(Date expirationDate) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ApplicationLicense.Attributes.EXPIRATION_DATE.getKey(), expirationDate);
    attributes.put(ApplicationLicense.Attributes.FEATURES.getKey(), LICENSE_ATTRIBUTES);
    return attributes;
  }
}
