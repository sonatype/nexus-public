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

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.common.encoding.EncodingUtil
import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.browse.node.BrowseNode
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration
import org.sonatype.nexus.repository.browse.node.BrowseNodeStore
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.ossindex.VulnerabilityService

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope

/**
 * Browse {@link DirectComponent}.
 *
 * @since 3.6
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Browse')
class BrowseComponent
    extends DirectComponentSupport
{
  @PackageScope static final FOLDER = "folder"
  @PackageScope static final COMPONENT = "component"
  @PackageScope static final ASSET = "asset"

  @Inject
  BrowseNodeConfiguration configuration

  @Inject
  BrowseNodeStore<EntityId> browseNodeStore

  @Inject
  RepositoryManager repositoryManager

  @Inject
  Provider<VulnerabilityService> vulnerabilityServiceProvider

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<BrowseNodeXO> read(TreeStoreLoadParameters treeStoreLoadParameters) {
    String repositoryName = treeStoreLoadParameters.repositoryName
    String path = treeStoreLoadParameters.node

    Repository repository = repositoryManager.get(repositoryName)

    List<String> pathSegments
    if (isRoot(path)) {
      pathSegments = Collections.emptyList()
    }
    else {
      pathSegments = path.split('/').collect { String part -> EncodingUtil.urlDecode(part) }
    }

    return browseNodeStore.getByPath(repository.name, pathSegments, configuration.maxNodes)
        .collect { BrowseNode browseNode ->
          def encodedPath = EncodingUtil.urlEncode(browseNode.name)
          def type = browseNode.assetId != null ? ASSET : browseNode.componentId != null ? COMPONENT : FOLDER
          new BrowseNodeXO(
              id: isRoot(path) ? encodedPath : (path + '/' + encodedPath),
              type: type,
              text: browseNode.name,
              leaf: browseNode.leaf,
              componentId: browseNodeStore.getValue(browseNode.componentId as EntityId),
              assetId: browseNodeStore.getValue(browseNode.assetId as EntityId),
              packageUrl: browseNode.packageUrl
          )
        }
  }

  def isRoot(String path) {
    return '/'.equals(path)
  }

}
