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
package org.sonatype.nexus.repository.content.browse.store;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.template.EscapeHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseListItem;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.BrowseNodeFilter;
import org.sonatype.nexus.repository.browse.node.BrowseNodeIdentity;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH_CHAR;
import static org.sonatype.nexus.repository.content.browse.store.BrowseNodeDAO.FILTER_PARAMS;
import static org.sonatype.nexus.repository.content.store.InternalIds.contentRepositoryId;

/**
 * New-DB implementation of {@link BrowseNodeQueryService}.
 *
 * @since 3.26
 */
@Named
@Singleton
public class BrowseNodeQueryServiceImpl
    extends StateGuardLifecycleSupport
    implements BrowseNodeQueryService
{
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");

  private static final EscapeHelper escapeHelper = new EscapeHelper();

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  private final Map<String, BrowseNodeFilter> browseNodeFilters;

  private final Map<String, BrowseNodeIdentity> browseNodeIdentities;

  private final Map<String, BrowseNodeComparator> browseNodeComparators;

  private final BrowseNodeComparator defaultBrowseNodeComparator;

  private final ContentAuthHelper contentAuthHelper;

  @Inject
  public BrowseNodeQueryServiceImpl(
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final Map<String, BrowseNodeFilter> browseNodeFilters,
      final Map<String, BrowseNodeIdentity> browseNodeIdentities,
      final Map<String, BrowseNodeComparator> browseNodeComparators,
      final ContentAuthHelper contentAuthHelper)
  {
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
    this.browseNodeFilters = checkNotNull(browseNodeFilters);
    this.browseNodeIdentities = checkNotNull(browseNodeIdentities);
    this.browseNodeComparators = checkNotNull(browseNodeComparators);
    this.defaultBrowseNodeComparator = checkNotNull(browseNodeComparators.get(DefaultBrowseNodeComparator.NAME));
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
  }

  @Override
  public Iterable<BrowseNode> getByPath(
      final Repository repository,
      final List<String> displayPath,
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
    String contentFilter = buildContentAuthFilter(repository, selectors, filterParameters);
    boolean hasJexl = selectors.stream().anyMatch(this::isJexl);

    List<BrowseNode> results;
    if (repository.getType() instanceof GroupType) {
      int repositoryId = contentRepositoryId(repository);

      BrowseNodeIdentity identity = browseNodeIdentities.getOrDefault(format, BrowseNode::getName);
      Equivalence<BrowseNode> browseNodeEquivalence = Equivalence.equals().onResultOf(identity::identity);
      BrowseNodeFilter filter = browseNodeFilters.getOrDefault(format, (node, name) -> true);

      // overlay member results, first-one-wins if there are any nodes with the same name
      results = members(repository)
          .map(m -> selectByPath(m, displayPath, maxNodes, contentFilter, filterParameters, hasJexl))
          .flatMap(List::stream)
          .map(browseNodeEquivalence::wrap)
          .distinct()
          .map(Wrapper::get)
          .filter(node -> filter.test(node, repositoryId == ((BrowseNodeData) node).repositoryId))
          .limit(maxNodes)
          .collect(toList());
    }
    else {
      results = selectByPath(repository, displayPath, maxNodes, contentFilter, filterParameters, hasJexl);
    }

    results.sort(getBrowseNodeComparator(format));

    return results;
  }

  /**
   * Retrieve the browse nodes directly under the given display path in the selected repository.
   */
  private List<BrowseNode> selectByPath(
      final Repository repository,
      final List<String> displayPath,
      final int maxNodes,
      @Nullable final String contentFilter,
      @Nullable final Map<String, Object> filterParams,
      final boolean hasJexl)
  {
    List<BrowseNode> nodes = repository.optionalFacet(BrowseFacet.class)
        .map(facet -> facet.getByDisplayPath(displayPath, maxNodes, contentFilter, filterParams))
        .orElse(ImmutableList.of());

    if (hasJexl) {
      // additional filtering that we couldn't do in SQL
      String repositoryName = repository.getName();
      String format = repository.getFormat().getValue();
      nodes = nodes.stream()
          .filter(node -> contentAuthHelper.checkPathPermissionsJexlOnly(node.getPath(), format, repositoryName))
          .collect(toList());
    }

    return nodes;
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
    return repository.facet(GroupFacet.class).leafMembers().stream();
  }

  /**
   * Returns the potentially format-specific comparator to use for sorting browse nodes.
   */
  private Comparator<BrowseNode> getBrowseNodeComparator(final String format) {
    return browseNodeComparators.getOrDefault(format, defaultBrowseNodeComparator);
  }

  private boolean isJexl(final SelectorConfiguration selectorConfiguration) {
    return JexlSelector.TYPE.equals(selectorConfiguration.getType());
  }

  /**
   * Builds a browse node filter in SQL for the current user.
   */
  @Nullable
  private String buildContentAuthFilter(final Repository repository,
                                        final List<SelectorConfiguration> selectors,
                                        final Map<String, Object> filterParameters)
  {
    if (selectors.isEmpty()) {
      return null;
    }

    StringBuilder filterBuilder = new StringBuilder();
    appendContentAuthFilter(filterBuilder, repository, selectors, filterParameters);
    return filterBuilder.toString();
  }

  /**
   * Appends a content authentication filter in SQL for the current user.
   */
  private void appendContentAuthFilter(final StringBuilder filterBuilder,
                                       final Repository repository,
                                       final List<SelectorConfiguration> selectors,
                                       final Map<String, Object> filterParameters)
  {
    String format = repository.getFormat().getValue();

    if (selectors.size() > 1) {
      filterBuilder.append('(');
    }

    SelectorSqlBuilder sqlBuilder = new SelectorSqlBuilder()
        .propertyAlias("path", "B.request_path")
        .propertyAlias("format", "'" + format + "'")
        .propertyPrefix("B.")
        .parameterPrefix("#{" + FILTER_PARAMS + ".")
        .parameterSuffix("}");

    int selectorCount = 0;

    for (SelectorConfiguration selector : selectors) {
      if (CselSelector.TYPE.equals(selector.getType())) {
        try {
          sqlBuilder.parameterNamePrefix("s" + selectorCount + "p");

          selectorManager.toSql(selector, sqlBuilder);

          if (selectorCount > 0) {
            filterBuilder.append(" or ");
          }

          filterBuilder.append('(').append(sqlBuilder.getQueryString()).append(')');
          filterParameters.putAll(sqlBuilder.getQueryParameters());

          selectorCount++;
        }
        catch (SelectorEvaluationException e) {
          log.warn("Problem evaluating selector {} as SQL", selector.getName(), e);
        }
        finally {
          sqlBuilder.clearQueryString();
        }
      }
    }

    if (selectors.size() > 1) {
      filterBuilder.append(')');
    }
  }

  @Override
  public List<BrowseListItem> toListItems(final Repository repository, final Iterable<BrowseNode> nodes) {
    List<BrowseListItem> items = new ArrayList<>();
    if (nodes == null) {
      return items;
    }

    for (BrowseNode node : nodes) {
      String lastModified = null;
      String size = null;
      String link;

      if (node.isLeaf()) {
        Optional<FluentAsset> asset = getAssetById(repository, node.getAssetId());

        if (!asset.isPresent()) {
          log.error("Could not find expected asset (id): {} ({}) in repository: {}",
              node.getPath(), node.getAssetId(), repository.getName());
          // expected an asset here but it's missing, move along to the next node
          continue;
        }

        Optional<AssetBlob> blob = asset.get().blob();
        if (blob.isPresent()) {
          // blobs are immutable so "last-modified" is the creation time of the latest blob
          lastModified = DATE_TIME_FORMAT.format(blob.get().blobCreated().toZonedDateTime());
          size = Long.toString(blob.get().blobSize());
        }

        link = getAssetLink(repository, asset.get());
      }
      else {
        link = escapeHelper.uri(node.getName()) + SLASH_CHAR;
      }

      items.add(new BrowseListItem(link, node.getName(), !node.isLeaf(), lastModified, size, ""));
    }

    return items;
  }

  /**
   * Retrieves the asset associated with the external id.
   */
  private Optional<FluentAsset> getAssetById(final Repository repository, final EntityId assetId) {
    return assetId != null ? repository.facet(ContentFacet.class).assets().find(assetId) : empty();
  }

  /**
   * Get a link to the asset for use in the HTML view.
   */
  private String getAssetLink(final Repository repository, final Asset asset) {
    return repository.getUrl() + Stream.of(asset.path().split(SLASH)).map(escapeHelper::uri).collect(joining(SLASH));
  }
}
