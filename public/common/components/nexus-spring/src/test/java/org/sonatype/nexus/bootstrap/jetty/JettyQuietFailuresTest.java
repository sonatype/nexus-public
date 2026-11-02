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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link JettyQuietFailures}.
 */
class JettyQuietFailuresTest
{
  @Test
  void nullIsNotQuiet() {
    assertThat(JettyQuietFailures.isQuiet(null), is(false));
  }

  @Test
  void jettyQuietExceptionIsQuiet() {
    assertThat(JettyQuietFailures.isQuiet(new EofException()), is(true));
    assertThat(JettyQuietFailures.isQuiet(new BadMessageException(400, "Unable to parse form content")), is(true));
  }

  @Test
  void wrappedJettyQuietExceptionIsQuiet() {
    assertThat(JettyQuietFailures.isQuiet(new IllegalStateException(new EofException())), is(true));
  }

  @Test
  void suppressedJettyQuietExceptionIsQuiet() {
    IOException failure = new IOException("write failed");
    failure.addSuppressed(new EofException());

    assertThat(JettyQuietFailures.isQuiet(failure), is(true));
  }

  @Test
  void idleTimeoutWriteFailureIsQuiet() {
    assertThat(JettyQuietFailures.isQuiet(idleTimeoutWriteFailure()), is(true));
  }

  @Test
  void suppressedIdleTimeoutWriteFailureIsQuiet() {
    IOException failure = new IOException("write failed");
    failure.addSuppressed(idleTimeoutWriteFailure());

    assertThat(JettyQuietFailures.isQuiet(failure), is(true));
  }

  /**
   * The PyPI group serve path rewraps failures in {@code UncheckedIOException}, pushing the idle timeout more than one
   * cause deep.
   */
  @Test
  void deeplyNestedIdleTimeoutIsQuiet() {
    assertThat(JettyQuietFailures.isQuiet(new UncheckedIOException(idleTimeoutWriteFailure())), is(true));
  }

  /**
   * A timeout waiting on a server-side dependency is a real failure operators need to see. The Azure blob store
   * rethrows Reactor read timeouts in the same {@code IOException(TimeoutException)} shape a client write timeout
   * produces, so only Jetty's own idle-timeout wording may count as quiet.
   */
  @Test
  void serverSideReadTimeoutIsNotQuiet() {
    IOException readTimeout =
        new IOException(new TimeoutException("Did not observe any item or terminal signal within 30000ms"));

    assertThat(JettyQuietFailures.isQuiet(readTimeout), is(false));
  }

  @Test
  void timeoutWithoutMessageIsNotQuiet() {
    assertThat(JettyQuietFailures.isQuiet(new IOException(new TimeoutException())), is(false));
  }

  @Test
  void unrelatedFailureIsNotQuiet() {
    assertThat(JettyQuietFailures.isQuiet(new IllegalStateException("boom")), is(false));
  }

  /**
   * The shape Jetty produces when a blocking write exceeds the connection idle timeout: IdleTimeout raises a plain
   * TimeoutException which IO.rethrow wraps in an IOException.
   */
  private static IOException idleTimeoutWriteFailure() {
    return new IOException(new TimeoutException("Idle timeout expired: 30000/30000 ms"));
  }
}
