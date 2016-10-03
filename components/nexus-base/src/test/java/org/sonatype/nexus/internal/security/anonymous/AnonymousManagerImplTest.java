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
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousConfigurationChangedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AnonymousManagerImplTest
    extends TestSupport
{
  @Mock
  private EventBus eventBus;

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

  private AnonymousManagerImpl manager;

  @Before
  public void setUp() {
    when(defaults.get()).thenReturn(defaultConfig);
    when(storeConfig.copy()).thenReturn(storeConfigCopy);
    when(defaultConfig.copy()).thenReturn(defaultConfigCopy);
    manager = new AnonymousManagerImpl(eventBus, store, defaults);
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
    verify(eventBus).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getConfiguration(), is(storeConfigCopy));
  }
}
