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
package org.sonatype.nexus.repository.content.store;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.browse.internal.BrowseNodeDAO;
import org.sonatype.nexus.repository.content.browse.internal.DatastoreBrowseNode;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeComparator;
import org.sonatype.nexus.repository.storage.BrowseNodeCrudStore;
import org.sonatype.nexus.repository.storage.BrowseNodeFacet;
import org.sonatype.nexus.repository.storage.BrowseNodeFilter;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import org.apache.ibatis.exceptions.PersistenceException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * A Datastore backed implementation of component/asset tree browsing.
 *
 * @since 3.22
 */
@Named
@Singleton
public class DatastoreBrowseNodeStoreImpl<T extends BrowseNodeDAO>
    extends ContentStoreSupport<T>
    implements BrowseNodeStore<Integer>, BrowseNodeCrudStore<Asset, Component>
{
  private final Map<String, BrowseNodeFilter> browseNodeFilters;

  private final Map<String, BrowseNodeComparator> browseNodeComparators;

  private final RepositoryManager repositoryManager;

  private final Map<String, ContentRepositoryStore<? extends ContentRepositoryDAO>> contentRepositoryStores;

  private final BrowseNodeComparator defaultBrowseNodeComparator;

  private final int deletePageSize;

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  @Inject
  public DatastoreBrowseNodeStoreImpl(
      final DataSessionSupplier sessionSupplier,
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final BrowseNodeConfiguration configuration,
      final RepositoryManager repositoryManager,
      final Map<String, ContentRepositoryStore<? extends ContentRepositoryDAO>> contentRepositoryStores,
      final Map<String, BrowseNodeFilter> browseNodeFilters,
      final Map<String, BrowseNodeComparator> browseNodeComparators)
  {
    super(sessionSupplier, "content");
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
    this.browseNodeFilters = checkNotNull(browseNodeFilters);
    this.browseNodeComparators = checkNotNull(browseNodeComparators);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.contentRepositoryStores = checkNotNull(contentRepositoryStores);
    this.deletePageSize = checkNotNull(configuration).getDeletePageSize();
    this.defaultBrowseNodeComparator = checkNotNull(browseNodeComparators.get(DefaultBrowseNodeComparator.NAME));
  }

  @Override
  public String getValue(final Integer id) {
    return id == null ? null : String.valueOf(id);
  }

  @Override
  public Integer fromValue(final String value) {
    return value == null ? null : Integer.valueOf(value, 10);
  }

  @Override
  @Transactional
  public void createAssetNode(
      final String repositoryName,
      final String format,
      final List<BrowsePaths> paths,
      final Asset asset)
  {
    checkNotNull(repositoryName);
    checkNotNull(format);
    checkNotNull(paths);
    checkArgument(!paths.isEmpty());
    checkNotNull(asset);

    createNodes(repositoryName, paths).ifPresent(nodeId -> dao().linkAsset(nodeId, asset));
  }

  @Override
  @Transactional
  public void createComponentNode(
      final String repositoryName,
      final String format,
      final List<BrowsePaths> paths,
      final Component component)
  {
    checkNotNull(repositoryName);
    checkNotNull(format);
    checkNotNull(paths);
    checkArgument(!paths.isEmpty());
    checkNotNull(component);

    createNodes(repositoryName, paths).ifPresent(nodeId -> dao().linkComponent(nodeId, component));
  }

  @Override
  @Transactional
  public boolean assetNodeExists(final Asset asset) {
    return dao().assetNodeExists(asset);
  }

  @Override
  public void deleteAssetNode(final Asset asset) {
    checkNotNull(asset);
    Optional<DatastoreBrowseNode> browseNode = findBrowseNodeByAssetId(asset);

    browseNode.ifPresent(node -> {
      log.trace("Removing asset from browse node: {}", node);

      boolean deleted = maybeDeleteOrUnlink(dao -> dao.maybeDeleteAssetNode(asset), dao -> dao.unlinkAsset(asset));

      if (deleted && node.getParentId() != null) {
        log.trace("Node removed, maybe deleting parents.");
        maybeDeleteParents(node.getParentId());
      }
    });
  }

  @Override
  public void deleteComponentNode(final Component component) {
    checkNotNull(component);
    Optional<DatastoreBrowseNode> browseNode = findBrowseNodeByComponentId(component);

    browseNode.ifPresent(node -> {
      log.trace("Removing component from browse node: {}", node);

      boolean deleted =
          maybeDeleteOrUnlink(dao -> dao.maybeDeleteComponentNode(component), dao -> dao.unlinkComponent(component));

      if (deleted && node.getParentId() != null) {
        log.trace("Node removed, maybe deleting parents.");
        maybeDeleteParents(node.getParentId());
      }
    });
  }

  @Transactional
  protected Optional<DatastoreBrowseNode> findBrowseNodeByComponentId(Component component){
    return dao().findBrowseNodeByComponentId(component);
  }

  @Transactional
  protected Optional<DatastoreBrowseNode> findBrowseNodeByAssetId(Asset asset){
    return dao().findBrowseNodeByAssetId(asset);
  }

  /**
   * Returns true if {@code deleteFn} was successful, false otherwise.
   */
  @Transactional(retryOn = RetryUnlinkException.class)
  protected boolean maybeDeleteOrUnlink(
      final Predicate<BrowseNodeDAO> deleteFn,
      final Predicate<BrowseNodeDAO> unlinkFn)
  {
    BrowseNodeDAO dao = dao();
    if (deleteFn.test(dao)) {
      log.trace("Deleted node");
      return true;
    }
    else if (!unlinkFn.test(dao)) {
      log.trace("Failed to unlink");
      throw new RetryUnlinkException();
    }
    log.trace("Unlinked");
    return false;
  }

  @Transactional
  protected Optional<Integer> getParentBrowseNodeId(final Integer nodeId){
    return dao().getParentBrowseNodeId(nodeId);
  }

  @Transactional
  protected boolean tryDeleteNode(final Integer browseNodeId) {
    return dao().deleteBrowseNode(browseNodeId);
  }

  @Override
  public void deleteByRepository(final String repositoryName) {
    checkNotNull(repositoryName);

    log.info("Deleting browse nodes for repository: {}", repositoryName);

    ContentRepository repository = getContentRepository(repositoryName);

    long start = System.currentTimeMillis();
    long count = 0;
    int lastCount = 0;
    do {
      lastCount = deleteRepositoryNodes(repository);
      count += lastCount;
    }
    while (lastCount > 0);

    log.info("Finished browse node cleanup for {} nodes in {} taking {} ms", count, repositoryName,
        System.currentTimeMillis() - start);
  }

  @Transactional
  protected int deleteRepositoryNodes(final ContentRepository repository) {
    return dao().deleteRepository(repository, deletePageSize);
  }

  @Transactional
  @Override
  public Iterable<BrowseNode<Integer>> getByPath(
      final String repositoryName,
      final List<String> path,
      final int maxNodes)
  {
    List<SelectorConfiguration> selectors = emptyList();

    Repository repository = repositoryManager.get(repositoryName);
    ContentRepository contentRepository = getContentRepository(repository);
    int repositoryId = getContentRepositoryId(contentRepository);

    String format = repository.getFormat().getValue();
    if (!hasBrowsePermission(repositoryName, format)) {
      // user doesn't have repository-wide access so need to apply content selection
      selectors = selectorManager.browseActive(asList(repositoryName), asList(format));
      if (selectors.isEmpty()) {
        return emptyList(); // no browse permission and no selectors -> no results
      }
    }

    String pathString = path.isEmpty() ? null : path.stream().collect(Collectors.joining("/"));

    BrowseNodeFilter filter = browseNodeFilters.getOrDefault(repository.getFormat().getValue(), (node, name) -> true);

    List<BrowseNode<Integer>> results;
    if (repository.getType() instanceof GroupType) {
      Equivalence<DatastoreBrowseNode> browseNodeIdentity = getIdentity(repository);
      // overlay member results, first-one-wins if there are any nodes with the same name
      results = members(repository)
          .map(r -> dao().findChildren(getContentRepository(r), pathString, maxNodes)) // XXX NEXUS-22586 assetFilter & filterParameters
          .flatMap(iter -> StreamSupport.stream(iter.spliterator(), false))
          .map(browseNodeIdentity::wrap)
          .distinct()
          .map(Wrapper::get)
          .filter(node -> filter.test(node, node.getRepositoryId() == repositoryId))
          .limit(maxNodes)
          .collect(toList());
    }
    else {
      results = StreamSupport
          .stream(dao().findChildren(contentRepository, pathString, maxNodes).spliterator(), false)
          .filter(node -> filter.test(node, node.getRepositoryId() == repositoryId))
          .collect(toList());
    }

    results.sort(getBrowseNodeComparator(format));

    return results;
  }



  private Equivalence<DatastoreBrowseNode> getIdentity(final Repository repository) {
    Optional<BrowseNodeFacet> browseNodeFacet = repository.optionalFacet(BrowseNodeFacet.class);
    if (browseNodeFacet.isPresent()) {
      return Equivalence.equals().onResultOf(input -> browseNodeFacet.get().browseNodeIdentity().apply(input));
    }
    else {
      return Equivalence.equals().onResultOf(BrowseNode::getName);
    }
  }

  /**
   * Walk the given {@code browsePaths} creating nodes as needed returning the ID associated with the deepest browse
   * path.
   */
  private Optional<Integer> createNodes(final String repositoryName, final List<BrowsePaths> browsePaths) {
    Repository repository = repositoryManager.get(repositoryName);
    ContentRepository contentRepository = getContentRepository(repository);
    List<String> paths = browsePaths.stream().map(BrowsePaths::getRequestPath).collect(Collectors.toList());

    // Try to find the deepest node in the path to avoid failures creating pre-existing nodes.
    Optional<DatastoreBrowseNode> deepestNode =
        dao().findDeepestNode(contentRepository, paths.toArray(new String[paths.size()]));

    Integer parentId = null;
    List<BrowsePaths> nodesToCreate = browsePaths;

    if (deepestNode.isPresent()) {
      parentId = deepestNode.get().getId();
      int deepestExistingPathLength = deepestNode.get().getPath().length();

      nodesToCreate =
          browsePaths.stream().filter(bp -> (bp.getRequestPath().length() > deepestExistingPathLength))
              .collect(Collectors.toList());
    }

    // Create missing nodes in the path
    for (BrowsePaths browsePath : nodesToCreate) {
      DatastoreBrowseNode node = new DatastoreBrowseNode(repository.getFormat().getValue(), parentId,
          browsePath.getRequestPath(), browsePath.getBrowsePath());
      dao().createNode(contentRepository, node);
      parentId = node.getId();
    }

    return Optional.ofNullable(parentId);
  }

  private void maybeDeleteParents(final Integer nodeId) {
    Optional<Integer> parentId = getParentBrowseNodeId(nodeId);
    try {
      log.trace("Removing parent {}", nodeId);
      tryDeleteNode(nodeId);
    }
    catch (PersistenceException e) {
      log.trace("Failed to delete parent node: {}", nodeId, e);
      return;
    }
    parentId.ifPresent(this::maybeDeleteParents);
  }

  /**
   * Does the current user have permission to browse the full repository?
   */
  private boolean hasBrowsePermission(final String repositoryName, final String format) {
    return securityHelper.anyPermitted(new RepositoryViewPermission(format, repositoryName, BreadActions.BROWSE));
  }

  /**
   * Returns stream of all non-group repositories reachable from the given repository.
   */
  private static Stream<Repository> members(final Repository repository) {
    return repository.facet(GroupFacet.class).allMembers().stream();
  }

  private Comparator<BrowseNode<?>> getBrowseNodeComparator(final String format) {
    return browseNodeComparators.getOrDefault(format, defaultBrowseNodeComparator);
  }

  private ContentRepository getContentRepository(final Repository repository) {
    // TODO this is a lot of effort to get there.
    return (checkNotNull(contentRepositoryStores.get(repository.getFormat().getValue()))
        .readContentRepository(EntityHelper.id((Entity) repository.getConfiguration()))
        .orElseThrow(NullPointerException::new));
  }

  private ContentRepository getContentRepository(final String repositoryName) {
    return getContentRepository(repositoryManager.get(repositoryName));
  }

  private static int getContentRepositoryId(final ContentRepository contentRepository) {
    // TODO How should the Content store's ID for the repository be retrieved?
    return ((ContentRepositoryData) contentRepository).repositoryId;
  }

  private static class RetryUnlinkException
      extends RuntimeException
  {

  }
}
