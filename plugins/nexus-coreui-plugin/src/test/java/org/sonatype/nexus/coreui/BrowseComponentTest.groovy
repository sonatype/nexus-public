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
package org.sonatype.nexus.coreui

import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.storage.BrowseNode
import org.sonatype.nexus.repository.storage.BrowseNodeStore

import spock.lang.Specification
import spock.lang.Subject

import static org.sonatype.nexus.coreui.BrowseComponent.ASSET
import static org.sonatype.nexus.coreui.BrowseComponent.COMPONENT
import static org.sonatype.nexus.coreui.BrowseComponent.FOLDER

/**
 * @since 3.1
 */
class BrowseComponentTest
    extends Specification
{
  private static final String REPOSITORY_NAME = 'repositoryName'
  private static final String ROOT = '/'

  BrowseNodeConfiguration configuration = new BrowseNodeConfiguration()

  BrowseNodeStore browseNodeStore = Mock()

  RepositoryManager repositoryManager = Mock()

  EntityId assetId = Mock()

  EntityId componentId = Mock()

  Repository repository = Mock()

  @Subject
  BrowseComponent browseComponent = new BrowseComponent(configuration: configuration, browseNodeStore: browseNodeStore, repositoryManager: repositoryManager)

  def setup() {
    componentId.value >> "componentId"
    assetId.value >> "assetId"
  }

  def "Root node list query"() {
    given: 'These test objects'
      def browseNodes = [new BrowseNode(name: 'com'),
                         new BrowseNode(name: 'org', componentId: componentId),
                         new BrowseNode(name: 'net', assetId: assetId, leaf: true)]

    when: 'Requesting the list of root nodes'
      TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters(
          repositoryName: REPOSITORY_NAME,
          node: ROOT,
          filter: 'foo')

      1 * repositoryManager.get(REPOSITORY_NAME) >> repository
      1 * browseNodeStore.getByPath(repository, [], configuration.maxHtmlNodes,'foo') >> browseNodes
      List<BrowseNodeXO> xos = browseComponent.read(treeStoreLoadParameters)

    then: 'the 3 root entries are returned'
      xos*.text == ['com', 'org', 'net']
      xos*.id   == ['com', 'org', 'net']
      xos*.type == [FOLDER, COMPONENT, ASSET]
      xos*.leaf == [false, false, true]
  }

  def "non-root list query"() {
    given: 'These test objects'
      def browseNodes = [new BrowseNode(name: 'com'),
                         new BrowseNode(name: 'org', componentId: componentId),
                         new BrowseNode(name: 'net', assetId: assetId, leaf: true)]

    when: 'Requesting the list of root nodes'
      TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters(
          repositoryName: REPOSITORY_NAME,
          node: 'com/boogie/down',
          filter: null)

      1 * repositoryManager.get(REPOSITORY_NAME) >> repository
      1 * browseNodeStore.getByPath(repository, ['com','boogie','down'], configuration.maxHtmlNodes, null) >> browseNodes
      List<BrowseNodeXO> xos = browseComponent.read(treeStoreLoadParameters)

    then: 'the 3 entries are returned'
      xos*.text == ['com', 'org', 'net']
      xos*.id   == ['com/boogie/down/com', 'com/boogie/down/org', 'com/boogie/down/net']
      xos*.type == [FOLDER, COMPONENT, ASSET]
      xos*.leaf == [false, false, true]
  }

  def 'validate encoded segments'() {
    given: 'These test objects'
    def browseNodes = [new BrowseNode(name: 'com'),
                       new BrowseNode(name: 'org', componentId: componentId),
                       new BrowseNode(name: 'n/e/t', assetId: assetId, leaf: true)]

    when: 'Requesting the list of root nodes'
    TreeStoreLoadParameters treeStoreLoadParameters = new TreeStoreLoadParameters(
        repositoryName: REPOSITORY_NAME,
        node: 'com/boo%2Fgie/down')

      1 * repositoryManager.get(REPOSITORY_NAME) >> repository
      1 * browseNodeStore.getByPath(repository, ['com','boo/gie','down'], configuration.maxHtmlNodes, null) >> browseNodes
    List<BrowseNodeXO> xos = browseComponent.read(treeStoreLoadParameters)

    then: 'the 3 entries are returned'
      xos*.text == ['com', 'org', 'n/e/t']
      xos*.id   == ['com/boo%2Fgie/down/com', 'com/boo%2Fgie/down/org', 'com/boo%2Fgie/down/n%2Fe%2Ft']
      xos*.type == [FOLDER, COMPONENT, ASSET]
      xos*.leaf == [false, false, true]
  }
}
