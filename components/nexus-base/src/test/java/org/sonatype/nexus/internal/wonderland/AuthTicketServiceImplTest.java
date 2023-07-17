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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.wonderland.AuthTicketCache;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthTicketServiceImpl}.
 */
public class AuthTicketServiceImplTest
    extends TestSupport
{
  @Mock
  private AuthTicketGenerator authTicketGenerator;

  @Mock
  private AuthTicketCache authTicketCache;

  private AuthTicketServiceImpl underTest;

  @Before
  public void setup() {
    when(authTicketGenerator.generate()).thenReturn("ticket");
    when(authTicketCache.remove("user", "ticket", "NexusAuthorizingRealm")).thenReturn(true);

    underTest = new AuthTicketServiceImpl(authTicketGenerator, authTicketCache);
  }

  @Test
  public void testRedeemTicket_sameUser() {
    String ticket = underTest.createTicket("user", "NexusAuthorizingRealm");
    assertThat(underTest.redeemTicket("user", ticket, "NexusAuthorizingRealm"), is(true));
  }

  @Test
  public void testRedeemTicket_differentUser() {
    String ticket = underTest.createTicket("user", "NexusAuthorizingRealm");
    assertThat(underTest.redeemTicket("bad", ticket, "NexusAuthorizingRealm"), is(false));
  }
}
