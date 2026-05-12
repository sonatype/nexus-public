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
package org.sonatype.nexus.security.internal;

import java.util.List;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.common.UserPasswordChangedDistributedEvent;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmConfigurationEvent;
import org.sonatype.nexus.security.realm.RealmConfigurationStore;
import org.sonatype.nexus.security.realm.TestRealmConfiguration;

import jakarta.inject.Provider;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealmManagerImplTest
{
  @Mock
  private Provider<RealmConfiguration> initialRealmConfigurationProvider;

  @Mock
  private EventManager eventManager;

  @Mock
  private RealmConfigurationStore configStore;

  @Mock
  private RealmSecurityManager securityManager;

  @Mock(name = "A")
  private Realm realmA;

  @Mock(name = "B")
  private Realm realmB;

  @Mock
  private RealmConfigurationEvent configEvent;

  @Mock
  private AuthenticatingRealmImpl authenticatingRealm;

  @Mock
  private UserPasswordChangedDistributedEvent passwordChangedEvent;

  private RealmManagerImpl manager;

  @BeforeEach
  void setUp() {
    List<Realm> realms = List.of(realmA, realmB);
    RealmConfiguration defaultConfig = new TestRealmConfiguration();
    defaultConfig.setRealmNames(List.of("A"));
    lenient().when(initialRealmConfigurationProvider.get()).thenReturn(defaultConfig);
    manager = new RealmManagerImpl(eventManager, configStore, initialRealmConfigurationProvider, securityManager,
        realms, false);
  }

  @Test
  void testOnStoreChanged_LocalEvent() {
    when(configEvent.isLocal()).thenReturn(true);
    manager.on(configEvent);
    verifyNoInteractions(eventManager, configStore);
  }

  @Test
  void testOnStoreChanged_RemoteEvent() {
    RealmConfiguration reloadedConfig = new TestRealmConfiguration();
    reloadedConfig.setRealmNames(List.of("B"));
    when(configEvent.isLocal()).thenReturn(false);
    when(configStore.load()).thenReturn(reloadedConfig);

    manager.on(configEvent);

    verify(configStore).load();
    verify(securityManager).setRealms(List.of(realmB));
  }

  @Test
  void testOnUserPasswordChanged() {
    when(passwordChangedEvent.isLocal()).thenReturn(false);
    when(passwordChangedEvent.isClearCache()).thenReturn(true);
    when(passwordChangedEvent.getUserId()).thenReturn("testuser");
    when(securityManager.getRealms()).thenReturn(List.of(authenticatingRealm));

    manager.on(passwordChangedEvent);

    verify(authenticatingRealm).clearCache("testuser");
  }

  @Test
  void testOnUserPasswordChanged_LocalEvent() {
    when(passwordChangedEvent.isLocal()).thenReturn(true);

    manager.on(passwordChangedEvent);

    verifyNoInteractions(authenticatingRealm);
  }

  @Test
  void testOnUserPasswordChanged_ClearCache() {
    when(passwordChangedEvent.isLocal()).thenReturn(false);
    when(passwordChangedEvent.isClearCache()).thenReturn(false);

    manager.on(passwordChangedEvent);

    verifyNoInteractions(authenticatingRealm);
  }
}
