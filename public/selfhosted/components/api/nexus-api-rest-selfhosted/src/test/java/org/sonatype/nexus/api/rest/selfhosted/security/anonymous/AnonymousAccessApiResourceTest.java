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
package org.sonatype.nexus.api.rest.selfhosted.security.anonymous;

import java.io.IOException;

import org.sonatype.nexus.api.rest.selfhosted.security.anonymous.model.AnonymousAccessSettingsXO;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.TestAnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class AnonymousAccessApiResourceTest
{
  @Mock
  private AnonymousManager anonymousManager;

  @Mock
  private RealmSecurityManager realmSecurityManager;

  private AnonymousAccessApiResource underTest;

  private AnonymousConfiguration initialAnonymousConfiguration = new TestAnonymousConfiguration();

  @BeforeEach
  void setup() {
    initialAnonymousConfiguration.setEnabled(true);
    initialAnonymousConfiguration.setUserId(AnonymousConfiguration.DEFAULT_USER_ID);
    initialAnonymousConfiguration.setRealmName(AnonymousConfiguration.DEFAULT_REALM_NAME);

    lenient().when(anonymousManager.newConfiguration()).thenReturn(initialAnonymousConfiguration);
    lenient().when(anonymousManager.getConfiguration()).thenReturn(initialAnonymousConfiguration);

    Realm realm = mock(Realm.class);
    lenient().when(realm.getName()).thenReturn(AnonymousConfiguration.DEFAULT_REALM_NAME);
    lenient().when(realmSecurityManager.getRealms()).thenReturn(Lists.newArrayList(realm));

    underTest = new AnonymousAccessApiResource(anonymousManager, realmSecurityManager);
  }

  @Test
  void testGet() {
    assertThat(underTest.read(), is(new AnonymousAccessSettingsXO(initialAnonymousConfiguration)));
  }

  @Test
  void testUpdate() {
    AnonymousConfiguration newConfiguration = new TestAnonymousConfiguration();
    newConfiguration.setRealmName(AnonymousConfiguration.DEFAULT_REALM_NAME);
    newConfiguration.setUserId(AnonymousConfiguration.DEFAULT_USER_ID);
    newConfiguration.setEnabled(false);

    AnonymousAccessSettingsXO xo = new AnonymousAccessSettingsXO(newConfiguration);
    when(anonymousManager.getConfiguration()).thenReturn(newConfiguration);
    assertThat(underTest.update(xo), is(new AnonymousAccessSettingsXO(newConfiguration)));
  }

  @Test
  void testInvalidRealm() {
    AnonymousConfiguration newConfiguration = new TestAnonymousConfiguration();
    newConfiguration.setRealmName("invalidRealmName");
    newConfiguration.setUserId(AnonymousConfiguration.DEFAULT_USER_ID);
    newConfiguration.setEnabled(false);

    assertThrows(ValidationErrorsException.class,
        () -> underTest.update(new AnonymousAccessSettingsXO(newConfiguration)));
  }

  @Test
  void testDeserialize() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String value = "{\"enabled\": true }";
    AnonymousAccessSettingsXO settings = mapper.readValue(value.getBytes(), AnonymousAccessSettingsXO.class);
    assertThat(settings.isEnabled(), is(true));
  }
}
