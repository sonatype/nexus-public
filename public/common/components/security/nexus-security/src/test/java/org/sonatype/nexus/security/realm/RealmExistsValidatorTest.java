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
package org.sonatype.nexus.security.realm;

import java.util.Arrays;
import java.util.Collections;

import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RealmExistsValidatorTest
{
  @Mock
  private RealmSecurityManager realmSecurityManager;

  private RealmExistsValidator underTest;

  @Before
  public void setUp() {
    underTest = new RealmExistsValidator(realmSecurityManager);
  }

  /*
   * Null value is considered valid (other constraints such as @NotEmpty handle the null/empty case)
   */
  @Test
  public void isValid_nullValue() {
    assertThat(underTest.isValid(null, null), is(true));
  }

  @Test
  public void isValid_realmExists() {
    Realm realm = mock(Realm.class);
    when(realm.getName()).thenReturn("NexusAuthenticatingRealm");
    when(realmSecurityManager.getRealms()).thenReturn(Collections.singletonList(realm));

    assertThat(underTest.isValid("NexusAuthenticatingRealm", null), is(true));
  }

  @Test
  public void isValid_realmDoesNotExist() {
    Realm realm = mock(Realm.class);
    when(realm.getName()).thenReturn("NexusAuthenticatingRealm");
    when(realmSecurityManager.getRealms()).thenReturn(Collections.singletonList(realm));

    assertThat(underTest.isValid("UnknownRealm", null), is(false));
  }

  @Test
  public void isValid_noRealmsConfigured() {
    when(realmSecurityManager.getRealms()).thenReturn(Collections.emptyList());

    assertThat(underTest.isValid("NexusAuthenticatingRealm", null), is(false));
  }

  @Test
  public void isValid_matchesOneOfMultipleRealms() {
    Realm realm1 = mock(Realm.class);
    when(realm1.getName()).thenReturn("NexusAuthenticatingRealm");
    Realm realm2 = mock(Realm.class);
    when(realm2.getName()).thenReturn("NexusAuthorizingRealm");
    when(realmSecurityManager.getRealms()).thenReturn(Arrays.asList(realm1, realm2));

    assertThat(underTest.isValid("NexusAuthorizingRealm", null), is(true));
  }
}
