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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to hold the calculated base URL of the current request.
 *
 * @since 2.8
 */
public final class BaseUrlHolder
{
  private static final Logger log = LoggerFactory.getLogger(BaseUrlHolder.class);

  // TODO: Sort out if we want a java.net.URL or if String is fine

  private static final InheritableThreadLocal<String> value = new InheritableThreadLocal<>();

  private BaseUrlHolder() {}

  /**
   * Set the current base URL.
   *
   * The value will be normalized to never end with "/".
   */
  public static void set(String url) {
    checkNotNull(url);

    // strip off trailing "/", note this is done so that script/template can easily $baseUrl/foo
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }

    log.trace("Set: {}", url);
    value.set(url);
  }

  /**
   * Returns the current base URL; never null.
   *
   * @throws IllegalStateException
   */
  public static String get() {
    String url = value.get();
    checkState(url != null, "Base URL not set");
    return url;
  }

  public static void unset() {
    log.trace("Unset");
    value.remove();
  }

  public static boolean isSet() {
    return value.get() != null;
  }
}
