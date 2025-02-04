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

import java.util.concurrent.Callable;
import java.util.function.Supplier;

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

  private static final InheritableThreadLocal<String> baseUrl = new InheritableThreadLocal<>();

  private static final InheritableThreadLocal<String> relativePath = new InheritableThreadLocal<>();

  private BaseUrlHolder() {
    // empty
  }

  /**
   * Set the current base URL, and the relative path.
   *
   * The value will be normalized to never end with "/".
   */
  public static void set(final String url, final String newRelativePath) {
    checkNotNull(url);
    checkNotNull(newRelativePath);

    String strippedUrl = stripSlash(url);
    String strippedRelativePath = stripSlash(newRelativePath);

    log.trace("Set: {}", strippedUrl);
    baseUrl.set(strippedUrl);

    log.trace("Set relativePath: {}", strippedRelativePath);
    relativePath.set(strippedRelativePath);
  }

  private static String stripSlash(final String url) {
    // strip off trailing "/", note this is done so that script/template can easily $baseUrl/foo
    if (url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }

  /**
   * Returns the current base URL; never null.
   *
   * @throws IllegalStateException
   */
  public static String get() {
    String url = baseUrl.get();
    checkState(url != null, "Base URL not set");
    return url;
  }

  public static String getRelativePath() {
    String url = relativePath.get();
    checkState(url != null, "Relative path not set");
    return url;
  }

  public static void unset() {
    log.trace("Unset");
    baseUrl.remove();
    relativePath.remove();
  }

  public static boolean isSet() {
    return baseUrl.get() != null;
  }

  public static <R> R with(final String url, final String relative, final Supplier<R> operation) {
    set(url, relative);
    try {
      return operation.get();
    }
    finally {
      unset();
    }
  }

  public static <R> R call(final String url, final String relative, final Callable<R> operation) throws Exception {
    set(url, relative);
    try {
      return operation.call();
    }
    finally {
      unset();
    }
  }
}
