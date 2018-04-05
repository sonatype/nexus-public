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
package org.sonatype.nexus.repository.manager.internal

import javax.inject.Inject
import javax.inject.Named

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.types.GroupType

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Graph the relationships of Group Repositories.
 * @since 3.10
 */
@Named
class RepositoryMemberGraph
    extends ComponentSupport
{
  final RepositoryManager repositoryManager

  final Type groupType

  final def quote = { string -> "\"$string\"" }

  final def nodeColor = { Repository repository ->
    switch (repository.type.value) {
      case 'group':
        return 'color=red'
      case 'hosted':
        return 'color=blue'
      case 'proxy':
        return 'color=green'
      default:
        return 'color=black'
    }
  }

  @Inject
  RepositoryMemberGraph(final RepositoryManager repositoryManager, @Named(GroupType.NAME) final Type type) {
    this.repositoryManager = checkNotNull(repositoryManager)
    this.groupType = checkNotNull(type)
  }

  /**
   * Graph all repositories presently configured.
   * @return
   */
  Graph<Repository> renderAllRepos() {
    List<Graph<Repository>> graphs = repositoryManager.browse().collect { Repository repository ->
      render(repository)
    }
    return combineGraphs(graphs.toArray(new Graph<Repository>[graphs.size()]))
  }

  /**
   * Render a Graph structure rooted at the given Repository.
   */
  Graph<Repository> render(Repository repository) {
    MutableGraph<Repository> graph = GraphBuilder.directed().allowsSelfLoops(false).build()
    processRepository(graph, repository, null)     
    return graph
  }

  /**
   * Combine multiple Repository graphs into one structure.
   */
  Graph<Repository> combineGraphs(Graph<Repository>... graphs) {
    MutableGraph<Repository> graph = GraphBuilder.directed().allowsSelfLoops(true).build()
    graphs.each { Graph<Repository> subGraph ->
      subGraph.nodes().each { graph.addNode(it) }
      subGraph.edges().each { EndpointPair<Repository> edge -> graph.putEdge(edge.source(), edge.target()) }
    }
    return graph
  }

  /**
   * Convert a graph structure into the .dot language. 
   * @param graph
   * @return
   */
  String toDotGraph(MutableGraph<Repository> graph) {
    try {
      List<String> nodes = graph.nodes().collect { Repository node -> "${quote(node.name)} [${nodeColor(node)}]" }.sort()
      List<String> edges = graph.edges().collect { EndpointPair<Repository> edge ->
        "${quote(edge.source().name)} -> ${quote(edge.target().name)}"
      }.sort()
      printDigraph(nodes+edges)
    }
    catch (e) {
      log.error('Failed to convert graph to dot',e)
    }
  }

  private String printDigraph(List<String> lines) {
    "digraph { ${lines.join('; ')} }"
  }

  private void processRepository(MutableGraph<Repository> graph, Repository repository, Repository parentRepository) {
    boolean isGroup = repository.type.value == 'group'
    if (isGroup && graph.nodes().contains(repository)) {
      throw new IllegalArgumentException(
          "Group repository already processed, indicates a cycle in the graph for : ${repository.name}")
    }

    if (!graph.addNode(repository)) {
      log.warn("This repository appears more than once in the group graph: ${repository.name}")
    }
    if (parentRepository) {
      graph.putEdge(parentRepository, repository)
    }

    if (isGroup) {
      repository.configuration.attributes('group').get('memberNames').each { String member ->
        Repository memberRepository = repositoryManager.get(member)
        processRepository(graph, memberRepository, repository)
      }
    }
  }
}
