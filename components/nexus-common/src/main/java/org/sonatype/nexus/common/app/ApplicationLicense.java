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
package org.sonatype.nexus.common.app;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Provide information about the application license.
 *
 * Exposed for informative use only, replacing this component does not actually change how licensing is applied.
 *
 * @since 3.0
 */
public interface ApplicationLicense
{
  /**
   * Returns {@code true} if license is required for operation.
   */
  boolean isRequired();

  /**
   * Returns {@code true} if license is installed and valid.
   */
  boolean isValid();

  /**
   * Returns {@code true} if license is installed.
   */
  boolean isInstalled();

  /**
   * Returns {@code true} if license is installed and has expired.
   */
  boolean isExpired();

  /**
   * Returns license attributes.
   *
   * This is an immutable view of additional information about the current license.
   * Could be an empty-map if no license was installed.
   *
   */
  Map<String,Object> getAttributes();

  /**
   * Returns the license finger-print, or {@code null} if not installed.
   */
  @Nullable
  String getFingerprint();

  /**
   * Refreshes the cached license details with the latest from the license manager.
   *
   * @since 3.16
   */
  void refresh();

  /**
   * Keys for Attribute values associated with this license.
   */
  enum Attributes {
    EVAL("evaluation"),
    USERS("users"),
    FEATURES("features"),
    EFFECTIVE_FEATURES("effectiveFeatures"),
    EFFECTIVE_DATE("effectiveDate"),
    EXPIRATION_DATE("expirationDate"),
    CONTACT_NAME("contactName"),
    CONTACT_EMAIL("contactEmail"),
    CONTACT_COMPANY("contactCompany"),
    CONTACT_COUNTRY("contactCountry");

    private final String key;

    Attributes(final String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }
}
