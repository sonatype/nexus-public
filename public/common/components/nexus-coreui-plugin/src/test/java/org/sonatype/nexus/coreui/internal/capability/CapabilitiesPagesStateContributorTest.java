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
package org.sonatype.nexus.coreui.internal.capability;

import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.common.app.FeatureFlags.EXTJS_CAPABILITIES_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.REACT_CAPABILITIES_ENABLED;

public class CapabilitiesPagesStateContributorTest
{
  @Test
  public void shouldReturnBothFlagsEnabled() {
    CapabilitiesPagesStateContributor underTest = new CapabilitiesPagesStateContributor(true, true);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(EXTJS_CAPABILITIES_ENABLED), is(true));
    assertThat(state.get(REACT_CAPABILITIES_ENABLED), is(true));
  }

  @Test
  public void shouldReturnBothFlagsDisabled() {
    CapabilitiesPagesStateContributor underTest = new CapabilitiesPagesStateContributor(false, false);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(EXTJS_CAPABILITIES_ENABLED), is(false));
    assertThat(state.get(REACT_CAPABILITIES_ENABLED), is(false));
  }

  @Test
  public void shouldReturnExtjsEnabledAndReactDisabled() {
    CapabilitiesPagesStateContributor underTest = new CapabilitiesPagesStateContributor(true, false);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(EXTJS_CAPABILITIES_ENABLED), is(true));
    assertThat(state.get(REACT_CAPABILITIES_ENABLED), is(false));
  }

  @Test
  public void shouldReturnExtjsDisabledAndReactEnabled() {
    CapabilitiesPagesStateContributor underTest = new CapabilitiesPagesStateContributor(false, true);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(EXTJS_CAPABILITIES_ENABLED), is(false));
    assertThat(state.get(REACT_CAPABILITIES_ENABLED), is(true));
  }
}
