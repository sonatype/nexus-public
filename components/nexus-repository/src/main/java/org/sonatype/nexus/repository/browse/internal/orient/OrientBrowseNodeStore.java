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
package org.sonatype.nexus.repository.browse.internal.orient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.template.EscapeHelper;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseListItem;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeFilter;
import org.sonatype.nexus.repository.browse.node.BrowseNodeIdentity;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.internal.ComponentSchemaRegistration;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;
import static org.sonatype.nexus.repository.browse.internal.orient.BrowseNodeEntityAdapter.AUTHZ_REPOSITORY_NAME;
import static org.sonatype.nexus.repository.browse.internal.orient.BrowseNodeEntityAdapter.P_ASSET_ID;
import static org.sonatype.nexus.repository.browse.internal.orient.BrowseNodeEntityAdapter.P_FORMAT;
import static org.sonatype.nexus.repository.browse.internal.orient.BrowseNodeEntityAdapter.P_PATH;

/**
 * Orient {@link BrowseNode} store that also acts as a {@link BrowseNodeQueryService}.
 *
 * Note that this store does not register the browse-node schema because that needs to be registered
 * after the component and asset schemas. To guarantee ordering, registration of all those schemas is
 * done by {@link ComponentSchemaRegistration}.
 *
 * @since 3.7
 */
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
@Priority(Integer.MAX_VALUE) // make sure this implementation appears above the datastore one in mixed-mode
@Named
public class OrientBrowseNodeStore
    extends StateGuardLifecycleSupport
    implements BrowseNodeQueryService
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final BrowseNodeEntityAdapter entityAdapter;

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  private final Map<String, BrowseNodeFilter> browseNodeFilters;

  private final Map<String, BrowseNodeIdentity> browseNodeIdentities;

  private final Map<String, BrowseNodeComparator> browseNodeComparators;

  private final BrowseNodeComparator defaultBrowseNodeComparator;

  private final int deletePageSize;

  @Inject
  public OrientBrowseNodeStore(
      @Named("component") final Provider<DatabaseInstance> databaseInstance,
      final BrowseNodeEntityAdapter entityAdapter,
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final BrowseNodeConfiguration configuration,
      final Map<String, BrowseNodeFilter> browseNodeFilters,
      final Map<String, BrowseNodeIdentity> browseNodeIdentities,
      final Map<String, BrowseNodeComparator> browseNodeComparators)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
    this.browseNodeFilters = checkNotNull(browseNodeFilters);
    this.browseNodeIdentities = checkNotNull(browseNodeIdentities);
    this.browseNodeComparators = checkNotNull(browseNodeComparators);
    this.deletePageSize = configuration.getDeletePageSize();
    this.defaultBrowseNodeComparator = checkNotNull(browseNodeComparators.get(DefaultBrowseNodeComparator.NAME));
  }

  @Guarded(by = STARTED)
  public void createComponentNode(final String repositoryName,
                                  final String format,
                                  final List<BrowsePath> paths,
                                  final Component component)
  {
    inTxRetry(databaseInstance)
        // handle case where assets try to create the exact same component-level path at once
        .retryOn(ONeedRetryException.class, ORecordDuplicatedException.class)
        .run(db -> entityAdapter.createComponentNode(db, repositoryName, format, paths, component));
  }

  @Guarded(by = STARTED)
  public void createAssetNode(final String repositoryName,
                              final String format,
                              final List<BrowsePath> paths,
                              final Asset asset)
  {
    inTxRetry(databaseInstance)
        // handle case where an asset and its component try to create the exact same path at once
        .retryOn(ONeedRetryException.class, ORecordDuplicatedException.class)
        .run(db -> entityAdapter.createAssetNode(db, repositoryName, format, paths, asset));
  }

  @Guarded(by = STARTED)
  public boolean assetNodeExists(final Asset asset) {
    EntityId assetId = EntityHelper.id(asset);
    return inTx(databaseInstance).call(db -> entityAdapter.assetNodeExists(db, assetId));
  }

  @Guarded(by = STARTED)
  public void deleteComponentNode(final Component component) {
    EntityId componentId = EntityHelper.id(component);
    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteComponentNode(db, componentId));
  }

  @Guarded(by = STARTED)
  public void deleteAssetNode(final Asset asset) {
    EntityId assetId = EntityHelper.id(asset);
    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteAssetNode(db, assetId));
  }

  @Guarded(by = STARTED)
  public void deleteByRepository(final String repositoryName) {
    log.debug("Deleting all browse nodes for repository {}", repositoryName);

    ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);

    int deletedCount;
    do {
      deletedCount = inTxRetry(databaseInstance).call(
          db -> entityAdapter.deleteByRepository(db, repositoryName, deletePageSize));

      progressLogger.info("Deleted {} browse nodes for repository {} in {}",
          deletedCount, repositoryName, progressLogger.getElapsed());
    }
    while (deletedCount == deletePageSize);

    progressLogger.flush();

    log.debug("All browse nodes deleted for repository {} in {}", repositoryName, progressLogger.getElapsed());
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<BrowseNode> getByPath(
      final Repository repository,
      final List<String> path,
      final int maxNodes)
  {
    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    List<SelectorConfiguration> selectors = emptyList();
    if (!hasBrowsePermission(repositoryName, format)) {
      // user doesn't have repository-wide access so need to apply content selection
      selectors = selectorManager.browseActive(asList(repositoryName), asList(format));
      if (selectors.isEmpty()) {
        return emptyList(); // no browse permission and no selectors -> no results
      }
    }

    Map<String, Object> filterParameters = new HashMap<>();
    String assetFilter = buildAssetFilter(repository, selectors, filterParameters);

    BrowseNodeFilter filter = browseNodeFilters.getOrDefault(format, (node, name) -> true);

    List<BrowseNode> results;
    if (repository.getType() instanceof GroupType) {
      BrowseNodeIdentity identity = browseNodeIdentities.getOrDefault(format, BrowseNode::getName);
      Equivalence<OrientBrowseNode> browseNodeEquivalence = Equivalence.equals().onResultOf(identity::identity);

      // overlay member results, first-one-wins if there are any nodes with the same name
      results = members(repository)
          .map(m -> getByPath(m.getName(), path, maxNodes, assetFilter, filterParameters))
          .flatMap(List::stream)
          .map(browseNodeEquivalence::wrap)
          .distinct()
          .map(Wrapper::get)
          .filter(node -> filter.test(node, repositoryName.equals(node.getRepositoryName())))
          .limit(maxNodes)
          .collect(toList());
    }
    else {
      results = getByPath(repository.getName(), path, maxNodes, assetFilter, filterParameters).stream()
          .filter(node -> filter.test(node, repositoryName.equals(node.getRepositoryName())))
          .collect(toList());
    }

    results.sort(getBrowseNodeComparator(format));

    return results;
  }

  /**
   * Returns stream of all non-group repositories reachable from the given repository.
   */
  private static Stream<Repository> members(final Repository repository) {
    return repository.facet(GroupFacet.class).leafMembers().stream();
  }

  /**
   * Returns the browse nodes directly visible under the path according to the given asset filter.
   */
  private List<OrientBrowseNode> getByPath(
      final String repositoryName,
      final List<String> path,
      final int maxNodes,
      @Nullable final String assetFilter,
      @Nullable final Map<String, Object> filterParameters)
  {
    return inTx(databaseInstance)
        .call(db -> entityAdapter.getByPath(db, repositoryName, path, maxNodes, assetFilter, filterParameters));
  }

  /**
   * Builds an asset filter in SQL for the current user.
   */
  private String buildAssetFilter(final Repository repository,
                                  final List<SelectorConfiguration> selectors,
                                  final Map<String, Object> filterParameters)
  {
    StringBuilder filterBuilder = new StringBuilder();
    if (!selectors.isEmpty()) {
      appendContentAuthFilter(filterBuilder, repository, selectors, filterParameters);
    }
    return filterBuilder.toString();
  }

  /**
   * Does the current user have permission to browse the full repository?
   */
  private boolean hasBrowsePermission(final String repositoryName, final String format) {
    return securityHelper.anyPermitted(new RepositoryViewPermission(format, repositoryName, BreadActions.BROWSE));
  }

  /**
   * Appends a content authentication filter in SQL for the current user.
   */
  private void appendContentAuthFilter(final StringBuilder filterBuilder,
                                       final Repository repository,
                                       final List<SelectorConfiguration> selectors,
                                       final Map<String, Object> filterParameters)
  {
    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    if (selectors.size() > 1) {
      filterBuilder.append('(');
    }

    SelectorSqlBuilder sqlBuilder = new SelectorSqlBuilder()
        .propertyAlias("path", P_PATH)
        .propertyAlias("format", P_FORMAT)
        .parameterPrefix(":")
        .propertyPrefix(P_ASSET_ID + ".attributes." + format + ".");

    int cselCount = 0;

    for (SelectorConfiguration selector : selectors) {
      if (CselSelector.TYPE.equals(selector.getType())) {
        try {
          sqlBuilder.parameterNamePrefix("s" + cselCount + "p");

          selectorManager.toSql(selector, sqlBuilder);

          if (cselCount > 0) {
            filterBuilder.append(" or ");
          }

          filterBuilder.append('(').append(sqlBuilder.getQueryString()).append(')');
          filterParameters.putAll(sqlBuilder.getQueryParameters());

          cselCount++;
        }
        catch (SelectorEvaluationException e) {
          log.warn("Problem evaluating selector {} as SQL", selector.getName(), e);
        }
        finally {
          sqlBuilder.clearQueryString();
        }
      }
    }

    if (selectors.size() > cselCount) {
      if (cselCount > 0) {
        filterBuilder.append(" or ");
      }

      // call 'contentAuth' function if we need to evaluate any non-CSEL selectors (such as JEXL based selectors)
      filterBuilder.append(
          String.format("contentAuth(@this.%s, @this.%s, :%s, true) = true", P_PATH, P_FORMAT, AUTHZ_REPOSITORY_NAME));

      filterParameters.put(AUTHZ_REPOSITORY_NAME, repositoryName);
    }

    if (selectors.size() > 1) {
      filterBuilder.append(')');
    }
  }

  private Comparator<BrowseNode> getBrowseNodeComparator(final String format) {
    return browseNodeComparators.getOrDefault(format, defaultBrowseNodeComparator);
  }

  @Override
  public List<BrowseListItem> toListItems(final Repository repository, final Iterable<BrowseNode> nodes)
  {
    List<BrowseListItem> listItems = new ArrayList<>();

    if (nodes != null) {
      SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
      for (BrowseNode browseNode : nodes) {
        String size = null;
        String lastModified = null;
        String listItemPath;
        if (browseNode.isLeaf()) {
          Asset asset = getAssetById(repository, browseNode.getAssetId());

          if (asset == null) {
            log.error("Could not find expected asset (id): {} ({}) in repository: {}",
                browseNode.getPath(), browseNode.getAssetId(), repository.getName());
            //something bad going on here, move along to the next
            continue;
          }

          size = String.valueOf(asset.size());
          lastModified = Optional.ofNullable(asset.blobUpdated()).map(dateTime -> format.format(dateTime.toDate()))
              .orElse("");
          listItemPath = getListItemPath(repository, browseNode, asset);
        }
        else {
          listItemPath = getListItemPath(repository, browseNode, null);
        }

        listItems.add(
            new BrowseListItem(listItemPath, browseNode.getName(), !browseNode.isLeaf(), lastModified, size,
                ""));
      }
    }

    return listItems;
  }

  private Asset getAssetById(final Repository repository, final EntityId assetId) {
    Optional<GroupFacet> optionalGroupFacet = repository.optionalFacet(GroupFacet.class);
    List<Repository> members = optionalGroupFacet.isPresent() ? optionalGroupFacet.get().allMembers()
        : Collections.singletonList(repository);

    return Transactional.operation.withDb(repository.facet(StorageFacet.class).txSupplier()).call(() -> {
      StorageTx tx = UnitOfWork.currentTx();
      Asset candidate = tx.findAsset(assetId);
      if (candidate != null) {
        // we just fetched the asset so we know its bucketId will have a DB record attached
        ODocument bucketRecord = ((AttachedEntityId) candidate.bucketId()).getIdentity().getRecord();
        String asssetBucketRepositoryName = bucketRecord.field(BucketEntityAdapter.P_REPOSITORY_NAME);
        if (members.stream().anyMatch(repo -> repo.getName().equals(asssetBucketRepositoryName))) {
          return candidate;
        }
      }

      return null;
    });
  }

  private static final EscapeHelper escapeHelper = new EscapeHelper();

  private String getListItemPath(final Repository repository,
                                 final BrowseNode browseNode,
                                 final Asset asset)
  {
    final String listItemPath;

    if (asset == null) {
      listItemPath = escapeHelper.uri(browseNode.getName()) + "/";
    }
    else {
      listItemPath = repository.getUrl() + "/" +
          Stream.of(asset.name().split("/"))
              .map(escapeHelper::uri)
              .collect(Collectors.joining("/"));
    }

    return listItemPath;
  }
}
