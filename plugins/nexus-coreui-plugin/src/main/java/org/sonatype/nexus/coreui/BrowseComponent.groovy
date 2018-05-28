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

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.app.VersionComparator
import org.sonatype.nexus.common.encoding.EncodingUtil
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.rapture.StateContributor
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.storage.BrowseNodeStore

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod

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
    implements StateContributor
{
  static final FOLDER = "folder"
  static final COMPONENT = "component"
  static final ASSET = "asset"

  @Inject
  BrowseNodeConfiguration configuration

  @Inject
  BrowseNodeStore browseNodeStore

  @Inject
  RepositoryManager repositoryManager

  final VersionComparator versionComparator = new VersionComparator()

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<BrowseNodeXO> read(TreeStoreLoadParameters treeStoreLoadParameters) {
    String repositoryName = treeStoreLoadParameters.repositoryName
    String path = treeStoreLoadParameters.node
    String filter = treeStoreLoadParameters.filter

    Repository repository = repositoryManager.get(repositoryName)

    List<String> pathSegments
    if (isRoot(path)) {
      pathSegments = Collections.emptyList()
    }
    else {
      pathSegments = path.split('/').collect EncodingUtil.&urlDecode
    }

    return browseNodeStore.getByPath(repository, pathSegments, configuration.maxNodes, filter).collect { browseNode ->
      def encodedPath = EncodingUtil.urlEncode(browseNode.name)
      new BrowseNodeXO(
          id: isRoot(path) ? encodedPath : (path + '/' + encodedPath),
          type: browseNode.assetId != null ? ASSET : browseNode.componentId != null ? COMPONENT : FOLDER,
          text: browseNode.name,
          leaf: browseNode.leaf,
          componentId: browseNode.componentId != null ? browseNode.componentId.value : null,
          assetId: browseNode.assetId != null ? browseNode.assetId.value : null
      )
    }
  }

  @Override
  @Nullable
  Map<String, Object> getState() {
    return ['browseComponentAssetTree': configuration.enabled, 'browseTreeMaxNodes': configuration.maxNodes]
  }

  def isRoot(String path) {
    return '/'.equals(path)
  }
}
