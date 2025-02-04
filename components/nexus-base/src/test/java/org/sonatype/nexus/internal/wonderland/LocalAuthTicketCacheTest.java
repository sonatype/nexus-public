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
 * Tests for {@link LocalAuthTicketCache}.
 */
public class LocalAuthTicketCacheTest
    extends TestSupport
{
  @Test
  public void addRemoveIsRemoved() {
    LocalAuthTicketCache tokens = new LocalAuthTicketCache();
    tokens.add("user", "foo", "NexusAuthorizingRealm");
    assertThat(tokens.remove("user", "foo", "NexusAuthorizingRealm"), is(true));
  }

  @Test
  public void neverAddedRemove() {
    LocalAuthTicketCache tokens = new LocalAuthTicketCache();
    assertThat(tokens.remove("user", "foo", "NexusAuthorizingRealm"), is(false));
  }

  @Test(expected = IllegalStateException.class)
  public void addDoesNotAllowDuplicates() {
    LocalAuthTicketCache tokens = new LocalAuthTicketCache();
    tokens.add("user", "foo", "NexusAuthorizingRealm");
    // this should fail
    tokens.add("user", "foo", "NexusAuthorizingRealm");
  }

  @Test
  public void testExpiredRemoveIsFalse() {
    final AtomicBoolean expired = new AtomicBoolean(false);
    LocalAuthTicketCache tokens = new LocalAuthTicketCache()
    {
      @Override
      protected boolean isTokenExpired(final long now, final Entry<UserAuthToken, Long> entry) {
        return expired.get();
      }
    };

    tokens.add("user", "foo", "NexusAuthorizingRealm");
    assertThat(tokens.remove("user", "foo", "NexusAuthorizingRealm"), is(true));

    tokens.add("user", "foo", "NexusAuthorizingRealm");
    // simulate expire
    expired.set(true);
    assertThat(tokens.remove("user", "foo", "NexusAuthorizingRealm"), is(false));
  }

  @Test
  public void testRemoveFailsIfDifferentUser() {
    LocalAuthTicketCache cache = new LocalAuthTicketCache();
    cache.add("user", "foo", "NexusAuthorizingRealm");
    assertThat(cache.remove("bad", "foo", "NexusAuthorizingRealm"), is(false));
  }
}
