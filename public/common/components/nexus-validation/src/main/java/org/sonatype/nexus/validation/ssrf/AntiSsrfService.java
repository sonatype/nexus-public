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
package org.sonatype.nexus.validation.ssrf;

import jakarta.validation.ValidationException;

/**
 * Service for SSRF protection validation and configuration management.
 */
public interface AntiSsrfService
{
  /**
   * Validates a hostname or IP address. Results are cached.
   *
   * @param host the hostname or IP address to validate
   * @throws ValidationException if the host is blocked by SSRF protection
   */
  void validateHost(String host);

  /**
   * Validates a hostname or IP address without caching, for use during repository or blob-store
   * configuration saves where a fresh evaluation against the current config is needed.
   *
   * @param host the hostname or IP address to validate
   * @throws ValidationException if the host is blocked by SSRF protection
   */
  void validateHostWithoutCache(String host);

  /**
   * Returns the current SSRF protection configuration.
   */
  SsrfProtectionConfiguration getConfiguration();

  /**
   * Updates the SSRF protection configuration.
   *
   * @param configuration the new configuration to apply
   */
  void updateConfiguration(SsrfProtectionConfiguration configuration);
}
