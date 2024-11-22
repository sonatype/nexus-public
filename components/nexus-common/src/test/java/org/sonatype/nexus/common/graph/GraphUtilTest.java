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
package org.sonatype.nexus.common.graph;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GraphUtilTest
{
  private static final String ROOT_NODE = "root";

  private static final String TEST_NODE = "test";

  @Test(expected = IllegalStateException.class)
  public void depthRequiresADirectedGraph() {
    Graph<String> undirectedGraph = GraphBuilder.undirected().allowsSelfLoops(false).build();

    GraphUtil.depth(undirectedGraph, "", 0);
  }

  @Test(expected = IllegalStateException.class)
  public void depthRequiresNoLoops() {
    MutableGraph<String> directedGraphWithLoops = GraphBuilder.directed().build();
    directedGraphWithLoops.addNode(ROOT_NODE);
    directedGraphWithLoops.putEdge(ROOT_NODE, TEST_NODE);
    directedGraphWithLoops.putEdge(TEST_NODE, ROOT_NODE);

    GraphUtil.depth(directedGraphWithLoops, "", 0);
  }

  @Test
  public void depthComputesTheDepthForNoChildEdges() {
    MutableGraph<String> graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    graph.addNode(ROOT_NODE);
    graph.putEdge(ROOT_NODE, TEST_NODE);

    assertThat(GraphUtil.depth(graph, TEST_NODE, 0), is(1));
  }

  @Test
  public void depthComputesTheCorrectDepthWithChildren() {
    MutableGraph<String> graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    graph.addNode(ROOT_NODE);
    graph.putEdge(ROOT_NODE, TEST_NODE);
    graph.putEdge(TEST_NODE, "A");
    graph.putEdge(TEST_NODE, "B");
    graph.putEdge(TEST_NODE, "C");
    graph.putEdge("A", "AA");
    graph.putEdge("AA", "AAA");
    graph.putEdge("AAA", "AAAA");

    assertThat(GraphUtil.depth(graph, TEST_NODE, 0), is(5));
    assertThat(GraphUtil.depth(graph, "A", 0), is(4));
  }
}
