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
package org.sonatype.nexus.repository.manager.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepositoryMemberGraphTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  private Type groupType;

  private Type hostedType;

  private Type proxyType;

  private RepositoryMemberGraph repositoryMemberGraph;

  @Before
  public void setUp() {
    groupType = new GroupType();
    hostedType = new HostedType();
    proxyType = new ProxyType();
    repositoryMemberGraph = new RepositoryMemberGraph(repositoryManager, groupType);
  }

  @Test
  public void testSingleNodeNonGroupGraph() {
    Repository repository = mock(Repository.class);
    when(repository.getType()).thenReturn(hostedType);

    Graph<Repository> graph = repositoryMemberGraph.render(repository);
    Set<Repository> nodes = graph.nodes();

    assertThat(nodes.size(), is(1));
    assertThat(nodes.iterator().next(), is(repository));
    assertThat(graph.edges().isEmpty(), is(true));
  }

  @Test
  public void testSingleNodeGroupGraph() {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    NestedAttributesMap attributesMap = new NestedAttributesMap("group", Map.of("memberNames", List.of()));

    when(repository.getType()).thenReturn(groupType);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.attributes(anyString())).thenReturn(attributesMap);

    Graph<Repository> graph = repositoryMemberGraph.render(repository);
    Set<Repository> nodes = graph.nodes();

    assertThat(nodes.size(), is(1));
    assertThat(nodes.iterator().next(), is(repository));
    assertThat(graph.edges().isEmpty(), is(true));
  }

  @Test
  public void testGroupNodeWithMembers() {
    Repository group = mock(Repository.class);
    Repository hosted = mock(Repository.class);
    Repository proxy = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    NestedAttributesMap attributesMap =
        new NestedAttributesMap("group", Map.of("memberNames", List.of("hosted", "proxy")));

    MutableGraph<Repository> expected = GraphBuilder.directed().build();
    expected.addNode(group);
    expected.addNode(hosted);
    expected.addNode(proxy);
    expected.putEdge(group, hosted);
    expected.putEdge(group, proxy);

    when(group.getType()).thenReturn(groupType);
    when(group.getConfiguration()).thenReturn(configuration);
    when(configuration.attributes(anyString())).thenReturn(attributesMap);
    when(repositoryManager.get("hosted")).thenReturn(hosted);
    when(repositoryManager.get("proxy")).thenReturn(proxy);
    when(hosted.getType()).thenReturn(hostedType);
    when(proxy.getType()).thenReturn(proxyType);

    Graph<Repository> graph = repositoryMemberGraph.render(group);

    assertThat(graph.nodes(), is(expected.nodes()));
    assertThat(graph.edges(), is(expected.edges()));
  }

  @Test
  public void testGroupNodeWithNestedGroups() {
    Repository group1 = mock(Repository.class);
    Repository hosted1 = mock(Repository.class);
    Repository proxy1 = mock(Repository.class);
    Repository group2 = mock(Repository.class);
    Repository hosted2 = mock(Repository.class);
    Repository proxy2 = mock(Repository.class);
    Repository group3 = mock(Repository.class);
    Repository proxy3 = mock(Repository.class);

    Configuration configuration1 = mock(Configuration.class);
    NestedAttributesMap attributesMap1 =
        new NestedAttributesMap("group", Map.of("memberNames", List.of("hosted1", "proxy1", "group2")));
    Configuration configuration2 = mock(Configuration.class);
    NestedAttributesMap attributesMap2 =
        new NestedAttributesMap("group", Map.of("memberNames", List.of("hosted2", "proxy2", "group3")));
    Configuration configuration3 = mock(Configuration.class);
    NestedAttributesMap attributesMap3 =
        new NestedAttributesMap("group", Map.of("memberNames", List.of("hosted2", "proxy3")));

    MutableGraph<Repository> expected = GraphBuilder.directed().build();
    expected.addNode(group1);
    expected.addNode(hosted1);
    expected.addNode(proxy1);
    expected.addNode(group2);
    expected.addNode(hosted2);
    expected.addNode(proxy2);
    expected.addNode(group3);
    expected.addNode(proxy3);
    expected.putEdge(group1, hosted1);
    expected.putEdge(group1, proxy1);
    expected.putEdge(group1, group2);
    expected.putEdge(group2, hosted2);
    expected.putEdge(group2, proxy2);
    expected.putEdge(group2, group3);
    expected.putEdge(group3, hosted2);
    expected.putEdge(group3, proxy3);

    when(group1.getType()).thenReturn(groupType);
    when(group1.getConfiguration()).thenReturn(configuration1);
    when(configuration1.attributes(anyString())).thenReturn(attributesMap1);
    when(repositoryManager.get("hosted1")).thenReturn(hosted1);
    when(repositoryManager.get("proxy1")).thenReturn(proxy1);
    when(repositoryManager.get("group2")).thenReturn(group2);
    when(hosted1.getType()).thenReturn(hostedType);
    when(proxy1.getType()).thenReturn(proxyType);
    when(group2.getType()).thenReturn(groupType);
    when(group2.getConfiguration()).thenReturn(configuration2);
    when(configuration2.attributes(anyString())).thenReturn(attributesMap2);
    when(repositoryManager.get("hosted2")).thenReturn(hosted2);
    when(repositoryManager.get("proxy2")).thenReturn(proxy2);
    when(repositoryManager.get("group3")).thenReturn(group3);
    when(hosted2.getType()).thenReturn(hostedType);
    when(proxy2.getType()).thenReturn(proxyType);
    when(group3.getType()).thenReturn(groupType);
    when(group3.getConfiguration()).thenReturn(configuration3);
    when(configuration3.attributes(anyString())).thenReturn(attributesMap3);
    when(repositoryManager.get("proxy3")).thenReturn(proxy3);
    when(proxy3.getType()).thenReturn(proxyType);

    Graph<Repository> graph = repositoryMemberGraph.render(group1);

    assertThat(graph.nodes(), is(expected.nodes()));
    assertThat(graph.edges(), is(expected.edges()));
  }

  @Test
  public void testCyclicGraph() {
    Repository group1 = mock(Repository.class);
    Repository group2 = mock(Repository.class);

    Configuration configuration1 = mock(Configuration.class);
    NestedAttributesMap attributesMap1 = new NestedAttributesMap("group", Map.of("memberNames", List.of("group2")));
    Configuration configuration2 = mock(Configuration.class);
    NestedAttributesMap attributesMap2 = new NestedAttributesMap("group", Map.of("memberNames", List.of("group1")));

    when(group1.getType()).thenReturn(groupType);
    when(group1.getName()).thenReturn("group1");
    when(group1.getConfiguration()).thenReturn(configuration1);
    when(configuration1.attributes(anyString())).thenReturn(attributesMap1);
    when(repositoryManager.get("group2")).thenReturn(group2);
    when(group2.getType()).thenReturn(groupType);
    when(group2.getName()).thenReturn("group2");
    when(group2.getConfiguration()).thenReturn(configuration2);
    when(configuration2.attributes(anyString())).thenReturn(attributesMap2);
    when(repositoryManager.get("group1")).thenReturn(group1);

    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> repositoryMemberGraph.render(group1));

    assertThat(expected.getMessage(),
        is("Group repository already processed, indicates a cycle in the graph for : group1"));
  }

  @Test
  public void testCombineGraphs() {
    Repository group1 = mock(Repository.class);
    Repository hosted1 = mock(Repository.class);
    Configuration configuration1 = mock(Configuration.class);
    NestedAttributesMap attributesMap1 = new NestedAttributesMap("group", Map.of("memberNames", List.of("hosted1")));

    Repository group2 = mock(Repository.class);
    Repository hosted2 = mock(Repository.class);
    Configuration configuration2 = mock(Configuration.class);
    NestedAttributesMap attributesMap2 = new NestedAttributesMap("group", Map.of("memberNames", List.of("hosted2")));

    MutableGraph<Repository> expected = GraphBuilder.directed().build();
    expected.addNode(group1);
    expected.addNode(hosted1);
    expected.addNode(group2);
    expected.addNode(hosted2);
    expected.putEdge(group1, hosted1);
    expected.putEdge(group2, hosted2);

    when(group1.getType()).thenReturn(groupType);
    when(hosted1.getType()).thenReturn(hostedType);
    when(group1.getConfiguration()).thenReturn(configuration1);
    when(configuration1.attributes(anyString())).thenReturn(attributesMap1);
    when(repositoryManager.get("group1")).thenReturn(group1);
    when(repositoryManager.get("hosted1")).thenReturn(hosted1);
    when(group2.getType()).thenReturn(groupType);
    when(hosted2.getType()).thenReturn(hostedType);
    when(group2.getConfiguration()).thenReturn(configuration2);
    when(configuration2.attributes(anyString())).thenReturn(attributesMap2);
    when(repositoryManager.get("group2")).thenReturn(group2);
    when(repositoryManager.get("hosted2")).thenReturn(hosted2);

    Graph<Repository> combined = repositoryMemberGraph.combineGraphs(
        repositoryMemberGraph.render(group1),
        repositoryMemberGraph.render(group2));

    assertThat(combined.nodes(), is(expected.nodes()));
    assertThat(combined.edges(), is(expected.edges()));
  }

  @Test
  public void testConvertToDotGraph() {
    Repository group = mock(Repository.class);
    Repository hosted = mock(Repository.class);
    Repository proxy = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    NestedAttributesMap attributesMap =
        new NestedAttributesMap("group", Map.of("memberNames", List.of("hosted", "proxy")));

    MutableGraph<Repository> expected = GraphBuilder.directed().build();
    expected.addNode(group);
    expected.addNode(hosted);
    expected.addNode(proxy);
    expected.putEdge(group, hosted);
    expected.putEdge(group, proxy);

    when(group.getType()).thenReturn(groupType);
    when(group.getConfiguration()).thenReturn(configuration);
    when(configuration.attributes(anyString())).thenReturn(attributesMap);
    when(repositoryManager.get("hosted")).thenReturn(hosted);
    when(repositoryManager.get("proxy")).thenReturn(proxy);
    when(hosted.getType()).thenReturn(hostedType);
    when(proxy.getType()).thenReturn(proxyType);
    when(hosted.getName()).thenReturn("hosted");
    when(proxy.getName()).thenReturn("proxy");
    when(group.getName()).thenReturn("group");

    String dotGraph = repositoryMemberGraph.toDotGraph((MutableGraph<Repository>) repositoryMemberGraph.render(group));

    assertThat(dotGraph,
        is("digraph { \"group\" [color=red]; \"hosted\" [color=blue]; \"proxy\" [color=green]; \"group\" -> \"hosted\"; \"group\" -> \"proxy\" }"));
  }

  @Test
  public void testRenderAllRepositoryGraphsTogether() {
    Repository hosted1 = mock(Repository.class);
    Repository hosted2 = mock(Repository.class);

    MutableGraph<Repository> expected = GraphBuilder.directed().build();
    expected.addNode(hosted1);
    expected.addNode(hosted2);

    when(repositoryManager.browse()).thenReturn(List.of(hosted1, hosted2));
    when(hosted1.getType()).thenReturn(hostedType);
    when(hosted2.getType()).thenReturn(hostedType);
    when(hosted1.getName()).thenReturn("one");
    when(hosted2.getName()).thenReturn("two");

    Graph<Repository> graph = repositoryMemberGraph.renderAllRepos();

    assertThat(graph.nodes(), is(expected.nodes()));
    assertThat(graph.edges(), is(expected.edges()));
  }
}
