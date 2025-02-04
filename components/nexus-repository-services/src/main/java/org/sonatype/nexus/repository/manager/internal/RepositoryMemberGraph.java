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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
public class RepositoryMemberGraph
    extends ComponentSupport
{
  private final RepositoryManager repositoryManager;

  private final Type groupType;

  @Inject
  public RepositoryMemberGraph(final RepositoryManager repositoryManager, @Named(GroupType.NAME) final Type type) {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.groupType = checkNotNull(type);
  }

  public Graph<Repository> renderAllRepos() {
    Graph<Repository>[] graphs = StreamSupport.stream(repositoryManager.browse().spliterator(), false)
        .map(this::render)
        .toArray(Graph[]::new);

    return combineGraphs(graphs);
  }

  public Graph<Repository> render(Repository repository) {
    MutableGraph<Repository> graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    processRepository(graph, repository, null);
    return graph;
  }

  public Graph<Repository> combineGraphs(Graph<Repository>... graphs) {
    MutableGraph<Repository> graph = GraphBuilder.directed().allowsSelfLoops(true).build();
    for (Graph<Repository> subGraph : graphs) {
      subGraph.nodes().forEach(graph::addNode);
      subGraph.edges().forEach(edge -> graph.putEdge(edge.source(), edge.target()));
    }
    return graph;
  }

  public String toDotGraph(MutableGraph<Repository> graph) {
    try {
      List<String> nodes = graph.nodes()
          .stream()
          .map(node -> "\"%s\" [%s]".formatted(node.getName(), nodeColor(node)))
          .sorted()
          .toList();
      List<String> edges = graph.edges()
          .stream()
          .map(edge -> "\"%s\" -> \"%s\"".formatted(edge.source().getName(), edge.target().getName()))
          .sorted()
          .toList();
      return printDigraph(nodes, edges);
    }
    catch (Exception e) {
      log.error("Failed to convert graph to dot", e);
      return null;
    }
  }

  private String printDigraph(List<String> nodes, List<String> edges) {
    return String.format("digraph { %s }", String.join("; ", nodes) + "; " + String.join("; ", edges));
  }

  private void processRepository(MutableGraph<Repository> graph, Repository repository, Repository parentRepository) {
    boolean isGroup = groupType.equals(repository.getType());
    if (isGroup && graph.nodes().contains(repository)) {
      throw new IllegalArgumentException(
          String.format("Group repository already processed, indicates a cycle in the graph for : %s",
              repository.getName()));
    }

    if (!graph.addNode(repository)) {
      log.warn("This repository appears more than once in the group graph: {}", repository.getName());
    }
    if (parentRepository != null) {
      graph.putEdge(parentRepository, repository);
    }

    if (isGroup) {
      Optional.ofNullable(repository.getConfiguration().attributes("group"))
          .map(attributes -> attributes.get("memberNames"))
          .map(memberNames -> (List<String>) memberNames)
          .ifPresent(memberNames -> memberNames.stream()
              .map(repositoryManager::get)
              .filter(Objects::nonNull)
              .forEach(memberRepository -> processRepository(graph, memberRepository, repository)));
    }
  }

  private String nodeColor(Repository repository) {
    return switch (repository.getType().getValue()) {
      case "group" -> "color=red";
      case "hosted" -> "color=blue";
      case "proxy" -> "color=green";
      default -> "color=black";
    };
  }
}
