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
import com.google.common.graph.Graphs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;

/**
 * Additional utility functions for working with {@link com.google.common.graph.Graph}
 *
 * @since 3.25
 */
public class GraphUtil
{
  /**
   * Compute the depth that can be found from a node in a directed graph with no cycles by following all edges "down"
   * the graph until you hit a "leaf" node.
   *
   * @param graph a directed graph
   * @param node a root node to compute the depth for
   * @param initialDepth should be set to 0, recursive calls will use this track the current depth
   * @param <T> the type of node contained by the graph
   * @return the depth reachable from the node (if the node has no child nodes the depth is 1, otherwise it's the
   *         longest path from the node to any child node following the edges of the graph)
   */
  public static <T> int depth(Graph<T> graph, T node, int initialDepth) {
    checkNotNull(graph);
    checkNotNull(node);
    checkState(graph.isDirected(), "unable to compute depth for an undirected graph");
    checkState(!Graphs.hasCycle(graph), "unable to compute depth for a graph with cycles");

    int currentDepth = initialDepth + 1;
    return graph.successors(node)
        .stream()
        .map(successor -> depth(graph, successor, currentDepth))
        .max(comparing(Integer::intValue))
        .orElse(currentDepth);
  }
}
