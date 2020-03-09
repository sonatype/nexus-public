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

import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.types.ProxyType

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph
import spock.lang.Specification

/**
 * @since 3.10
 */
class RepositoryMemberGraphTest
    extends Specification
{
  RepositoryManager repositoryManager = Mock()

  Type groupType = new GroupType()

  Type hostedType = new HostedType()

  Type proxyType = new ProxyType()

  RepositoryMemberGraph repositoryMemberGraph

  def setup() {
    repositoryMemberGraph = new RepositoryMemberGraph(repositoryManager, groupType)
  }

  def 'Test single node non-group graph'() {
    given: 'A single hosted repository'
      Repository repository = Mock()

    when: 'rendering the graph'
      Graph<Repository> graph = repositoryMemberGraph.render(repository)
      Set<Repository> nodes = graph.nodes()

    then: 'only the hosted repository appears, with no edges'
      nodes.size() == 1
      nodes.iterator().next() == repository
      graph.edges().isEmpty()
      repository.type >> hostedType
  }

  def 'Test single node group graph'() {
    given: 'A single group repository with no members'
      Repository repository = Mock()
      Configuration configuration = Mock()
      NestedAttributesMap attributesMap = new NestedAttributesMap('group', [memberNames: []])

    when: 'rendering the graph'
      Graph<Repository> graph = repositoryMemberGraph.render(repository)
      Set<Repository> nodes = graph.nodes()
    
    then: 'only the group repository appears, with no edges'
      nodes.size() == 1
      nodes.iterator().next() == repository
      graph.edges().isEmpty()
      repository.type >> groupType
      repository.getConfiguration() >> configuration
      configuration.attributes(_) >> attributesMap
  }

  def 'Test group node with members'() {
    given: 'A group repository with hosted and proxy members'
      Repository group = Mock()
      Repository hosted = Mock()
      Repository proxy = Mock()
      Configuration configuration = Mock()
      NestedAttributesMap attributesMap = new NestedAttributesMap('group', [memberNames: ['hosted', 'proxy']])

      MutableGraph<Repository> expected = GraphBuilder.directed().build()
      expected.with {
        addNode(group)
        addNode(hosted)
        addNode(proxy)
        putEdge(group, hosted)
        putEdge(group, proxy)
      }

    when: 'rendering the graph'
      Graph<Repository> graph = repositoryMemberGraph.render(group)

    then: 'all three repositories appear in the graph, with edges from the group to each member'
      graph.nodes() == expected.nodes()
      graph.edges() == expected.edges()

      group.type >> groupType
      group.getConfiguration() >> configuration
      configuration.attributes(_) >> attributesMap
      repositoryManager.get('hosted') >> hosted
      repositoryManager.get('proxy') >> proxy
      hosted.type >> hostedType
      proxy.type >> proxyType
  }

  def 'Test group node with nested groups'() {
    given: 'A group that contains other groups, as well as hosted and proxy repositories'
      Repository group1 = Mock()
      Repository hosted1 = Mock()
      Repository proxy1 = Mock()
      Repository group2 = Mock()
      Repository hosted2 = Mock()
      Repository proxy2 = Mock()
      Repository group3 = Mock()
      Repository proxy3 = Mock()

      Configuration configuration1 = Mock()
      NestedAttributesMap attributesMap1 = new NestedAttributesMap('group',
          [memberNames: ['hosted1', 'proxy1', 'group2']])
      Configuration configuration2 = Mock()
      NestedAttributesMap attributesMap2 = new NestedAttributesMap('group',
          [memberNames: ['hosted2', 'proxy2', 'group3']])
      Configuration configuration3 = Mock()
      NestedAttributesMap attributesMap3 = new NestedAttributesMap('group', [memberNames: ['hosted2', 'proxy3']])

      MutableGraph<Repository> expected = GraphBuilder.directed().build()
      expected.with {
        addNode(group1)
        addNode(hosted1)
        addNode(proxy1)
        addNode(group2)
        addNode(hosted2)
        addNode(proxy2)
        addNode(group3)
        addNode(proxy3)

        putEdge(group1, hosted1)
        putEdge(group1, proxy1)
        putEdge(group1, group2)

        putEdge(group2, hosted2)
        putEdge(group2, proxy2)
        putEdge(group2, group3)

        putEdge(group3, hosted2)
        putEdge(group3, proxy3)
      }

    when: 'rendering the graph'
      Graph<Repository> graph = repositoryMemberGraph.render(group1)
    println graph.nodeOrder()

    then: 'all of the nodes and expected edges are present in the graph'
      graph.nodes() == expected.nodes()
      graph.edges() == expected.edges()

      group1.type >> groupType
      group1.getConfiguration() >> configuration1
      configuration1.attributes(_) >> attributesMap1
      repositoryManager.get('hosted1') >> hosted1
      repositoryManager.get('proxy1') >> proxy1
      repositoryManager.get('group2') >> group2
      hosted1.type >> hostedType
      proxy1.type >> proxyType
      group2.type >> groupType
      group2.getConfiguration() >> configuration2
      configuration2.attributes(_) >> attributesMap2

      repositoryManager.get('hosted2') >> hosted2
      repositoryManager.get('proxy2') >> proxy2
      repositoryManager.get('group3') >> group3
      hosted2.type >> hostedType
      proxy2.type >> proxyType
      group3.type >> groupType
      group3.getConfiguration() >> configuration3
      configuration3.attributes(_) >> attributesMap3

      repositoryManager.get('proxy3') >> proxy3
      proxy3.type >> proxyType
  }

  def 'Test cyclic graph'() {
    given: 'A group repository with a cycle in its membership'
      Repository group1 = Mock()
      Repository group2 = Mock()

      Configuration configuration1 = Mock()
      NestedAttributesMap attributesMap1 = new NestedAttributesMap('group', [memberNames: ['group2']])
      Configuration configuration2 = Mock()
      NestedAttributesMap attributesMap2 = new NestedAttributesMap('group', [memberNames: ['group1']])

    when: 'rendering the graph'
      repositoryMemberGraph.render(group1)

    then: 'an exception is thrown indicating a cycle'
      group1.type >> groupType
      group1.getConfiguration() >> configuration1
      configuration1.attributes(_) >> attributesMap1
      repositoryManager.get('group2') >> group2
      group2.type >> groupType
      group2.getConfiguration() >> configuration2
      configuration2.attributes(_) >> attributesMap2
      repositoryManager.get('group1') >> group1

      IllegalArgumentException illegalArgumentException = thrown(IllegalArgumentException)
      illegalArgumentException.message.contains('indicates a cycle in the graph')
  }
  
  def 'Can combine graphs'() {
    given: 'Two group repositories'
      Repository group1 = Mock()
      Repository hosted1 = Mock()
      Configuration configuration1 = Mock()
      NestedAttributesMap attributesMap1 = new NestedAttributesMap('group', [memberNames: ['hosted1']])

      Repository group2 = Mock()
      Repository hosted2 = Mock()
      Configuration configuration2 = Mock()
      NestedAttributesMap attributesMap2 = new NestedAttributesMap('group', [memberNames: ['hosted2']])

      MutableGraph<Repository> expected = GraphBuilder.directed().build()
      expected.with {
        addNode(group1)
        addNode(hosted1)
        addNode(group2)
        addNode(hosted2)
        putEdge(group1, hosted1)
        putEdge(group2, hosted2)
      } 
    
    when: 'We can graph each and the combine the results'
      Graph<Repository> combined = repositoryMemberGraph.
          combineGraphs(repositoryMemberGraph.render(group1), repositoryMemberGraph.render(group2))
    
    then: 'Individual graphs are combined'
      combined.nodes() == expected.nodes()
      combined.edges() == expected.edges()

      group1.type >> groupType
      hosted1.type >> hostedType
      group1.getConfiguration() >> configuration1
      configuration1.attributes(_) >> attributesMap1
      repositoryManager.get('group1') >> group1
      repositoryManager.get('hosted1') >> hosted1
      group2.type >> groupType
      hosted2.type >> hostedType
      group2.getConfiguration() >> configuration2
      configuration2.attributes(_) >> attributesMap2
      repositoryManager.get('group2') >> group2
      repositoryManager.get('hosted2') >> hosted2
  }
  
  def 'Can convert to .dot graph'() {
    given: 'A group repository with hosted and proxy members'
      Repository group = Mock()
      Repository hosted = Mock()
      Repository proxy = Mock()
      Configuration configuration = Mock()
      NestedAttributesMap attributesMap = new NestedAttributesMap('group', [memberNames: ['hosted', 'proxy']])

      MutableGraph<Repository> expected = GraphBuilder.directed().build()
      expected.with {
        addNode(group)
        addNode(hosted)
        addNode(proxy)
        putEdge(group, hosted)
        putEdge(group, proxy)
      }

    when: 'rendering the graph'
      String dotGraph = repositoryMemberGraph.toDotGraph(repositoryMemberGraph.render(group))

    then: 'all three repositories appear in the graph, with edges from the group to each member'
      dotGraph == '''digraph { "group" [color=red]; "hosted" [color=blue]; "proxy" [color=green]; "group" -> "hosted"; "group" -> "proxy" }'''
      
      group.type >> groupType
      group.getConfiguration() >> configuration
      configuration.attributes(_) >> attributesMap
      repositoryManager.get('hosted') >> hosted
      repositoryManager.get('proxy') >> proxy
      hosted.type >> hostedType
      proxy.type >> proxyType 
      hosted.name >> 'hosted'
      proxy.name >> 'proxy'
      group.name >> 'group'
  }
  
  def 'Can render all Repository graphs together'() {
    given: 'More than one repo'
    Repository hosted1 = Mock()
    Repository hosted2 = Mock()
    
    MutableGraph<Repository> expected = GraphBuilder.directed().build()
    expected.with{
      addNode(hosted1)
      addNode(hosted2)
    }
    
    when: 'rendering the graph of all repos'
      Graph<Repository> graph = repositoryMemberGraph.renderAllRepos()

    then: 'all Repositories are in the resulting graph'
      graph.nodes() == expected.nodes()
      graph.edges() == expected.edges()

      repositoryManager.browse() >> [hosted1, hosted2]
      hosted1.type >> hostedType
      hosted2.type >> hostedType
      hosted1.name >> 'one'
      hosted2.name >> 'two'
  }
}


