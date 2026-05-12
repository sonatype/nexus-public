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
package org.sonatype.nexus.coreui.internal.welcome;

import java.util.Map;

import org.sonatype.nexus.common.node.NodeAccess;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.coreui.internal.welcome.WelcomeStateContributor.NODE_ID;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WelcomeStateContributorTest
{
  @Mock
  private NodeAccess nodeAccess;

  private WelcomeStateContributor underTest;

  @Before
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
