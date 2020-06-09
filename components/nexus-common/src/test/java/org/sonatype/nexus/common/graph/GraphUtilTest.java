package org.sonatype.nexus.common.graph;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
  public void depthRequiresNoSelfLoops() {
    MutableGraph<String> directedGraphWithSelfLoopsAllowed  = GraphBuilder.directed().allowsSelfLoops(true).build();

    GraphUtil.depth(directedGraphWithSelfLoopsAllowed, "", 0);
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
