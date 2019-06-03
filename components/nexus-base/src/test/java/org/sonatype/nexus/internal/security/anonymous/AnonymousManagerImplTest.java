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
package org.sonatype.nexus.internal.security.anonymous;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousConfigurationChangedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AnonymousManagerImplTest
    extends TestSupport
{
  @Mock
  private EventManager eventManager;

  @Mock
  private AnonymousConfigurationStore store;

  @Mock
  private AnonymousConfiguration storeConfig;

  @Mock
  private AnonymousConfiguration storeConfigCopy;

  @Mock
  private Provider<AnonymousConfiguration> defaults;

  @Mock
  private AnonymousConfiguration defaultConfig;

  @Mock
  private AnonymousConfiguration defaultConfigCopy;

  @Mock
  private AnonymousConfigurationEvent configurationEvent;

  private AnonymousManagerImpl manager;

  @Before
  public void setUp() {
    when(defaults.get()).thenReturn(defaultConfig);
    when(storeConfig.copy()).thenReturn(storeConfigCopy);
    when(storeConfigCopy.copy()).thenReturn(storeConfigCopy);
    when(defaultConfig.copy()).thenReturn(defaultConfigCopy);
    manager = new AnonymousManagerImpl(eventManager, store, defaults);
  }

  @Test
  public void testGetConfiguration_FromDefaults() {
    assertThat(manager.getConfiguration(), is(defaultConfigCopy));
  }

  @Test
  public void testGetConfiguration_FromStore() {
    when(store.load()).thenReturn(storeConfig);
    assertThat(manager.getConfiguration(), is(storeConfigCopy));
    verifyZeroInteractions(defaults);
  }

  @Test
  public void testSetConfiguration() {
    manager.setConfiguration(storeConfig);
    verify(store).save(storeConfigCopy);
    ArgumentCaptor<AnonymousConfigurationChangedEvent> eventCaptor = ArgumentCaptor
        .forClass(AnonymousConfigurationChangedEvent.class);
    verify(eventManager).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getConfiguration(), is(storeConfigCopy));
  }

  @Test
  public void testHandleConfigurationEvent_FromLocalNode() {
    when(configurationEvent.isLocal()).thenReturn(true);
    when(store.load()).thenReturn(defaultConfig, storeConfig);
    assertThat(manager.getConfiguration(), is(defaultConfigCopy));
    manager.onStoreChanged(configurationEvent);
    assertThat(manager.getConfiguration(), is(defaultConfigCopy));
    verify(store).load();
    verify(eventManager, never()).post(any(AnonymousConfigurationChangedEvent.class));
  }

  @Test
  public void testHandleConfigurationEvent_FromRemoteNode() {
    when(configurationEvent.isLocal()).thenReturn(false);
    when(configurationEvent.getAnonymousConfiguration()).thenReturn(storeConfig);
    assertThat(manager.getConfiguration(), is(defaultConfigCopy));
    EventHelper.asReplicating(() -> manager.onStoreChanged(configurationEvent));
    assertThat(manager.getConfiguration(), is(storeConfigCopy));
    verify(store).load();
    verify(store, never()).save(storeConfigCopy);
    verify(eventManager).post(any(AnonymousConfigurationChangedEvent.class));
  }

  @Test
  public void testIsConfigured() {
    when(store.load()).thenReturn(defaultConfig, storeConfig);
    assertTrue(manager.isConfigured());
  }

  @Test
  public void testIsConfigured_unconfigured() {
    assertFalse(manager.isConfigured());
  }
}
