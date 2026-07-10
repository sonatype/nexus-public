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
package org.sonatype.nexus.bootstrap.jetty;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Request;

/**
 * A customizable extension of {@link ForwardedRequestCustomizer} that allows enabling/disabling
 * forwarded header processing at runtime.
 *
 * <p>
 * This class maintains all the functionality of the parent ForwardedRequestCustomizer, but
 * adds an enabled flag. When disabled, the customize method returns the request unchanged,
 * effectively skipping all forwarded header processing.
 * </p>
 *
 * <p>
 * This is used by the ForwardedRequestCapability to allow toggling forwarded header
 * processing via the Nexus UI without requiring server restart.
 * </p>
 */
public class CustomForwardedRequestCustomizer
    extends ForwardedRequestCustomizer
{
  private static volatile CustomForwardedRequestCustomizer INSTANCE;

  private volatile boolean enabled = true;

  public CustomForwardedRequestCustomizer() {
    if (INSTANCE != null) {
      throw new IllegalStateException("CustomForwardedRequestCustomizer was already initialized");
    }
    INSTANCE = this;
  }

  /**
   * Sets whether forwarded header processing is enabled.
   *
   * @param enabled true to enable processing, false to disable
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether forwarded header processing is enabled.
   *
   * @return true if processing is enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Customizes the request based on forwarded headers.
   *
   * <p>
   * When enabled, delegates to the parent implementation to process forwarded headers
   * (X-Forwarded-For, X-Forwarded-Proto, etc.). When disabled, returns the request unchanged,
   * effectively preserving the original connection information.
   * </p>
   *
   * @param request the request to customize
   * @param responseHeaders the response headers (unused by forwarded processing)
   * @return the customized request, or the original request if disabled
   */
  @Override
  public Request customize(final Request request, final Mutable responseHeaders) {
    if (!enabled) {
      return request;
    }
    return super.customize(request, responseHeaders);
  }

  public static Optional<CustomForwardedRequestCustomizer> instance() {
    return Optional.ofNullable(INSTANCE);
  }

  @VisibleForTesting
  public static void clearInstance() {
    INSTANCE = null;
  }
}
