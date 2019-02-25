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
package org.sonatype.nexus.repository.storage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;
import static org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter.AUTHZ_REPOSITORY_NAME;
import static org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter.P_ASSET_ID;
import static org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter.P_ASSET_NAME_LOWERCASE;

/**
 * @since 3.7
 */
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
@Named
public class BrowseNodeStoreImpl
    extends StateGuardLifecycleSupport
    implements BrowseNodeStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final BrowseNodeEntityAdapter entityAdapter;

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  private final Map<String, BrowseNodeFilter> browseNodeFilters;

  private final Map<String, BrowseNodeComparator> browseNodeComparators;

  private final BrowseNodeComparator defaultBrowseNodeComparator;

  private final int deletePageSize;

  @Inject
  public BrowseNodeStoreImpl(@Named("component") final Provider<DatabaseInstance> databaseInstance,
                             final BrowseNodeEntityAdapter entityAdapter,
                             final SecurityHelper securityHelper,
                             final SelectorManager selectorManager,
                             final BrowseNodeConfiguration configuration,
                             final Map<String, BrowseNodeFilter> browseNodeFilters,
                             final Map<String, BrowseNodeComparator> browseNodeComparators)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
    this.browseNodeFilters = checkNotNull(browseNodeFilters);
    this.browseNodeComparators = checkNotNull(browseNodeComparators);
    this.deletePageSize = configuration.getDeletePageSize();
    this.defaultBrowseNodeComparator = checkNotNull(browseNodeComparators.get(DefaultBrowseNodeComparator.NAME));
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void createComponentNode(final String repositoryName, final List<String> path, final Component component) {
    inTxRetry(databaseInstance)
        // handle case where assets try to create the exact same component-level path at once
        .retryOn(ONeedRetryException.class, ORecordDuplicatedException.class)
        .run(db -> entityAdapter.createComponentNode(db, repositoryName, path, component));
  }

  @Override
  @Guarded(by = STARTED)
  public void createAssetNode(final String repositoryName, final List<String> path, final Asset asset) {
    inTxRetry(databaseInstance)
        // handle case where an asset and its component try to create the exact same path at once
        .retryOn(ONeedRetryException.class, ORecordDuplicatedException.class)
        .run(db -> entityAdapter.createAssetNode(db, repositoryName, path, asset));
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteComponentNode(EntityId componentId) {
    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteComponentNode(db, componentId));
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteAssetNode(final EntityId assetId) {
    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteAssetNode(db, assetId));
  }

  @Override
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
  public Iterable<BrowseNode> getByPath(final Repository repository,
                                        final List<String> path,
                                        final int maxNodes,
                                        @Nullable final String keyword)
  {
    List<SelectorConfiguration> selectors = emptyList();

    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();
    if (!hasBrowsePermission(repositoryName, format)) {
      // user doesn't have repository-wide access so need to apply content selection
      selectors = selectorManager.browseActive(asList(repositoryName), asList(format));
      if (selectors.isEmpty()) {
        return emptyList(); // no browse permission and no selectors -> no results
      }
    }

    Map<String, Object> filterParameters = new HashMap<>();
    String assetFilter = buildAssetFilter(repository, keyword, selectors, filterParameters);

    BrowseNodeFilter filter = browseNodeFilters.getOrDefault(repository.getFormat().getValue(), (node, name) -> true);

    List<BrowseNode> results;
    if (repository.getType() instanceof GroupType) {
      // overlay member results, first-one-wins if there are any nodes with the same name
      results = members(repository)
          .map(m -> getByPath(m.getName(), path, maxNodes, assetFilter, filterParameters))
          .flatMap(List::stream)
          .filter(distinctByName())
          .filter(node -> filter.test(node, repositoryName))
          .limit(maxNodes)
          .collect(toList());
    }
    else {
      results = getByPath(repository.getName(), path, maxNodes, assetFilter, filterParameters).stream()
          .filter(node -> filter.test(node, repositoryName))
          .collect(toList());
    }

    results.sort(getBrowseNodeComparator(format));

    return results;
  }

  /**
   * Returns a filter that discards nodes which have the same name as an earlier node.
   *
   * Warning: this method is not thread-safe, so don't use it with a parallel stream
   */
  private static Predicate<BrowseNode> distinctByName() {
    Set<String> names = new HashSet<>();
    return node -> names.add(node.getName());
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
  private List<BrowseNode> getByPath(final String repositoryName,
                                     final List<String> path,
                                     final int maxNodes,
                                     @Nullable final String assetFilter,
                                     @Nullable final Map<String, Object> filterParameters)
  {
    return inTx(databaseInstance).call(
        db -> entityAdapter.getByPath(db, repositoryName, path, maxNodes, assetFilter, filterParameters));
  }

  /**
   * Builds an asset filter in SQL for the current user.
   */
  private String buildAssetFilter(final Repository repository,
                                  @Nullable final String keyword,
                                  final List<SelectorConfiguration> selectors,
                                  final Map<String, Object> filterParameters)
  {
    StringBuilder filterBuilder = new StringBuilder();
    if (keyword != null) {
      appendKeywordFilter(filterBuilder, keyword, filterParameters);
    }
    if (!selectors.isEmpty()) {
      if (filterBuilder.length() > 0) {
        filterBuilder.append(" and ");
      }
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
   * Appends a keyword filter in SQL.
   */
  private void appendKeywordFilter(final StringBuilder filterBuilder,
                                   final String keyword,
                                   final Map<String, Object> filterParameters)
  {
    filterBuilder.append(P_ASSET_NAME_LOWERCASE).append(" like :keyword_filter");
    filterParameters.put("keyword_filter", '%' + Strings2.lower(keyword) + '%');
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
        .propertyAlias("path", P_ASSET_ID + ".name")
        .propertyAlias("format", P_ASSET_ID + ".format")
        .propertyPrefix(P_ASSET_ID + ".attributes." + format + ".");

    int cselCount = 0;

    for (SelectorConfiguration selector : selectors) {
      if (CselSelector.TYPE.equals(selector.getType())) {
        try {
          sqlBuilder.parameterPrefix("s" + cselCount + "p");

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
      filterBuilder.append(String.format("contentAuth(@this.%s, :%s, true) = true", P_ASSET_ID, AUTHZ_REPOSITORY_NAME));

      filterParameters.put(AUTHZ_REPOSITORY_NAME, repositoryName);
    }

    if (selectors.size() > 1) {
      filterBuilder.append(')');
    }
  }

  private Comparator<BrowseNode> getBrowseNodeComparator(String format) {
    return browseNodeComparators.getOrDefault(format, defaultBrowseNodeComparator);
  }
}
