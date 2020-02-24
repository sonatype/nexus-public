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
package org.sonatype.nexus.security.authc;

import com.google.common.collect.Lists;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FirstSuccessfulModularRealAuthenticatorTest
{
  private FirstSuccessfulModularRealmAuthenticator firstSuccessfulModularRealmAuthenticator;

  @Before
  public void init() {
    firstSuccessfulModularRealmAuthenticator =
        new FirstSuccessfulModularRealmAuthenticator();
  }

  @Test
  public void testMultiRealmInvalidCredentials() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    Realm realmOne = mock(Realm.class);
    Realm realmTwo = mock(Realm.class);

    when(realmOne.supports(usernamePasswordToken)).thenReturn(true);
    when(realmTwo.supports(usernamePasswordToken)).thenReturn(true);

    when(realmOne.getAuthenticationInfo(usernamePasswordToken)).thenThrow(new IncorrectCredentialsException());
    when(realmTwo.getAuthenticationInfo(usernamePasswordToken)).thenThrow(new IncorrectCredentialsException());

    try {
      firstSuccessfulModularRealmAuthenticator
          .doMultiRealmAuthentication(Lists.newArrayList(realmOne, realmTwo), usernamePasswordToken);
    }
    catch (NexusAuthenticationException e) {
      assertThat(e.getAuthenticationFailureReasons(), containsInAnyOrder(AuthenticationFailureReason.INCORRECT_CREDENTIALS));
    }
  }

  @Test
  public void testMultiRealmMultipleFailures() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    Realm realmOne = mock(Realm.class);
    Realm realmTwo = mock(Realm.class);

    when(realmOne.supports(usernamePasswordToken)).thenReturn(true);
    when(realmTwo.supports(usernamePasswordToken)).thenReturn(true);

    when(realmOne.getAuthenticationInfo(usernamePasswordToken)).thenThrow(new IncorrectCredentialsException());
    when(realmTwo.getAuthenticationInfo(usernamePasswordToken)).thenThrow(new UnknownAccountException());

    try {
      firstSuccessfulModularRealmAuthenticator
          .doMultiRealmAuthentication(Lists.newArrayList(realmOne, realmTwo), usernamePasswordToken);
    }
    catch (NexusAuthenticationException e) {
      assertThat(e.getAuthenticationFailureReasons(), containsInAnyOrder(AuthenticationFailureReason.INCORRECT_CREDENTIALS, AuthenticationFailureReason.USER_NOT_FOUND));
    }
  }

  @Test
  public void testSingleRealmFailureIsStillSuccessful() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    Realm realmOne = mock(Realm.class);
    Realm realmTwo = mock(Realm.class);

    when(realmOne.supports(usernamePasswordToken)).thenReturn(true);
    when(realmTwo.supports(usernamePasswordToken)).thenReturn(true);

    when(realmOne.getAuthenticationInfo(usernamePasswordToken)).thenThrow(new IncorrectCredentialsException());
    when(realmTwo.getAuthenticationInfo(usernamePasswordToken)).thenReturn(new SimpleAccount());

    firstSuccessfulModularRealmAuthenticator
        .doMultiRealmAuthentication(Lists.newArrayList(realmOne, realmTwo), usernamePasswordToken);
  }
}
