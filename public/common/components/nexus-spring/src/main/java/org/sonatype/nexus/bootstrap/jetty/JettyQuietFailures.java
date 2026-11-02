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

import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import com.google.common.base.Throwables;
import org.eclipse.jetty.io.QuietException;

/**
 * Recognises request failures that originate outside the application and so carry no diagnostic value for operators.
 */
public final class JettyQuietFailures
{
  // the only idle-timeout wording Jetty produces (org.eclipse.jetty.io.IdleTimeout)
  private static final String IDLE_TIMEOUT_MESSAGE_PREFIX = "Idle timeout expired:";

  private JettyQuietFailures() {
    // static utility
  }

  /**
   * Jetty uses {@link QuietException} for failures outside the application such as the client disconnecting or invalid
   * encoding. A blocking write that exceeds the connection idle timeout is equally client-side but is not a
   * {@code QuietException}: {@code IdleTimeout} raises a plain {@link TimeoutException} which {@code IO.rethrow} wraps
   * in an {@code IOException}.
   */
  public static boolean isQuiet(final Throwable failure) {
    if (failure == null) {
      return false;
    }
    if (QuietException.isQuiet(failure) || isIdleTimeout(failure)) {
      return true;
    }
    return Stream.of(failure.getSuppressed())
        .anyMatch(suppressed -> QuietException.isQuiet(suppressed) || isIdleTimeout(suppressed));
  }

  /*
   * Timeouts waiting on server-side dependencies also reach us as a TimeoutException, so matching on type alone would
   * silence real failures operators need to see. Only Jetty's own idle-timeout wording counts as quiet.
   */
  private static boolean isIdleTimeout(final Throwable failure) {
    return Throwables.getCausalChain(failure)
        .stream()
        .anyMatch(cause -> cause instanceof TimeoutException
            && cause.getMessage() != null
            && cause.getMessage().startsWith(IDLE_TIMEOUT_MESSAGE_PREFIX));
  }
}
