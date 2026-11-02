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
package org.sonatype.nexus.node.datastore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Answers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link NodeHeartbeatManager#hasVersionSkew()} default method, which
 * {@link org.sonatype.nexus.node.upgrade.MigrationTaskSupport#oldNodesRunning()} and
 * {@code ReNormalizationTaskLauncher} both rely on to detect an in-progress rolling upgrade.
 */
public class NodeHeartbeatManagerTest
{
  // real default method body is exercised via CALLS_REAL_METHODS, unlike a plain mock which would stub it away
  private final NodeHeartbeatManager underTest = mock(NodeHeartbeatManager.class, Answers.CALLS_REAL_METHODS);

  @Test
  public void testNoSkewWhenAllActiveNodesReportSameVersion() {
    List<NodeHeartbeat> heartbeats = List.of(
        heartbeatFor("3.93.0-SNAPSHOT", "abc123"),
        heartbeatFor("3.93.0-SNAPSHOT", "abc123"));
    when(underTest.getActiveNodeHeartbeatData()).thenReturn(heartbeats);

    assertThat(underTest.hasVersionSkew(), is(false));
  }

  @Test
  public void testSkewWhenActiveNodesReportDifferentVersions() {
    List<NodeHeartbeat> heartbeats = List.of(
        heartbeatFor("3.93.0-SNAPSHOT", "abc123"),
        heartbeatFor("3.88.0", "old456"));
    when(underTest.getActiveNodeHeartbeatData()).thenReturn(heartbeats);

    assertThat(underTest.hasVersionSkew(), is(true));
  }

  @Test
  public void testSkewWhenActiveNodesShareVersionButDifferentBuildRevision() {
    List<NodeHeartbeat> heartbeats = List.of(
        heartbeatFor("3.93.0-SNAPSHOT", "abc123"),
        heartbeatFor("3.93.0-SNAPSHOT", "def456"));
    when(underTest.getActiveNodeHeartbeatData()).thenReturn(heartbeats);

    assertThat(underTest.hasVersionSkew(), is(true));
  }

  @Test
  public void testNoSkewForSingleNodeDeployment() {
    List<NodeHeartbeat> heartbeats = List.of(heartbeatFor("3.93.0-SNAPSHOT", "abc123"));
    when(underTest.getActiveNodeHeartbeatData()).thenReturn(heartbeats);

    assertThat(underTest.hasVersionSkew(), is(false));
  }

  @Test
  public void testNoSkewWhenNoActiveNodesReportSystemInfo() {
    when(underTest.getActiveNodeHeartbeatData()).thenReturn(List.of());

    assertThat(underTest.hasVersionSkew(), is(false));
  }

  @Test
  public void testNoSkewWhenAHeartbeatIsMissingVersion() {
    List<NodeHeartbeat> heartbeats = List.of(
        heartbeatFor("3.93.0-SNAPSHOT", "abc123"),
        heartbeatMissingAttribute(NodeHeartbeatManager.VERSION_ATTRIBUTE, "abc123"));
    when(underTest.getActiveNodeHeartbeatData()).thenReturn(heartbeats);

    assertThat(underTest.hasVersionSkew(), is(false));
  }

  @Test
  public void testNoSkewWhenAHeartbeatIsMissingBuildRevision() {
    List<NodeHeartbeat> heartbeats = List.of(
        heartbeatFor("3.93.0-SNAPSHOT", "abc123"),
        heartbeatMissingAttribute(NodeHeartbeatManager.BUILD_REVISION_ATTRIBUTE, "3.93.0-SNAPSHOT"));
    when(underTest.getActiveNodeHeartbeatData()).thenReturn(heartbeats);

    assertThat(underTest.hasVersionSkew(), is(false));
  }

  private static NodeHeartbeat heartbeatFor(final String version, final String buildRevision) {
    Map<String, Object> systemInfo = new HashMap<>();
    systemInfo.put(NodeHeartbeatManager.NEXUS_STATUS_KEY,
        Map.of(NodeHeartbeatManager.VERSION_ATTRIBUTE, version,
            NodeHeartbeatManager.BUILD_REVISION_ATTRIBUTE, buildRevision));

    NodeHeartbeat heartbeat = mock(NodeHeartbeat.class);
    when(heartbeat.systemInfo()).thenReturn(systemInfo);
    return heartbeat;
  }

  // builds a heartbeat whose nexus-status map carries only the one attribute other than missingAttribute,
  // simulating a partial/stale heartbeat row rather than a well-formed one missing both fields
  private static NodeHeartbeat heartbeatMissingAttribute(final String missingAttribute, final String presentValue) {
    String presentAttribute = missingAttribute.equals(NodeHeartbeatManager.VERSION_ATTRIBUTE)
        ? NodeHeartbeatManager.BUILD_REVISION_ATTRIBUTE
        : NodeHeartbeatManager.VERSION_ATTRIBUTE;

    Map<String, Object> systemInfo = new HashMap<>();
    systemInfo.put(NodeHeartbeatManager.NEXUS_STATUS_KEY, Map.of(presentAttribute, presentValue));

    NodeHeartbeat heartbeat = mock(NodeHeartbeat.class);
    when(heartbeat.systemInfo()).thenReturn(systemInfo);
    return heartbeat;
  }
}
