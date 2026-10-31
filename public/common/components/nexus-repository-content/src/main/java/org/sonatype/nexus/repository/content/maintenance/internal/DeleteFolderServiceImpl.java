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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.app.softdelete.SoftDeleteFormatGate;
import org.sonatype.nexus.common.app.softdelete.SoftDeletedEntityInfo;
import org.sonatype.nexus.common.app.softdelete.TrashIndexRecorder;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.maintenance.SingleAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.SecurityHelper;
import org.apache.shiro.subject.Subject;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static java.time.OffsetDateTime.now;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import org.springframework.stereotype.Component;

/**
 * @since 3.26
 */
@Component
public class DeleteFolderServiceImpl
    implements DeleteFolderService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final BrowseNodeQueryService browseNodeQueryService;

  private final BrowseNodeConfiguration configuration;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final SecurityHelper securityHelper;

  private final DatabaseCheck databaseCheck;

  private final SoftDeleteFormatGate softDeleteFormatGate;

  @Autowired(required = false)
  private TrashIndexRecorder trashIndexRecorder;

  @Autowired
  public DeleteFolderServiceImpl(
      final BrowseNodeQueryService browseNodeQueryService,
      final BrowseNodeConfiguration configuration,
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager,
      final SecurityHelper securityHelper,
      final DatabaseCheck databaseCheck,
      final SoftDeleteFormatGate softDeleteFormatGate)
  {
    this.browseNodeQueryService = requireNonNull(browseNodeQueryService);
    this.configuration = requireNonNull(configuration);
    this.contentPermissionChecker = requireNonNull(contentPermissionChecker);
    this.variableResolverAdapterManager = requireNonNull(variableResolverAdapterManager);
    this.securityHelper = requireNonNull(securityHelper);
    this.databaseCheck = requireNonNull(databaseCheck);
    this.softDeleteFormatGate = requireNonNull(softDeleteFormatGate);
  }

  @Override
  public void deleteFolder(final Repository repository, final String treePath, final OffsetDateTime timestamp) {
    ContentFacet contentFacet = repository.facet(ContentFacet.class);
    ContentMaintenanceFacet contentMaintenance = repository.facet(ContentMaintenanceFacet.class);
    int maxNodes = configuration.getMaxNodes();

    boolean canDeleteComponent = canDeleteComponent(repository);

    // The plain deleteFolders() path removes assets/components but relies on the asynchronous browse-node
    // trimming that reacts to hard-delete events. Soft-delete records deletions as UPDATEs (deleted_on),
    // which never fire those events, so browse nodes would linger and 404 on click. Use the browse-node-aware
    // path (which removes nodes synchronously) whenever soft-delete is enabled, in addition to PostgreSQL.
    if (this.databaseCheck.isPostgresql() || softDeleteFormatGate.isEnabled()) {
      deleteFoldersAndBrowseNode(repository, treePath, timestamp, contentFacet, contentMaintenance, maxNodes,
          canDeleteComponent);
    }
    else {
      deleteFolders(repository, treePath, timestamp, contentFacet, contentMaintenance, maxNodes, canDeleteComponent);
    }
  }

  public void deleteFolders(
      final Repository repository,
      final String treePath,
      final OffsetDateTime timestamp,
      ContentFacet contentFacet,
      ContentMaintenanceFacet contentMaintenance,
      int maxNodes,
      boolean canDeleteComponent)
  {
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
        else {
          // For nodes with both asset and component (e.g., Go repos using AssetPathBrowseNodeGenerator)
          // Delete asset first, then check if component should be deleted.
          // Note: This order differs from the separated node logic below because we need to
          // verify no assets remain before deleting the component to prevent orphans.
          if (node.getAssetId() != null && node.getComponentId() != null) {
            boolean assetDeleted = checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);
            // After deleting the asset, check if the component has any remaining assets
            // If not, delete the component to prevent orphaned components
            if (assetDeleted && canDeleteComponent) {
              checkDeleteComponentWithAssets(repository, timestamp, contentFacet, contentMaintenance, node);
            }
          }
          else {
            // For nodes with only component or only asset, use the original logic
            checkDeleteComponent(repository, timestamp, contentFacet, contentMaintenance, canDeleteComponent, node);
            checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);
          }
        }
      }
    }
  }

  public void deleteFoldersAndBrowseNode(
      final Repository repository,
      final String treePath,
      final OffsetDateTime timestamp,
      ContentFacet contentFacet,
      ContentMaintenanceFacet contentMaintenance,
      int maxNodes,
      boolean canDeleteComponent)
  {
    if (log.isDebugEnabled()) {
      log.debug("Deleting folder: '{}' and children", treePath);
    }

    Instant start = Instant.now();
    BrowseFacet browseFacet = repository.facet(BrowseFacet.class);

    ConcurrentLinkedDeque<String> pathStack = new ConcurrentLinkedDeque<>();
    Set<String> processedPaths = new HashSet<>();
    pathStack.push(treePath);

    while (checkCancellation() && !pathStack.isEmpty()) {
      processNodesAndLeaves(repository, timestamp, contentFacet, contentMaintenance, maxNodes, canDeleteComponent,
          pathStack,
          browseFacet,
          processedPaths);
    }

    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    if (log.isDebugEnabled()) {
      log.debug("Deleting folder: '{}' and children took {} milliseconds", treePath, duration.toMillis());
    }
  }

  private void processNodesAndLeaves(
      final Repository repository,
      final OffsetDateTime timestamp,
      final ContentFacet contentFacet,
      final ContentMaintenanceFacet contentMaintenance,
      final int maxNodes,
      final boolean canDeleteComponent,
      final ConcurrentLinkedDeque<String> pathStack,
      final BrowseFacet browseFacet,
      final Set<String> processedPaths)
  {
    String nodePath = pathStack.poll();
    if (log.isTraceEnabled()) {
      log.trace("Processing node: '{}'", nodePath);
    }

    List<String> pathSegments = Splitter.on('/').omitEmptyStrings().splitToList(nodePath);

    Iterable<BrowseNode> nodes = browseNodeQueryService.getByPath(repository, pathSegments, maxNodes);

    // If the nodes don't have next() means this folder doesn't have children
    if (!nodes.iterator().hasNext()) {
      // I'm going to transform the treePath to a request path
      String requestPath = transformTreePathToRequestPath(nodePath);

      // Delete folder browse node only if it has no children (all were successfully deleted)
      browseFacet.getByRequestPath(requestPath)
          .ifPresent(node -> {
            try {
              browseFacet.deleteByNodeId(((BrowseNodeData) node).getNodeId());
              if (log.isDebugEnabled()) {
                log.debug("Folder browse node deleted: '{}'", nodePath);
              }
            }
            catch (Exception e) {
              log.warn("Failed to delete folder browse node for path: '{}' - skipping.", nodePath, e);
            }
          });
    }
    else {
      // This folder has children
      if (processedPaths.contains(nodePath)) {
        // We've already attempted to process this folder's children
        // If we're here again, it means children couldn't be deleted
        if (log.isDebugEnabled()) {
          log.debug("Skipping folder '{}' - children could not be deleted", nodePath);
        }
        return;
      }

      // Push the parent back to the stack to revisit after processing children
      pathStack.push(nodePath);
      // First attempt - mark as processed now to detect second attempt
      processedPaths.add(nodePath);

      for (BrowseNode node : nodes) {
        checkCancellation();

        if (!node.isLeaf()) {
          pathStack.push(nodePath + "/" + node.getName());
        }
        else {
          processLeafDeletion(browseFacet, timestamp, contentFacet, contentMaintenance,
              canDeleteComponent, repository, node);
        }
      }
    }
  }

  boolean checkDeleteComponent(
      final Repository repository,
      final OffsetDateTime timestamp,
      final ContentFacet contentFacet,
      final ContentMaintenanceFacet contentMaintenance,
      final boolean canDeleteComponent,
      final BrowseNode node)
  {
    if (canDeleteComponent && node.getAssetId() == null && node.getComponentId() != null) {
      return contentFacet.components()
          .find(node.getComponentId())
          .map(component -> deleteComponent(repository, component, timestamp, contentMaintenance))
          .orElse(true); // If component not found, consider it already deleted
    }
    return true; // No component associated with this node or cannot delete
  }

  /**
   * Check and delete a component if it has no remaining assets.
   * This is used for formats like Go, NPM, Raw, Composer, Cargo, Terraform, and Yum
   * where browse nodes contain both asset and component references via AssetPathBrowseNodeGenerator.
   *
   * @param timestamp the timestamp to check against
   * @param contentFacet the content facet
   * @param contentMaintenance the maintenance facet
   * @param node the browse node
   * @return true if safe to proceed with browse node deletion (component deleted, not found, or still has assets),
   *         false only if component deletion was attempted and failed
   */
  boolean checkDeleteComponentWithAssets(
      final Repository repository,
      final OffsetDateTime timestamp,
      final ContentFacet contentFacet,
      final ContentMaintenanceFacet contentMaintenance,
      final BrowseNode node)
  {
    if (node.getComponentId() != null) {
      return contentFacet.components()
          .find(node.getComponentId())
          .map(component -> {
            // Check if component has any remaining assets
            boolean hasAssets = component.assets().stream().findAny().isPresent();
            if (!hasAssets) {
              // No assets left, delete the component
              return deleteComponent(repository, component, timestamp, contentMaintenance);
            }
            else {
              // Component still has assets, don't delete it yet
              if (log.isTraceEnabled()) {
                log.trace("Component {} still has assets, not deleting yet", node.getComponentId());
              }
              return true; // Return true to not block browse node deletion
            }
          })
          .orElse(true); // If component not found, consider it already deleted
    }
    return true; // No component associated with this node
  }

  boolean checkDeleteAsset(
      final Repository repository,
      final OffsetDateTime timestamp,
      final ContentFacet contentFacet,
      final ContentMaintenanceFacet contentMaintenance,
      final BrowseNode node)
  {
    if (node.getAssetId() != null) {
      return contentFacet.assets()
          .find(node.getAssetId())
          .map(asset -> deleteAsset(repository, asset, timestamp, contentMaintenance))
          .orElse(true); // If asset not found, consider it already deleted
    }
    return true; // No asset associated with this node
  }

  private boolean deleteComponent(
      final Repository repository,
      final FluentComponent component,
      final OffsetDateTime timestamp,
      final ContentMaintenanceFacet contentMaintenance)
  {
    OffsetDateTime lastUpdated = component.lastUpdated();
    if (timestamp.isAfter(lastUpdated)) {
      try {
        if (shouldSoftDelete(repository)) {
          return softDeleteComponent(repository, component, contentMaintenance, timestamp);
        }
        contentMaintenance.deleteComponent(component);
        return true;
      }
      catch (Exception e) {
        log.warn("Failed to delete a component - skipping.", e);
        return false;
      }
    }
    return false;
  }

  private boolean deleteAsset(
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
        if (shouldSoftDelete(repository)) {
          return softDeleteAsset(repository, asset, contentMaintenance, timestamp);
        }
        contentMaintenance.deleteAsset(asset);
        return true;
      }
      catch (WebApplicationException e) {
        // A 409 Conflict means the format's referential-integrity guard rejected the deletion
        // (e.g. a Docker/OCI blob still referenced by a live manifest). This is expected
        // behaviour, not an application error — log at debug and skip.
        if (e.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
          log.debug("Skipping deletion of referenced asset '{}' (409 Conflict): {}",
              asset.path(), e.getMessage());
        }
        else {
          log.error("Failed to delete asset '{}' (HTTP {}): skipping.", asset.path(),
              e.getResponse().getStatus(), e);
        }
        return false;
      }
      catch (Exception e) {
        log.error("Failed to delete an asset - skipping.", e);
        return false;
      }
    }
    return false;
  }

  /**
   * Resolves the current subject's principal for the {@code deleted_by} audit field, falling back to {@code "system"}
   * for an anonymous/unauthenticated subject (principal can be {@code null}). Mirrors
   * {@code ContentDeletionService.softDeleteContext()} so the folder-delete path is consistent and NPE-safe.
   */
  private String currentPrincipalOrSystem() {
    Subject subject = securityHelper.subject();
    return subject != null && subject.getPrincipal() != null ? subject.getPrincipal().toString() : "system";
  }

  private boolean shouldSoftDelete(final Repository repository) {
    return softDeleteFormatGate.isEnabled()
        && HostedType.NAME.equals(repository.getType().getValue())
        && softDeleteFormatGate.isEnabledForFormat(repository.getFormat().getValue());
  }

  private boolean softDeleteComponent(
      final Repository repository,
      final FluentComponent component,
      final ContentMaintenanceFacet contentMaintenance,
      final OffsetDateTime folderDeleteStarted)
  {
    ContentFacetSupport contentFacet = (ContentFacetSupport) repository.facet(ContentFacet.class);
    String deletedBy = currentPrincipalOrSystem();

    ComponentStore<?> componentStore = contentFacet.stores().componentStore;
    AssetStore<?> assetStore = contentFacet.stores().assetStore;

    componentStore.softDeleteComponent(component, deletedBy);
    assetStore.softDeleteComponentAssets(component, deletedBy);
    assetStore.reSoftDeleteComponentAssets(component, deletedBy, folderDeleteStarted);
    contentMaintenance.afterComponentSoftDeleted(component);
    recordComponentInTrashIndex(repository, component, contentFacet, deletedBy);

    log.info("Soft-deleted component '{}' in repository '{}'", component.name(), repository.getName());
    return true;
  }

  private boolean softDeleteAsset(
      final Repository repository,
      final FluentAsset asset,
      final ContentMaintenanceFacet contentMaintenance,
      final OffsetDateTime folderDeleteStarted)
  {
    ContentFacetSupport contentFacet = (ContentFacetSupport) repository.facet(ContentFacet.class);
    String deletedBy = currentPrincipalOrSystem();

    // Referential-integrity guard (NEXUS-54127): refuse soft-deleting an asset still referenced by a
    // live/restorable sibling (e.g. a Docker/OCI blob referenced by a manifest), mirroring the hard-delete
    // guard in the format's ContentMaintenanceFacet.deleteAsset so folder-delete cannot strand referenced content.
    contentMaintenance.validateAssetDeletable(asset);

    AssetStore<?> assetStore = contentFacet.stores().assetStore;
    assetStore.softDeleteAsset(asset, deletedBy);
    contentMaintenance.afterAssetSoftDeleted(asset);
    recordAssetInTrashIndex(repository, asset, contentFacet, deletedBy);

    asset.component()
        .ifPresent(component -> handleAssetSoftDeleteCascade(repository, component, contentFacet, contentMaintenance,
            deletedBy, folderDeleteStarted));

    log.info("Soft-deleted asset '{}' in repository '{}'", asset.path(), repository.getName());
    return true;
  }

  private void handleAssetSoftDeleteCascade(
      final Repository repository,
      final org.sonatype.nexus.repository.content.Component component,
      final ContentFacetSupport contentFacet,
      final ContentMaintenanceFacet contentMaintenance,
      final String deletedBy,
      final OffsetDateTime folderDeleteStarted)
  {
    try {
      boolean shouldCascade = false;
      if (contentMaintenance instanceof SingleAssetMaintenanceFacet) {
        shouldCascade = true;
      }
      else if (contentMaintenance instanceof LastAssetMaintenanceFacet) {
        Collection<Asset> remaining = contentFacet.stores().assetStore.browseComponentAssets(component);
        shouldCascade = remaining.isEmpty();
      }

      if (shouldCascade) {
        log.info("Auto-soft-deleting component (no remaining active assets) in repository '{}'",
            repository.getName());
        contentFacet.stores().componentStore.softDeleteComponent(component, deletedBy);
        contentFacet.stores().assetStore.reSoftDeleteComponentAssets(component, deletedBy, folderDeleteStarted);
        contentMaintenance.afterComponentSoftDeleted(component);
        recordComponentInTrashIndex(repository, component, contentFacet, deletedBy);
      }
    }
    catch (Exception e) {
      log.warn("Could not evaluate cascade for component in repository '{}'", repository.getName(), e);
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

  private String transformTreePathToRequestPath(final String treePath) {
    String requestPath = treePath;
    if (!requestPath.startsWith("/")) {
      requestPath = "/" + requestPath;
    }
    if (!requestPath.endsWith("/")) {
      requestPath = requestPath + "/";
    }
    return requestPath;
  }

  @VisibleForTesting
  boolean canDeleteComponent(final Repository repository) {
    return securityHelper.isPermitted(new RepositoryViewPermission(repository, DELETE))[0];
  }

  private void recordAssetInTrashIndex(
      final Repository repository,
      final FluentAsset asset,
      final ContentFacetSupport contentFacet,
      final String deletedBy)
  {
    if (trashIndexRecorder == null) {
      return;
    }
    try {
      String repoName = repository.getName();
      String format = repository.getFormat().getValue();
      String externalId = InternalIds.toExternalId(InternalIds.internalAssetId(asset)).getValue();
      String encodedId = encodeIdentifier(repoName, externalId);

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("repositoryName", repoName);
      metadata.put("format", format);
      metadata.put("path", asset.path());
      metadata.put("repositoryId", contentFacet.contentRepositoryId());

      asset.component().ifPresent(component -> {
        String compExternalId = InternalIds.toExternalId(
            InternalIds.internalComponentId(component)).getValue();
        metadata.put("parentComponentId", encodeIdentifier(repoName, compExternalId));
      });

      SoftDeletedEntityInfo info = new SoftDeletedEntityInfo(
          "asset", encodedId, asset.path(), now(), deletedBy, metadata);
      trashIndexRecorder.record(info);
    }
    catch (Exception e) {
      log.warn("Failed to record asset in trash index", e);
    }
  }

  private void recordComponentInTrashIndex(
      final Repository repository,
      final org.sonatype.nexus.repository.content.Component component,
      final ContentFacetSupport contentFacet,
      final String deletedBy)
  {
    if (trashIndexRecorder == null) {
      return;
    }
    try {
      String repoName = repository.getName();
      String format = repository.getFormat().getValue();
      String externalId = InternalIds.toExternalId(
          InternalIds.internalComponentId(component)).getValue();
      String encodedId = encodeIdentifier(repoName, externalId);

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("repositoryName", repoName);
      metadata.put("format", format);
      metadata.put("repositoryId", contentFacet.contentRepositoryId());
      metadata.put("namespace", component.namespace());
      metadata.put("name", component.name());
      metadata.put("version", component.version());
      metadata.put("kind", component.kind());

      SoftDeletedEntityInfo info = new SoftDeletedEntityInfo(
          "component", encodedId, componentDisplayName(component), now(), deletedBy, metadata);
      trashIndexRecorder.record(info);
      // Subsume ALL of this component's child-asset Trash entries under the component -- matching the single
      // component soft-delete path -- so an asset soft-deleted before this folder operation is not left as a
      // separate top-level row while also appearing as a child of the expanded component.
      trashIndexRecorder.removeByParentComponent(encodedId);
    }
    catch (Exception e) {
      log.warn("Failed to record component in trash index", e);
    }
  }

  /**
   * Builds a {@code namespace:name:version} display name (omitting empty parts) so that components which
   * share a name but differ by version are distinguishable in Trash, matching the per-component
   * soft-delete path ({@code ComponentSoftDeletedEntityService}).
   */
  @VisibleForTesting
  static String componentDisplayName(final org.sonatype.nexus.repository.content.Component component) {
    StringBuilder sb = new StringBuilder();
    String namespace = component.namespace();
    if (namespace != null && !namespace.isEmpty()) {
      sb.append(namespace).append(':');
    }
    sb.append(component.name());
    String version = component.version();
    if (version != null && !version.isEmpty()) {
      sb.append(':').append(version);
    }
    return sb.toString();
  }

  private static String encodeIdentifier(final String repositoryName, final String externalId) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString((repositoryName + ":" + externalId).getBytes(StandardCharsets.UTF_8));
  }

  @VisibleForTesting
  void processLeafDeletion(
      BrowseFacet browseFacet,
      OffsetDateTime timestamp,
      ContentFacet contentFacet,
      ContentMaintenanceFacet contentMaintenance,
      boolean canDeleteComponent,
      Repository repository,
      BrowseNode node)
  {
    BrowseNodeData nodeData = (BrowseNodeData) node;
    if (log.isTraceEnabled()) {
      log.trace("Processing leaf: '{}'", nodeData.getPath());
    }

    boolean componentDeleted = true;
    boolean assetDeleted = true;

    // For nodes with both asset and component (e.g., Go repos using AssetPathBrowseNodeGenerator)
    // Delete asset first, then check if component should be deleted.
    // Note: This order differs from the separated node logic below because we need to
    // verify no assets remain before deleting the component to prevent orphans.
    if (node.getAssetId() != null && node.getComponentId() != null) {
      assetDeleted = checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);

      // After deleting the asset, check if the component has any remaining assets
      // If not, delete the component to prevent orphaned components
      if (assetDeleted && canDeleteComponent) {
        componentDeleted =
            checkDeleteComponentWithAssets(repository, timestamp, contentFacet, contentMaintenance, node);
      }
    }
    else {
      // For nodes with only component or only asset, use the original logic
      componentDeleted =
          checkDeleteComponent(repository, timestamp, contentFacet, contentMaintenance, canDeleteComponent, node);
      assetDeleted = checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);
    }

    // Only delete browse node if both component and asset were successfully deleted (if they exist)
    if (componentDeleted && assetDeleted) {
      try {
        browseFacet.deleteByNodeId(nodeData.getNodeId());
        if (log.isDebugEnabled()) {
          log.debug("Browse node deleted for path: '{}'", nodeData.getPath());
        }
      }
      catch (Exception e) {
        log.warn("Failed to delete browse node for path: '{}' - skipping.", nodeData.getPath(), e);
      }
    }
    else {
      if (log.isDebugEnabled()) {
        log.debug(
            "Browse node NOT deleted for path: '{}' because component or asset deletion failed or was not permitted",
            nodeData.getPath());
      }
    }
  }
}
