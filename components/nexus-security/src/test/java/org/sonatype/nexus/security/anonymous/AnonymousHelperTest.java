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
package org.sonatype.nexus.security.anonymous;

import java.util.Arrays;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;
import org.sonatype.nexus.security.internal.DefaultRealmConstants;
import org.sonatype.nexus.security.user.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnonymousHelper}.
 */
public class AnonymousHelperTest
    extends TestSupport
{
  private static final String TEST_REALM = "TEST";

  @Mock
  private UserManager userManager1;

  @Mock
  private UserManager userManagerAuth;

  @Before
  public void setup() {
    when(userManager1.getAuthenticationRealmName()).thenReturn(TEST_REALM);
    when(userManagerAuth.getAuthenticationRealmName()).thenReturn(DefaultRealmConstants.DEFAULT_REALM_NAME);
  }

  @Test
  public void testGetAuthenticationRealms() {
    List<UserManager> userManagers = Arrays.asList(userManager1, userManagerAuth);

    List<String> realms = AnonymousHelper.getAuthenticationRealms(userManagers);

    assertThat(realms, hasItem(userManager1.getAuthenticationRealmName()));
    assertThat(realms, not(hasItem(userManagerAuth.getAuthenticationRealmName())));
    assertThat(realms, hasItem(AuthorizingRealmImpl.NAME));
  }
}
