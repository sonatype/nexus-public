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
package org.sonatype.nexus.rapture.internal.system.status

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.app.ApplicationLicense
import org.sonatype.nexus.common.app.ApplicationLicense.Attributes
import org.sonatype.nexus.rapture.StateContributor
import org.sonatype.nexus.rapture.internal.state.LicenseXO

import com.codahale.metrics.health.HealthCheck
import com.google.common.collect.ImmutableMap
import groovy.time.TimeCategory

import static com.codahale.metrics.health.HealthCheck.Result
import static java.lang.String.format
/**
 * Health check that indicates if the license is properly configured and valid
 *
 * @since 3.next
 */
@Named("Licensing")
@Singleton
class LicenseHealthCheck
    extends HealthCheck
    implements StateContributor
{
  private final ApplicationLicense applicationLicenseProvider

  private static final String STATE_ID = "license"

  @Inject
  LicenseHealthCheck(final ApplicationLicense applicationLicenseProvider) {
    this.applicationLicenseProvider = applicationLicenseProvider
  }

  @Override
  protected Result check() {
    ApplicationLicense applicationLicense = applicationLicenseProvider

    return applicationLicense == null || isHealthy(applicationLicense) ? Result.healthy() : Result
        .unhealthy(reason(applicationLicense))
  }

  private static boolean isHealthy(final ApplicationLicense applicationLicense) {
    return applicationLicense.isValid() && applicationLicense.isInstalled() && !applicationLicense.isExpired()
  }

  private static String reason(final ApplicationLicense applicationLicense) {
    return format("Expired: %s, Installed: %s, Valid: %s", applicationLicense.isExpired(),
        applicationLicense.isInstalled(), applicationLicense.isValid())
  }

  @Nullable
  @Override
  Map<String, Object> getState() {
    return ImmutableMap.of(STATE_ID, calculateLicense())
  }

  private Object calculateLicense() {
    LicenseXO result = new LicenseXO()
    result.setRequired(applicationLicenseProvider.isRequired())
    result.setInstalled(applicationLicenseProvider.isInstalled())
    result.setValid(applicationLicenseProvider.isValid())
    Map<String, Object> attributes = applicationLicenseProvider.getAttributes()
    if (attributes && attributes.get(Attributes.EXPIRATION_DATE.getKey())) {
      use(TimeCategory) {
        def duration = attributes.get(Attributes.EXPIRATION_DATE.getKey()) - new Date()
        result.setDaysToExpiry(duration.days)
      }
    }
    if (attributes && attributes.get(Attributes.FEATURES.getKey())) {
      result.setFeatures(attributes.get(Attributes.FEATURES.getKey()))
    }
    else {
      result.setFeatures(Collections.emptyList())
    }
    return result
  }
}