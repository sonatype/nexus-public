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

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationLicense;

/**
 * OSS {@link ApplicationLicense}.
 *
 * @since 3.0
 */
@Named("OSS")
@Singleton
public class ApplicationLicenseImpl
  implements ApplicationLicense
{
  /**
   * Always {@code false}.
   */
  @Override
  public boolean isRequired() {
    return false;
  }

  /**
   * Always {@code false}.
   */
  @Override
  public boolean isValid() {
    return false;
  }

  /**
   * Always {@code false}.
   */
  @Override
  public boolean isInstalled() {
    return false;
  }

  /**
   * Always {@code false}.
   */
  @Override
  public boolean isExpired() {
    return false;
  }

  /**
   * Always empty-map.
   */
  @Override
  public Map<String, Object> getAttributes() {
    return Collections.emptyMap();
  }

  /**
   * Always {@code null}.
   */
  @Override
  @Nullable
  public String getFingerprint() {
    return null;
  }

  @Override
  public void refresh() {
    // no-op
  }
}
