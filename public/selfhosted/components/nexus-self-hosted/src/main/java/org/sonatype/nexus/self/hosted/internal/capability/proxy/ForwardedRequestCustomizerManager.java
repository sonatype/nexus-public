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
package org.sonatype.nexus.self.hosted.internal.capability.proxy;

import java.util.Optional;

import org.sonatype.nexus.bootstrap.jetty.CustomForwardedRequestCustomizer;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Manager for the {@link CustomForwardedRequestCustomizer}.
 *
 * <p>
 * This component provides runtime control over forwarded header processing.
 * It's used by the {@link ForwardedRequestCapability} to enable/disable header processing.
 * </p>
 *
 * <p>
 * The manager locates the CustomForwardedRequestCustomizer from the Jetty Server's
 * beans after the application starts.
 * </p>
 */
@Component
public class ForwardedRequestCustomizerManager
{
  private static final Logger log = LoggerFactory.getLogger(ForwardedRequestCustomizerManager.class);

  /**
   * Sets whether forwarded header processing is enabled.
   *
   * @param enabled true to enable processing, false to disable
   */
  public void setEnabled(final boolean enabled) {
    Optional<CustomForwardedRequestCustomizer> instance = CustomForwardedRequestCustomizer.instance();
    if (instance.isEmpty()) {
      if (enabled) {
        throw new IllegalStateException("Unable to detect configured CustomForwardedRequestCustomizer");
      }
      log.info("Forwarded header processing disabled");
      return;
    }

    boolean wasEnabled = instance.get().isEnabled();
    if (wasEnabled != enabled) {
      instance.get().setEnabled(enabled);
      log.info("Forwarded header processing {}", enabled ? "enabled" : "disabled");
    }
  }

  @VisibleForTesting
  boolean isEnabled() {
    return CustomForwardedRequestCustomizer.instance()
        .map(CustomForwardedRequestCustomizer::isEnabled)
        .orElse(false);
  }
}
