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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.analytics.internal.AnalyticsConstants;
import com.sonatype.nexus.licensing.ext.AbstractApplicationLicense;
import com.sonatype.nexus.licensing.ext.NexusCommunityFeature;
import com.sonatype.nexus.licensing.ext.MultiProductPreferenceFactory;
import org.sonatype.licensing.feature.Feature;
import org.sonatype.licensing.product.util.LicenseContent;
import org.sonatype.licensing.product.util.LicenseFingerprinter;
import org.sonatype.nexus.common.app.ApplicationLicense;
import org.sonatype.nexus.common.time.DateHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OSS {@link ApplicationLicense}.
 *
 * @since 3.0
 */
@Named("OSS")
@Singleton
public class ApplicationLicenseImpl
    extends AbstractApplicationLicense
    implements ApplicationLicense
{

  private final MultiProductPreferenceFactory productPreferenceFactory;

  private final LicenseContent licenseContent;

  @Inject
  protected ApplicationLicenseImpl(
      final LicenseFingerprinter fingerprinter,
      final List<Feature> availableFeatures,
      final MultiProductPreferenceFactory productPreferenceFactory,
      final LicenseContent licenseContent)
  {
    super(fingerprinter, availableFeatures);
    this.productPreferenceFactory = checkNotNull(productPreferenceFactory);
    this.licenseContent = checkNotNull(licenseContent);
    refresh();
  }

  @Override
  public boolean isRequired() {
    return trialPeriodHasEnded();
  }

  @Override
  public final void refresh() {
    setLicenseKey(licenseContent.key());
  }

  @Override
  public Map<String, Object> getAttributes() {
    return Collections.emptyMap();
  }

  private boolean trialPeriodHasEnded() {
    productPreferenceFactory.setProduct(NexusCommunityFeature.SHORT_NAME);
    Preferences preferences = productPreferenceFactory.nodeForPath("");

    Long componentTotalCountExceeded = preferences.getLong(AnalyticsConstants.COMPONENT_TOTAL_COUNT_EXCEEDED, 0);
    Long peakRequestsPerDayExceeded = preferences.getLong(AnalyticsConstants.PEAK_REQUESTS_PER_DAY_EXCEEDED, 0);

    Date oldestDate =
        DateHelper.oldestDateFromLongs(Arrays.asList(componentTotalCountExceeded, peakRequestsPerDayExceeded));
    if (oldestDate == null) {
      return false;
    }

    return DateHelper.daysElapsed(oldestDate, new Date()) > AnalyticsConstants.TRIAL_PERIOD_DAYS;
  }
}
