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
package org.sonatype.nexus.coreui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.common.Description;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.Realm;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RealmSettingsComponent}.
 */
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class RealmSettingsComponentTest
    extends Test5Support
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private RealmManager realmManager;

  @Mock
  private ApplicationContext applicationContext;

  private RealmSettingsComponent underTest;

  @BeforeEach
  void setUp() {
    underTest = new RealmSettingsComponent(realmManager);
    underTest.setApplicationContext(applicationContext);
  }

  @Test
  void testRead_returnsConfiguredRealms() {
    List<String> configuredRealms = List.of("NexusAuthenticatingRealm", "NexusAuthorizingRealm", "LdapRealm");
    when(realmManager.getConfiguredRealmIds()).thenReturn(configuredRealms);

    RealmSettingsXO result = underTest.read();

    assertThat(result, is(notNullValue()));
    assertThat(result.getRealms(), hasSize(3));
    assertThat(result.getRealms().get(0), is("NexusAuthenticatingRealm"));
    assertThat(result.getRealms().get(1), is("NexusAuthorizingRealm"));
    assertThat(result.getRealms().get(2), is("LdapRealm"));
    verify(realmManager).getConfiguredRealmIds();
  }

  @Test
  void testRead_emptyRealms() {
    when(realmManager.getConfiguredRealmIds()).thenReturn(List.of());

    RealmSettingsXO result = underTest.read();

    assertThat(result, is(notNullValue()));
    assertThat(result.getRealms(), hasSize(0));
  }

  @Test
  void testReadRealmTypes_returnsRealmBeans() {
    Map<String, Realm> realmBeans = new LinkedHashMap<>();
    realmBeans.put("NexusAuthenticatingRealm", new AlphaRealm());
    realmBeans.put("LdapRealm", new BetaRealm());
    when(applicationContext.getBeansOfType(Realm.class)).thenReturn(realmBeans);

    List<ReferenceXO> result = underTest.readRealmTypes();

    assertThat(result, hasSize(2));
    // IDs are the bean names from the application context
    // Sorted alphabetically by name (description)
    assertThat(result.get(0).getId(), is("NexusAuthenticatingRealm"));
    assertThat(result.get(0).getName(), is("Alpha Realm"));
    assertThat(result.get(1).getId(), is("LdapRealm"));
    assertThat(result.get(1).getName(), is("Beta Realm"));
    verify(applicationContext).getBeansOfType(Realm.class);
  }

  @Test
  void testReadRealmTypes_sortedAlphabeticallyIgnoringCase() {
    Map<String, Realm> realmBeans = new LinkedHashMap<>();
    realmBeans.put("ZetaRealmBean", new ZetaRealm());
    realmBeans.put("AlphaRealmBean", new AlphaRealm());
    realmBeans.put("MidRealmBean", new MidRealm());
    when(applicationContext.getBeansOfType(Realm.class)).thenReturn(realmBeans);

    List<ReferenceXO> result = underTest.readRealmTypes();

    assertThat(result, hasSize(3));
    assertThat(result.get(0).getName(), is("Alpha Realm"));
    assertThat(result.get(1).getName(), is("Mid Realm"));
    assertThat(result.get(2).getName(), is("Zeta Realm"));
  }

  @Test
  void testReadRealmTypes_emptyBeans() {
    when(applicationContext.getBeansOfType(Realm.class)).thenReturn(Map.of());

    List<ReferenceXO> result = underTest.readRealmTypes();

    assertThat(result, hasSize(0));
  }

  @Test
  void testUpdate_setsConfiguredRealmsAndReturnsUpdated() {
    RealmSettingsXO settingsXO = new RealmSettingsXO();
    settingsXO.setRealms(List.of("NexusAuthenticatingRealm", "LdapRealm"));

    List<String> updatedRealms = List.of("NexusAuthenticatingRealm", "LdapRealm");
    when(realmManager.getConfiguredRealmIds()).thenReturn(updatedRealms);

    RealmSettingsXO result = underTest.update(settingsXO);

    verify(realmManager).setConfiguredRealmIds(List.of("NexusAuthenticatingRealm", "LdapRealm"));
    assertThat(result, is(notNullValue()));
    assertThat(result.getRealms(), hasSize(2));
    assertThat(result.getRealms().get(0), is("NexusAuthenticatingRealm"));
    assertThat(result.getRealms().get(1), is("LdapRealm"));
  }

  @Test
  void testUpdate_emptyRealmList() {
    RealmSettingsXO settingsXO = new RealmSettingsXO();
    settingsXO.setRealms(List.of());

    when(realmManager.getConfiguredRealmIds()).thenReturn(List.of());

    RealmSettingsXO result = underTest.update(settingsXO);

    verify(realmManager).setConfiguredRealmIds(List.of());
    assertThat(result, is(notNullValue()));
    assertThat(result.getRealms(), hasSize(0));
  }

  @Test
  void testSetApplicationContext_setsContext() {
    ApplicationContext newContext = mock(ApplicationContext.class);
    Map<String, Realm> realmBeans = Map.of();
    when(newContext.getBeansOfType(Realm.class)).thenReturn(realmBeans);

    underTest.setApplicationContext(newContext);

    // Verify the new context is used by calling readRealmTypes
    underTest.readRealmTypes();
    verify(newContext).getBeansOfType(Realm.class);
  }

  // Stub Realm implementations with @Description for QualifierUtil.description() to work

  @Description("Alpha Realm")
  static class AlphaRealm
      implements Realm
  {
    @Override
    public String getName() {
      return "alpha";
    }

    @Override
    public boolean supports(AuthenticationToken token) {
      return false;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      return null;
    }
  }

  @Description("Beta Realm")
  static class BetaRealm
      implements Realm
  {
    @Override
    public String getName() {
      return "beta";
    }

    @Override
    public boolean supports(AuthenticationToken token) {
      return false;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      return null;
    }
  }

  @Description("Mid Realm")
  static class MidRealm
      implements Realm
  {
    @Override
    public String getName() {
      return "mid";
    }

    @Override
    public boolean supports(AuthenticationToken token) {
      return false;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      return null;
    }
  }

  @Description("Zeta Realm")
  static class ZetaRealm
      implements Realm
  {
    @Override
    public String getName() {
      return "zeta";
    }

    @Override
    public boolean supports(AuthenticationToken token) {
      return false;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      return null;
    }
  }
}
