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
package org.sonatype.nexus.coreui.internal;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.common.app.FeatureFlags.CLUSTERED_ZERO_DOWNTIME_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED;

public class ClusteredModeStateContributorTest
    extends TestSupport
{
  @Test
  public void shouldReturnBothFlagsEnabled() {
    ClusteredModeStateContributor underTest = new ClusteredModeStateContributor(true, true);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(DATASTORE_CLUSTERED_ENABLED), is(true));
    assertThat(state.get(CLUSTERED_ZERO_DOWNTIME_ENABLED), is(true));
  }

  @Test
  public void shouldReturnBothFlagsDisabled() {
    ClusteredModeStateContributor underTest = new ClusteredModeStateContributor(false, false);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(DATASTORE_CLUSTERED_ENABLED), is(false));
    assertThat(state.get(CLUSTERED_ZERO_DOWNTIME_ENABLED), is(false));
  }

  @Test
  public void shouldReturnClusteredEnabledAndZeroDowntimeDisabled() {
    ClusteredModeStateContributor underTest = new ClusteredModeStateContributor(true, false);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(DATASTORE_CLUSTERED_ENABLED), is(true));
    assertThat(state.get(CLUSTERED_ZERO_DOWNTIME_ENABLED), is(false));
  }

  @Test
  public void shouldReturnClusteredDisabledAndZeroDowntimeEnabled() {
    ClusteredModeStateContributor underTest = new ClusteredModeStateContributor(false, true);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(DATASTORE_CLUSTERED_ENABLED), is(false));
    assertThat(state.get(CLUSTERED_ZERO_DOWNTIME_ENABLED), is(true));
  }
}
