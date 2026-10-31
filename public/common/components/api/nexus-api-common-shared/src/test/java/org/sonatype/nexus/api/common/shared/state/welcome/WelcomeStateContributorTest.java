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
package org.sonatype.nexus.api.common.shared.state.welcome;

import java.util.Map;

import org.sonatype.nexus.common.node.NodeAccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.api.common.shared.state.welcome.WelcomeStateContributor.NODE_ID;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WelcomeStateContributorTest
{
  @Mock
  private NodeAccess nodeAccess;

  private WelcomeStateContributor underTest;

  @BeforeEach
  public void setUp() {
    when(nodeAccess.getId()).thenReturn("test-node-id");
  }

  @Test
  public void shouldReturnFeatureFlagEnabled() {
    underTest = new WelcomeStateContributor(true, nodeAccess);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get("nexus.react.welcome"), is(true));
  }

  @Test
  public void shouldReturnFeatureFlagDisabled() {
    underTest = new WelcomeStateContributor(false, nodeAccess);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get("nexus.react.welcome"), is(false));
  }

  @Test
  public void shouldReturnNodeId() {
    underTest = new WelcomeStateContributor(true, nodeAccess);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(NODE_ID), is("test-node-id"));
  }
}
