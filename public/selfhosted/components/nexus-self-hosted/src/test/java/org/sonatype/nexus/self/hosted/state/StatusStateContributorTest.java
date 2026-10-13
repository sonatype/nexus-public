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
package org.sonatype.nexus.self.hosted.state;

import java.util.Map;

import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEdition;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.rapture.internal.state.StatusXO;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the {@code getState()} contract for the self-hosted state contributor: NEXUS-54781 adds
 * {@code distribution} and {@code editionId} as sibling top-level keys of the pre-existing {@code status}
 * key and preserves the {@link StatusXO} payload (version, edition, build revision, build timestamp).
 */
class StatusStateContributorTest
{
  private static final String EXPECTED_EDITION_ID = "nexus-professional-edition";

  private static final String EXPECTED_VERSION = "3.99.0-SNAPSHOT";

  private static final String EXPECTED_BUILD_REVISION = "abc123";

  private static final String EXPECTED_BUILD_TIMESTAMP = "2026-09-07T12:00:00Z";

  @Test
  void shouldPublishStatusWithVersionEditionAndBuildInfo() {
    Map<String, Object> state = newContributor("PRO", EXPECTED_EDITION_ID).getState();

    StatusXO status = (StatusXO) state.get("status");
    assertThat(status.getVersion(), is(EXPECTED_VERSION));
    assertThat(status.getEdition(), is("PRO"));
    assertThat(status.getBuildRevision(), is(EXPECTED_BUILD_REVISION));
    assertThat(status.getBuildTimestamp(), is(EXPECTED_BUILD_TIMESTAMP));
  }

  @Test
  void shouldPublishSelfHostedDistribution() {
    Map<String, Object> state = newContributor("PRO", EXPECTED_EDITION_ID).getState();

    assertThat(state.get("distribution"), is("selfhosted"));
  }

  @Test
  void shouldPublishActiveEditionId() {
    Map<String, Object> state = newContributor("PRO", EXPECTED_EDITION_ID).getState();

    assertThat(state.get("editionId"), is(EXPECTED_EDITION_ID));
  }

  @Test
  void shouldPublishExactlyThreeTopLevelKeys() {
    Map<String, Object> state = newContributor("PRO", EXPECTED_EDITION_ID).getState();

    assertThat(state.keySet(), containsInAnyOrder("status", "distribution", "editionId"));
  }

  @Test
  void shouldReportProPlusEditionShortNameUnchanged() {
    Map<String, Object> state = newContributor("PROPLUS", EXPECTED_EDITION_ID).getState();

    assertThat(((StatusXO) state.get("status")).getEdition(), is("PROPLUS"));
  }

  @Test
  void shouldReportEnterpriseEditionShortNameUnchanged() {
    Map<String, Object> state = newContributor("ENTERPRISE", EXPECTED_EDITION_ID).getState();

    assertThat(((StatusXO) state.get("status")).getEdition(), is("ENTERPRISE"));
  }

  @Test
  void shouldReportCommunityEditionShortNameUnchanged() {
    Map<String, Object> state = newContributor("COMMUNITY", "nexus-community-edition").getState();

    assertThat(((StatusXO) state.get("status")).getEdition(), is("COMMUNITY"));
  }

  @Test
  void shouldOmitEditionIdWhenSelectorThrows() {
    ApplicationVersion applicationVersion = mock(ApplicationVersion.class);
    when(applicationVersion.getVersion()).thenReturn(EXPECTED_VERSION);
    when(applicationVersion.getEdition()).thenReturn("PRO");
    when(applicationVersion.getBuildRevision()).thenReturn(EXPECTED_BUILD_REVISION);
    when(applicationVersion.getBuildTimestamp()).thenReturn(EXPECTED_BUILD_TIMESTAMP);

    NexusEditionSelector nexusEditionSelector = mock(NexusEditionSelector.class);
    when(nexusEditionSelector.getCurrent()).thenThrow(new IllegalStateException("no active edition"));

    Map<String, Object> state = new StatusStateContributor(applicationVersion, nexusEditionSelector).getState();

    assertThat(state.keySet(), containsInAnyOrder("status", "distribution"));
  }

  private static StatusStateContributor newContributor(final String reportedEdition, final String editionId) {
    ApplicationVersion applicationVersion = mock(ApplicationVersion.class);
    when(applicationVersion.getVersion()).thenReturn(EXPECTED_VERSION);
    when(applicationVersion.getEdition()).thenReturn(reportedEdition);
    when(applicationVersion.getBuildRevision()).thenReturn(EXPECTED_BUILD_REVISION);
    when(applicationVersion.getBuildTimestamp()).thenReturn(EXPECTED_BUILD_TIMESTAMP);

    NexusEdition edition = mock(NexusEdition.class);
    when(edition.getId()).thenReturn(editionId);

    NexusEditionSelector nexusEditionSelector = mock(NexusEditionSelector.class);
    when(nexusEditionSelector.getCurrent()).thenReturn(edition);

    return new StatusStateContributor(applicationVersion, nexusEditionSelector);
  }
}
