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
package org.sonatype.nexus.internal.event;

import org.sonatype.nexus.common.event.EventManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link DebugEventInspector}'s conditional event-bus register/unregister
 * dispatch. Covers state transitions in both directions plus idempotency in both states.
 */
@ExtendWith(MockitoExtension.class)
class DebugEventInspectorTest
{
  @Mock
  private EventManager eventManager;

  private DebugEventInspector underTest;

  @BeforeEach
  void setup() {
    underTest = new DebugEventInspector(eventManager);
    reset(eventManager);
  }

  @Test
  void shouldNotInteractWithEventManagerDuringConstructionWhenDefaultDisabled() {
    DebugEventInspector freshlyConstructed = new DebugEventInspector(eventManager);

    verifyNoInteractions(eventManager);
    assertThat(freshlyConstructed.isEnabled(), is(false));
  }

  @Test
  void shouldRegisterWithEventManagerWhenEnabledFromDisabledState() {
    underTest.setEnabled(true);

    verify(eventManager).register(underTest);
    verify(eventManager, never()).unregister(underTest);
    assertThat(underTest.isEnabled(), is(true));
  }

  @Test
  void shouldUnregisterFromEventManagerWhenDisabledFromEnabledState() {
    underTest.setEnabled(true);
    reset(eventManager);

    underTest.setEnabled(false);

    verify(eventManager).unregister(underTest);
    verify(eventManager, never()).register(underTest);
    assertThat(underTest.isEnabled(), is(false));
  }

  @Test
  void shouldNotRegisterAgainWhenAlreadyEnabled() {
    underTest.setEnabled(true);
    reset(eventManager);

    underTest.setEnabled(true);

    verifyNoInteractions(eventManager);
    assertThat(underTest.isEnabled(), is(true));
  }

  @Test
  void shouldNotUnregisterWhenAlreadyDisabled() {
    underTest.setEnabled(false);

    verifyNoInteractions(eventManager);
    assertThat(underTest.isEnabled(), is(false));
  }
}
