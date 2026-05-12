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
package org.sonatype.nexus.coreui.internal.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.node.NodeAccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeAccessComponentTest
{
  @Mock
  NodeAccess nodeAccess;

  @InjectMocks
  NodeAccessComponent underTest;

  @Test
  void nodesReturnsSingleLocalNode() {
    String nodeId = "05F4743F-A7565846";
    Map<String, String> aliases = new LinkedHashMap<>();
    aliases.put(nodeId, "Node-1");

    when(nodeAccess.getMemberAliases()).thenReturn(aliases);
    when(nodeAccess.getId()).thenReturn(nodeId);

    List<NodeInfoXO> result = underTest.nodes();

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getName(), is(nodeId));
    assertThat(result.get(0).getLocal(), is(true));
    assertThat(result.get(0).getDisplayName(), is("Node-1"));
  }

  @Test
  void nodesReturnsMultipleNodesWithCorrectLocalFlag() {
    String localNodeId = "05F4743F-A7565846";
    String remoteNodeId = "12345678-ABCDEF01";

    Map<String, String> aliases = new LinkedHashMap<>();
    aliases.put(localNodeId, "Local-Node");
    aliases.put(remoteNodeId, "Remote-Node");

    when(nodeAccess.getMemberAliases()).thenReturn(aliases);
    when(nodeAccess.getId()).thenReturn(localNodeId);

    List<NodeInfoXO> result = underTest.nodes();

    assertThat(result, hasSize(2));

    assertThat(result.get(0).getName(), is(localNodeId));
    assertThat(result.get(0).getLocal(), is(true));
    assertThat(result.get(0).getDisplayName(), is("Local-Node"));

    assertThat(result.get(1).getName(), is(remoteNodeId));
    assertThat(result.get(1).getLocal(), is(false));
    assertThat(result.get(1).getDisplayName(), is("Remote-Node"));
  }

  @Test
  void nodesReturnsEmptyListWhenNoMembers() {
    when(nodeAccess.getMemberAliases()).thenReturn(Collections.emptyMap());

    List<NodeInfoXO> result = underTest.nodes();

    assertThat(result, is(empty()));
  }

  @Test
  void nodesUsesNodeIdAsDisplayNameWhenAliasMatchesId() {
    String nodeId = "05F4743F-A7565846";
    Map<String, String> aliases = new LinkedHashMap<>();
    aliases.put(nodeId, nodeId);

    when(nodeAccess.getMemberAliases()).thenReturn(aliases);
    when(nodeAccess.getId()).thenReturn(nodeId);

    List<NodeInfoXO> result = underTest.nodes();

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getName(), is(nodeId));
    assertThat(result.get(0).getDisplayName(), is(nodeId));
    assertThat(result.get(0).getLocal(), is(true));
  }
}
