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
package org.sonatype.nexus.repository.content.maintenance.internal;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.OffsetDateTime.now;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;
import static org.sonatype.nexus.security.BreadActions.DELETE;

/**
 * @since 3.26
 */
@Named
@Singleton
public class DeleteFolderServiceImpl
    extends ComponentSupport
    implements DeleteFolderService
{
  private final BrowseNodeQueryService browseNodeQueryService;

  private final BrowseNodeConfiguration configuration;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final SecurityHelper securityHelper;

  @Inject
  public DeleteFolderServiceImpl(
      final BrowseNodeQueryService browseNodeQueryService,
      final BrowseNodeConfiguration configuration,
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager,
      final SecurityHelper securityHelper)
  {
    this.browseNodeQueryService = checkNotNull(browseNodeQueryService);
    this.configuration = checkNotNull(configuration);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.securityHelper = checkNotNull(securityHelper);
  }

  @Override
  public void deleteFolder(final Repository repository, final String treePath, final OffsetDateTime timestamp) {

    ContentFacet contentFacet = repository.facet(ContentFacet.class);
    ContentMaintenanceFacet contentMaintenance = repository.facet(ContentMaintenanceFacet.class);
    int maxNodes = configuration.getMaxNodes();

    boolean canDeleteComponent = securityHelper.isPermitted(new RepositoryViewPermission(repository, DELETE))[0];

    Queue<String> pathQueue = new PriorityQueue<>();
    pathQueue.add(treePath);

    while (checkCancellation() && !pathQueue.isEmpty()) {
      String nodePath = pathQueue.poll();

      List<String> pathSegments = Splitter.on('/').omitEmptyStrings().splitToList(nodePath);

      Iterable<BrowseNode> nodes = browseNodeQueryService.getByPath(repository, pathSegments, maxNodes);
      Iterator<BrowseNode> nodeIterator = nodes.iterator();

      while (checkCancellation() && nodeIterator.hasNext()) {
        BrowseNode node = nodeIterator.next();

        if (!node.isLeaf()) {
          pathQueue.offer(nodePath + "/" + node.getName());
        }
        else if (canDeleteComponent && node.getAssetId() == null && node.getComponentId() != null) {
          contentFacet.components().find(node.getComponentId()).ifPresent(
              component -> deleteComponent(component, timestamp, contentMaintenance));
        }

        if (node.getAssetId() != null) {
          contentFacet.assets().find(node.getAssetId()).ifPresent(
              asset -> deleteAsset(repository, asset, timestamp, contentMaintenance));
        }
      }
    }
  }

  private void deleteComponent(
      final FluentComponent component,
      final OffsetDateTime timestamp,
      final ContentMaintenanceFacet contentMaintenance)
  {
    OffsetDateTime lastUpdated = component.lastUpdated();
    if (timestamp.isAfter(lastUpdated)) {
      contentMaintenance.deleteComponent(component);
    }
  }

  private void deleteAsset(
      final Repository repository,
      final FluentAsset asset,
      final OffsetDateTime timestamp,
      final ContentMaintenanceFacet contentMaintenance)
  {
    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);

    if (timestamp.isAfter(asset.blob().map(AssetBlob::blobCreated).orElse(now()))
        && canDeleteAsset(repositoryName, format, variableResolverAdapter, asset)) {
      try {
        contentMaintenance.deleteAsset(asset);
      }
      catch (Exception e) {
        log.error("Failed to delete an asset - skipping.", e);
      }
    }
  }

  private boolean canDeleteAsset(
      final String repositoryName,
      final String format,
      final VariableResolverAdapter variableResolverAdapter,
      final Asset asset)
  {
    VariableSource source = variableResolverAdapter.fromPath(asset.path(), format);
    return contentPermissionChecker.isPermitted(repositoryName, format, DELETE, source);
  }
}
