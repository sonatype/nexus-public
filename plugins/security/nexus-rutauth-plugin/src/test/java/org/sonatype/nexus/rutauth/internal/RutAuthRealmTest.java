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
package org.sonatype.nexus.rutauth.internal;

import org.sonatype.security.SecuritySystem;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.authc.AuthenticationInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

/**
 * Rut Auth Realm UT
 * 
 * @since 2.8
 */
public class RutAuthRealmTest
    extends TestSupport
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private UserManager redUserManager;

  @Mock
  private UserManager blueUserManager;

  private RutAuthRealm testSubject;

  @Before
  public void prepare() throws Exception {
    when(securitySystem.getRealms()).thenReturn(ImmutableList.of("RED", "GREEN", "BLUE"));

    when(redUserManager.getAuthenticationRealmName()).thenReturn("RED");
    when(blueUserManager.getAuthenticationRealmName()).thenReturn("BLUE");

    // GREEN has no UserManager
    testSubject = new RutAuthRealm(securitySystem, ImmutableList.of(redUserManager, blueUserManager));
  }

  @Test
  public void noSuchUser() throws Exception {
    // elvis not exists in any of those
    when(redUserManager.getUser("elvis")).thenThrow(UserNotFoundException.class);
    when(blueUserManager.getUser("elvis")).thenThrow(UserNotFoundException.class);

    final AuthenticationInfo auinfo = testSubject.getAuthenticationInfo(new RutAuthAuthenticationToken("Some-Header",
        "elvis", "localhost"));
    assertThat(auinfo, nullValue());
  }

  @Test
  public void multipleRealms() throws Exception {
    final User redUser = mock(User.class);
    when(redUser.getUserId()).thenReturn("joe");
    final User blueUser = mock(User.class);
    when(blueUser.getUserId()).thenReturn("joe");
    when(redUserManager.getUser("joe")).thenReturn(redUser);
    when(blueUserManager.getUser("joe")).thenReturn(blueUser);

    final AuthenticationInfo auinfo = testSubject.getAuthenticationInfo(new RutAuthAuthenticationToken("Some-Header",
        "joe", "localhost"));

    assertThat(auinfo, notNullValue());
    assertThat(auinfo.getPrincipals().getPrimaryPrincipal().toString(), equalTo("joe"));
    assertThat(auinfo.getPrincipals().getRealmNames(), hasItems("RED", "BLUE"));
  }

  @Test
  public void onlyInOne() throws Exception {
    final User redUser = mock(User.class);
    when(redUser.getUserId()).thenReturn("joe");
    when(redUserManager.getUser("joe")).thenReturn(redUser);
    when(blueUserManager.getUser("joe")).thenThrow(UserNotFoundException.class);

    final AuthenticationInfo auinfo = testSubject.getAuthenticationInfo(new RutAuthAuthenticationToken("Some-Header",
        "joe", "localhost"));

    assertThat(auinfo, notNullValue());
    assertThat(auinfo.getPrincipals().getPrimaryPrincipal().toString(), equalTo("joe"));
    assertThat(auinfo.getPrincipals().getRealmNames(), hasItems("RED"));
  }

}
