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
package org.sonatype.nexus.transaction;

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

/**
 * Test retry controller behaviour.
 */
public class RetryControllerTest
    extends TestSupport
{
  private static final Exception MINOR_CAUSE = new IllegalStateException();

  private static final Exception TEST_CAUSE = new TestException();

  private static final Exception MAJOR_CAUSE = new IOException();

  private RetryController underTest;

  private ArgumentCaptor<Long> backoffCaptor;

  static class TestException
      extends Exception
  {
    // blank
  }

  @Before
  public void setUp() {
    underTest = spy(new RetryController());
    backoffCaptor = ArgumentCaptor.forClass(Long.class);
    doNothing().when(underTest).backoff(backoffCaptor.capture());
  }

  @Test
  public void testRetryLimit() throws Exception {
    assertThat(underTest.allowRetry(0, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(1, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(2, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(3, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(4, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(5, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(6, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(7, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(8, MINOR_CAUSE), is(false));

    underTest.setRetryLimit(3);

    assertThat(underTest.allowRetry(0, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(1, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(2, MINOR_CAUSE), is(true));
    assertThat(underTest.allowRetry(3, MINOR_CAUSE), is(false));
  }

  @Test
  public void testNoBackoffWhenRetryLimitExceeded() throws Exception {
    assertThat(underTest.allowRetry(8, MINOR_CAUSE), is(false));
    assertThat(backoffCaptor.getAllValues(), is(empty()));
  }

  @Test
  public void testMinorDelay() throws Exception {
    verifyBackoff(0, MINOR_CAUSE, 0, 10);
    verifyBackoff(1, MINOR_CAUSE, 0, 30);
    verifyBackoff(2, MINOR_CAUSE, 0, 70);
    verifyBackoff(3, MINOR_CAUSE, 0, 150);
    verifyBackoff(4, MINOR_CAUSE, 0, 310);
    verifyBackoff(5, MINOR_CAUSE, 0, 630);
    verifyBackoff(6, MINOR_CAUSE, 0, 1270);
    verifyBackoff(7, MINOR_CAUSE, 0, 2550);

    underTest.setMinorDelayMillis(1);

    verifyBackoff(0, MINOR_CAUSE, 0, 1);
    verifyBackoff(1, MINOR_CAUSE, 0, 3);
    verifyBackoff(2, MINOR_CAUSE, 0, 7);
    verifyBackoff(3, MINOR_CAUSE, 0, 15);
    verifyBackoff(4, MINOR_CAUSE, 0, 31);
    verifyBackoff(5, MINOR_CAUSE, 0, 63);
    verifyBackoff(6, MINOR_CAUSE, 0, 127);
    verifyBackoff(7, MINOR_CAUSE, 0, 255);

    underTest.setMinSlots(10);
    underTest.setMaxSlots(50);

    verifyBackoff(0, MINOR_CAUSE, 0, 9);
    verifyBackoff(1, MINOR_CAUSE, 0, 9);
    verifyBackoff(2, MINOR_CAUSE, 0, 9);
    verifyBackoff(3, MINOR_CAUSE, 0, 15);
    verifyBackoff(4, MINOR_CAUSE, 0, 31);
    verifyBackoff(5, MINOR_CAUSE, 0, 49);
    verifyBackoff(6, MINOR_CAUSE, 0, 49);
    verifyBackoff(7, MINOR_CAUSE, 0, 49);
  }

  @Test
  public void testMajorDelay() throws Exception {
    verifyBackoff(0, MAJOR_CAUSE, 100, 200);
    verifyBackoff(1, MAJOR_CAUSE, 100, 400);
    verifyBackoff(2, MAJOR_CAUSE, 100, 800);
    verifyBackoff(3, MAJOR_CAUSE, 100, 1600);
    verifyBackoff(4, MAJOR_CAUSE, 100, 3200);
    verifyBackoff(5, MAJOR_CAUSE, 100, 6400);
    verifyBackoff(6, MAJOR_CAUSE, 100, 12800);
    verifyBackoff(7, MAJOR_CAUSE, 100, 25600);

    underTest.setMajorDelayMillis(1);

    verifyBackoff(0, MAJOR_CAUSE, 1, 2);
    verifyBackoff(1, MAJOR_CAUSE, 1, 4);
    verifyBackoff(2, MAJOR_CAUSE, 1, 8);
    verifyBackoff(3, MAJOR_CAUSE, 1, 16);
    verifyBackoff(4, MAJOR_CAUSE, 1, 32);
    verifyBackoff(5, MAJOR_CAUSE, 1, 64);
    verifyBackoff(6, MAJOR_CAUSE, 1, 128);
    verifyBackoff(7, MAJOR_CAUSE, 1, 256);

    underTest.setMinSlots(30);
    underTest.setMaxSlots(80);

    verifyBackoff(0, MAJOR_CAUSE, 1, 30);
    verifyBackoff(1, MAJOR_CAUSE, 1, 30);
    verifyBackoff(2, MAJOR_CAUSE, 1, 30);
    verifyBackoff(3, MAJOR_CAUSE, 1, 30);
    verifyBackoff(4, MAJOR_CAUSE, 1, 32);
    verifyBackoff(5, MAJOR_CAUSE, 1, 64);
    verifyBackoff(6, MAJOR_CAUSE, 1, 80);
    verifyBackoff(7, MAJOR_CAUSE, 1, 80);
  }

  @Test
  public void testMajorFilter() throws Exception {
    verifyBackoff(0, TEST_CAUSE, 0, 10);
    verifyBackoff(1, TEST_CAUSE, 0, 30);
    verifyBackoff(2, TEST_CAUSE, 0, 70);
    verifyBackoff(3, TEST_CAUSE, 0, 150);
    verifyBackoff(4, TEST_CAUSE, 0, 310);
    verifyBackoff(5, TEST_CAUSE, 0, 630);
    verifyBackoff(6, TEST_CAUSE, 0, 1270);
    verifyBackoff(7, TEST_CAUSE, 0, 2550);

    underTest.addAsMajorException(TestException.class);

    verifyBackoff(0, TEST_CAUSE, 100, 200);
    verifyBackoff(1, TEST_CAUSE, 100, 400);
    verifyBackoff(2, TEST_CAUSE, 100, 800);
    verifyBackoff(3, TEST_CAUSE, 100, 1600);
    verifyBackoff(4, TEST_CAUSE, 100, 3200);
    verifyBackoff(5, TEST_CAUSE, 100, 6400);
    verifyBackoff(6, TEST_CAUSE, 100, 12800);
    verifyBackoff(7, TEST_CAUSE, 100, 25600);

    underTest.removeAsMajorException(TestException.class);

    verifyBackoff(0, TEST_CAUSE, 0, 10);
    verifyBackoff(1, TEST_CAUSE, 0, 30);
    verifyBackoff(2, TEST_CAUSE, 0, 70);
    verifyBackoff(3, TEST_CAUSE, 0, 150);
    verifyBackoff(4, TEST_CAUSE, 0, 310);
    verifyBackoff(5, TEST_CAUSE, 0, 630);
    verifyBackoff(6, TEST_CAUSE, 0, 1270);
    verifyBackoff(7, TEST_CAUSE, 0, 2550);
  }

  private void verifyBackoff(final int retriesSoFar,
                             final Exception cause,
                             final long minBackoff,
                             final long maxBackoff)
  {
    underTest.allowRetry(retriesSoFar, cause);

    assertThat(backoffCaptor.getValue(), both(greaterThanOrEqualTo(minBackoff)).and(lessThanOrEqualTo(maxBackoff)));
  }
}
