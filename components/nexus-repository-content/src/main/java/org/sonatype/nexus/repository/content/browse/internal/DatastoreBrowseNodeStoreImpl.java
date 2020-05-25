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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeCrudStore;
import org.sonatype.nexus.repository.browse.node.BrowseNodeFacet;
import org.sonatype.nexus.repository.browse.node.BrowseNodeFilter;
import org.sonatype.nexus.repository.browse.node.BrowseNodeStore;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;
import org.sonatype.nexus.repository.content.store.ContentStoreSupport;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.selector.DatastoreContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import org.apache.ibatis.exceptions.PersistenceException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.repository.content.browse.internal.BrowseNodeDAOQueryBuilder.WHERE_PARAMS;

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

  private final DatastoreContentAuthHelper contentAuthHelper;

  @Inject
  public DatastoreBrowseNodeStoreImpl(
      final DataSessionSupplier sessionSupplier,
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final BrowseNodeConfiguration configuration,
      final RepositoryManager repositoryManager,
      final DatastoreContentAuthHelper contentAuthHelper,
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
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
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
      final List<BrowsePath> paths,
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
      final List<BrowsePath> paths,
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
  public List<BrowseNode<Integer>> getByPath(
      final String repositoryName,
      final List<String> path,
      final int maxNodes)
  {
    Repository repository = checkNotNull(repositoryManager.get(repositoryName));
    String format = repository.getFormat().getValue();
    int repositoryId = getContentRepository(repository).contentRepositoryId();

    List<SelectorConfiguration> selectors = emptyList();
    if (!hasBrowsePermission(repositoryName, format)) {
      // user doesn't have repository-wide access so need to apply content selection
      selectors = selectorManager.browseActive(asList(repositoryName), asList(format));
      if (selectors.isEmpty()) {
        return emptyList(); // no browse permission and no selectors -> no results
      }
    }

    BrowseNodeFilter filter = browseNodeFilters.getOrDefault(repository.getFormat().getValue(), (node, name) -> true);

    String[] repositories = repositoryStream(repository)
        .map(Repository::getName)
        .collect(toList())
        .toArray(new String[]{});

    String pathString = path.isEmpty() ? null : join("/", path);

    //We must do jexl matching in java, so if there is a jexl selector, filter for both jexl and csel selectors
    List<BrowseNode<Integer>> results;
    if (selectors.stream().anyMatch(this::isJexl)) {
      results = fetchPermittedNodes(repository, maxNodes,
          cr -> dao().findChildren(cr, pathString, maxNodes, emptyList(), emptyMap()),
          bn -> contentAuthHelper.checkPathPermissions(bn.getPath(), bn.getFormat(), repositories) &&  filter.test(bn, bn.getRepositoryId() == repositoryId));
    } else {
      Map<String, Object> cselParams = new HashMap<>();
      List<String> cselSelectors = parseCselSelectors(selectors, cselParams);

      results = fetchPermittedNodes(repository, maxNodes,
          cr -> dao().findChildren(cr, pathString, maxNodes, cselSelectors, cselParams),
          node -> filter.test(node, node.getRepositoryId() == repositoryId)
      );
    }

    results.sort(getBrowseNodeComparator(format));

    return results;
  }

  private List<BrowseNode<Integer>> fetchPermittedNodes(final Repository repository,
                                               final long maxNodes,
                                               Function<ContentRepository, Iterable<DatastoreBrowseNode>> findChildren,
                                               Predicate<DatastoreBrowseNode> filterNodePredicate) {
    Stream<DatastoreBrowseNode> nodeStream;
    if (repository.getType() instanceof GroupType) {
      Equivalence<BrowseNode<Integer>> browseNodeIdentity = getIdentity(repository);

      // overlay member results, first-one-wins if there are any nodes with the same name
      nodeStream = members(repository)
          .map(this::getContentRepository)
          .map(findChildren)
          .flatMap(iter -> stream(iter.spliterator(), false))
          .map(browseNodeIdentity::wrap)
          .distinct()
          .map(Wrapper::get);
    }
    else {
      nodeStream = of(repository)
          .map(this::getContentRepository)
          .map(findChildren)
          .flatMap(iter -> stream(iter.spliterator(), false));
    }

    return nodeStream
        .filter(filterNodePredicate)
        .limit(maxNodes)
        .collect(toList());
  }

  private Stream<Repository> repositoryStream(final Repository repository) {
    return isGroup(repository) ? members(repository) : of(repository);
  }

  private boolean isGroup(final Repository repository) {
    return repository.getType() instanceof GroupType;
  }

  private boolean isJexl(final SelectorConfiguration selectorConfiguration) {
    return JexlSelector.TYPE.equals(selectorConfiguration.getType());
  }

  /**
   * Parses each {@link CselSelector} configuration into a SQL string and populates the selectorParameters
   */
  private List<String> parseCselSelectors(
      final List<SelectorConfiguration> cselSelectors,
      final Map<String, Object> selectorParameters)
  {
    SelectorSqlBuilder sqlBuilder = new SelectorSqlBuilder()
        .propertyAlias("path", "path")
        .propertyAlias("format", "format")
        .parameterPrefix("#{" + WHERE_PARAMS + ".")
        .parameterSuffix("}")
        .propertyPrefix("B.");

    List<String> contentSelectors = new ArrayList<>(cselSelectors.size());
    int cselCount = 0;

    for (SelectorConfiguration selector : cselSelectors) {
        try {
          sqlBuilder.parameterNamePrefix("s" + cselCount++ + "p");

          selectorManager.toSql(selector, sqlBuilder);

          contentSelectors.add(sqlBuilder.getQueryString());
          selectorParameters.putAll(sqlBuilder.getQueryParameters());
        }
        catch (SelectorEvaluationException e) {
          log.warn("Problem evaluating selector {} as SQL", selector.getName(), e);
        }
        finally {
          sqlBuilder.clearQueryString();
        }
    }

    return contentSelectors;
  }

  private Equivalence<BrowseNode<Integer>> getIdentity(final Repository repository) {
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
  private Optional<Integer> createNodes(final String repositoryName, final List<BrowsePath> browsePaths) {
    Repository repository = repositoryManager.get(repositoryName);
    ContentRepository contentRepository = getContentRepository(repository);
    List<String> paths = browsePaths.stream().map(BrowsePath::getRequestPath).collect(Collectors.toList());

    // Try to find the deepest node in the path to avoid failures creating pre-existing nodes.
    Optional<DatastoreBrowseNode> deepestNode =
        dao().findDeepestNode(contentRepository, paths.toArray(new String[paths.size()]));

    Integer parentId = null;
    List<BrowsePath> nodesToCreate = browsePaths;

    if (deepestNode.isPresent()) {
      parentId = deepestNode.get().getId();
      int deepestExistingPathLength = deepestNode.get().getPath().length();

      nodesToCreate =
          browsePaths.stream().filter(bp -> (bp.getRequestPath().length() > deepestExistingPathLength))
              .collect(Collectors.toList());
    }

    // Create missing nodes in the path
    for (BrowsePath browsePath : nodesToCreate) {
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

  private static class RetryUnlinkException
      extends RuntimeException
  {

  }
}
