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
package com.sonatype.nexus.edition.oss;

import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.sonatype.analytics.internal.AnalyticsConstants;
import com.sonatype.nexus.licensing.ext.MultiProductPreferenceFactory;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.product.util.LicenseContent;
import org.sonatype.licensing.product.util.LicenseFingerprinter;
import org.sonatype.nexus.common.app.ApplicationLicense;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ApplicationLicenseImplTest
    extends TestSupport
{
  private ApplicationLicense underTest;

  @Mock
  private MultiProductPreferenceFactory productPreferenceFactory;

  @Mock
  private LicenseContent licenseContent;

  @Mock
  private LicenseFingerprinter fingerprinter;

  @Mock
  private List<Feature> availableFeatures;

  @Mock
  private Preferences preferences;


  @Before
  public void setUp() throws BackingStoreException {
    underTest = new ApplicationLicenseImpl(fingerprinter, availableFeatures, productPreferenceFactory, licenseContent);
    when(productPreferenceFactory.nodeForPath("")).thenReturn(preferences);
    when(preferences.childrenNames()).thenReturn(new String[] {AnalyticsConstants.COMPONENT_TOTAL_COUNT_EXCEEDED, AnalyticsConstants.PEAK_REQUESTS_PER_DAY_EXCEEDED});
  }

  @Test
  public void testCommunityLicenseIsRequired() {
    when(preferences.getLong(AnalyticsConstants.COMPONENT_TOTAL_COUNT_EXCEEDED, 0)).thenReturn(1722038400000l);
    when(preferences.getLong(AnalyticsConstants.PEAK_REQUESTS_PER_DAY_EXCEEDED, 0)).thenReturn(1722643200000l);

    boolean isCommunityLicenseRequired = underTest.isRequired();

    assertTrue(isCommunityLicenseRequired);
  }

  @Test
  public void testCommunityLicenseIsNotRequired() {
    when(preferences.getLong(AnalyticsConstants.COMPONENT_TOTAL_COUNT_EXCEEDED, 0)).thenReturn(0l);
    when(preferences.getLong(AnalyticsConstants.PEAK_REQUESTS_PER_DAY_EXCEEDED, 0)).thenReturn(0l);

    boolean isCommunityLicenseRequired = underTest.isRequired();

    assertFalse(isCommunityLicenseRequired);
  }
}
