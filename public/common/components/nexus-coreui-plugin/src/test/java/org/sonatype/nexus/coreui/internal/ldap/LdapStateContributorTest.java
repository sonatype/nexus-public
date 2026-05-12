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
package org.sonatype.nexus.coreui.internal.ldap;

import java.util.Map;

import org.sonatype.nexus.security.realm.RealmManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LdapStateContributorTest
{
  private static final String LDAP_REALM_NAME = "LdapRealm";

  @Mock
  private RealmManager realmManager;

  private LdapStateContributor underTest;

  @Before
  public void setUp() {
    underTest = new LdapStateContributor(false, 3, realmManager);
  }

  @Test
  public void shouldReturnLdapRealmDisabledWhenRealmIsNotEnabled() {
    when(realmManager.isRealmEnabled(LDAP_REALM_NAME)).thenReturn(false);

    Map<String, Object> state = underTest.getState();

    assertThat(state, hasEntry(is("ldapRealmEnabled"), is(false)));
    verify(realmManager).isRealmEnabled(LDAP_REALM_NAME);
  }

  @Test
  public void shouldReturnLdapRealmEnabledWhenRealmIsEnabled() {
    when(realmManager.isRealmEnabled(LDAP_REALM_NAME)).thenReturn(true);

    Map<String, Object> state = underTest.getState();

    assertThat(state, hasEntry(is("ldapRealmEnabled"), is(true)));
    verify(realmManager).isRealmEnabled(LDAP_REALM_NAME);
  }

  @Test
  public void shouldReturnFeatureFlagsInState() {
    when(realmManager.isRealmEnabled(LDAP_REALM_NAME)).thenReturn(false);

    Map<String, Object> state = underTest.getState();

    assertThat(state, hasEntry(is("nexus.react.ldap"), is(false)));
    assertThat(state, hasEntry(is("nexus.ldap.mapped.role.query.character.limit"), is(3)));
  }
}
