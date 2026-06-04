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
package org.sonatype.nexus.bootstrap.core;

import java.util.Map;

import org.sonatype.nexus.common.app.ApplicationLicense;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import static java.util.Collections.emptyMap;

/**
 * CORE {@link ApplicationLicense}.
 */
@Component
@ConditionalOnProperty(value = "nexus.edition", havingValue = "CORE", matchIfMissing = true)
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
    return emptyMap();
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

  @Override
  public boolean isEvaluation() {
    return false;
  }
}
