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
package org.sonatype.nexus.internal.wonderland;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link AuthTicketCache}.
 */
public class AuthTicketCacheTest
    extends TestSupport
{
  @Test
  public void addRemoveIsRemoved() {
    AuthTicketCache tokens = new AuthTicketCache();
    tokens.add("foo");
    assertThat(tokens.remove("foo"), is(true));
  }

  @Test
  public void neverAddedRemove() {
    AuthTicketCache tokens = new AuthTicketCache();
    assertThat(tokens.remove("foo"), is(false));
  }

  @Test(expected = IllegalStateException.class)
  public void addDoesNotAllowDuplicates() {
    AuthTicketCache tokens = new AuthTicketCache();
    tokens.add("foo");
    // this should fail
    tokens.add("foo");
  }

  @Test
  public void testExpiredRemoveIsFalse() {
    final AtomicBoolean expired = new AtomicBoolean(false);
    AuthTicketCache tokens = new AuthTicketCache()
    {
      @Override
      protected boolean isTokenExpired(long now, Entry<String, Long> entry) {
        return expired.get();
      }
    };

    tokens.add("foo");
    assertThat(tokens.remove("foo"), is(true));

    tokens.add("foo");
    // simulate expire
    expired.set(true);
    assertThat(tokens.remove("foo"), is(false));
  }
}
