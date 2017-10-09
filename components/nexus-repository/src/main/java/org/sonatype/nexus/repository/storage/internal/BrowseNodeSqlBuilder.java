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
package org.sonatype.nexus.repository.storage.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.repository.storage.DatabaseThreadUtils;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.selector.CselAssetSql;
import org.sonatype.nexus.selector.CselAssetSqlBuilder;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.base.Joiner;
import com.orientechnologies.orient.core.id.ORID;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

/**
 * Internal helper class that manages building the queries needed for retrieving lists of browse nodes
 *
 * @since 3.6
 */
@Named
public class BrowseNodeSqlBuilder
    extends ComponentSupport
{
  public static final String DB_CLASS = new OClassNameBuilder().type("browse_node").build();

  public static final String P_REPOSITORY_NAME = "repository_name";

  public static final String P_NODE_LIMIT = "node_limit";

  public static final String P_FILTER = "filter";

  public static final String P_ASSET_ID = "asset_id";

  public static final String P_COMPONENT_ID = "component_id";

  public static final String P_PATH = "path";

  public static final String P_PARENT_ID = "parent_id";

  public static final String P_AUTHZ_REPOSITORY_NAME = "authz_repository_name";

  public static final String P_CHILDREN_IDS = "children_ids";

  public static final String P_ID = "id";

  private static final String OR = " or ";

  private final SelectorManager selectorManager;

  private final CselAssetSqlBuilder cselAssetSqlBuilder;

  @Inject
  public BrowseNodeSqlBuilder(final SelectorManager selectorManager,
                              final CselAssetSqlBuilder cselAssetSqlBuilder)
  {
    this.selectorManager = checkNotNull(selectorManager);
    this.cselAssetSqlBuilder = checkNotNull(cselAssetSqlBuilder);
  }

  /**
   * Retrieve list of browse nodes, that are children of the provided parent.  Including where clauses that will
   * validate content selector security
   *
   * @param parentId            parent of the nodes to retrieve, if null, will retrieve list of root nodes
   * @param assetRepositoryName the repository we are pulling the records from
   * @param authzRepositoryName the repository we are authorizing against (this may differ from assetRepositoryName
   *                            just envision access content via a group repository
   * @param format              format of the repository in question
   * @param filter              filter to be applied to the result set (only nodes that match the filter, or have
   *                            children that match the filter will be returned)
   * @param parameters          A simple map that will be populated with parameters required for the query
   * @return                    The query string that can be passed into orient verbatim to retrieve some results
   */
  public String getBrowseNodesQueryWithContentSelectorAuthz(@Nullable final ORID parentId,
                                                            final String assetRepositoryName,
                                                            final String authzRepositoryName,
                                                            final String format,
                                                            @Nullable final String filter,
                                                            final Map<String, Object> parameters,
                                                            final int maxNodes)
  {
    parameters.put(P_REPOSITORY_NAME, assetRepositoryName);
    parameters.put(P_NODE_LIMIT, maxNodes);

    final StringBuilder query = new StringBuilder(String
        .format("select from %s let $asset = (select from $current.%s) where %s = :%s ", DB_CLASS, P_ASSET_ID,
            P_REPOSITORY_NAME, P_REPOSITORY_NAME));
    if (parentId == null) {
      query.append(String.format("and %s is null ", P_PARENT_ID));
    }
    else {
      parameters.put(P_PARENT_ID, parentId);
      query.append(String.format("and %s = :%s ", P_PARENT_ID, P_PARENT_ID));
    }
    if (!Strings2.isEmpty(filter)) {
      parameters.put(P_FILTER, "%" + Strings2.lower(filter) + "%");
      query.append(
          String.format("and (asset_id is null or $asset.%s.toLowerCase() like :%s) ", MetadataNodeEntityAdapter.P_NAME,
              P_FILTER));
    }
    query.append("and ");
    query.append(getAuthorizationWhereClause(authzRepositoryName, format, parameters, true));
    query.append(String.format(" limit :%s", P_NODE_LIMIT));

    log.debug("Assembled browse_node_with_content_selector_authz query: {}", query);
    return query.toString();
  }

  /**
   * Retrieve list of browse nodes, that are children of the provided parent.
   *
   * @param parentId            parent of the nodes to retrieve, if null, will retrieve list of root nodes
   * @param assetRepositoryName the repository we are pulling the records from
   * @param filter              filter to be applied to the result set (only nodes that match the filter, or have
   *                            children that match the filter will be returned)
   * @param parameters          A simple map that will be populated with parameters required for the query
   * @return                    The query string that can be passed into orient verbatim to retrieve some results
   */
  public String getBrowseNodesQuery(@Nullable final ORID parentId,
                                    final String assetRepositoryName,
                                    @Nullable final String filter,
                                    final Map<String, Object> parameters,
                                    final int maxNodes)
  {
    parameters.put(P_REPOSITORY_NAME, assetRepositoryName);
    parameters.put(P_NODE_LIMIT, maxNodes);

    final StringBuilder query = new StringBuilder(
        String.format("select from %s where %s = :%s ", DB_CLASS, P_REPOSITORY_NAME, P_REPOSITORY_NAME));
    if (parentId == null) {
      query.append(String.format("and %s is null ", P_PARENT_ID));
    }
    else {
      parameters.put(P_PARENT_ID, parentId);
      query.append(String.format("and %s = :%s ", P_PARENT_ID, P_PARENT_ID));
    }
    if (!Strings2.isEmpty(filter)) {
      parameters.put(P_FILTER, "%" + Strings2.lower(filter) + "%");
      query.append(String.format("and (%s is null or %s.%s.toLowerCase() like :%s) ", P_ASSET_ID, P_ASSET_ID,
          MetadataNodeEntityAdapter.P_NAME, P_FILTER));
    }
    query.append(String.format("limit :%s", P_NODE_LIMIT));

    log.debug("Assembled browse_node_with_repo_authz query: {}", query);
    return query.toString();
  }

  /**
   * Retrieve a query string that will return a single result if any of a particular nodes children (as defined by :id
   * parameter) match the supplied filter
   *
   * @param filter     Filter to use for comparison
   * @param parameters A simple map that will be populated with parameters required for the query
   * @return           The query string that can be passed into orient verbatim to retrieve some results
   */
  public String getChildMatchingFilterQuery(final String filter, final Map<String, Object> parameters) {
    parameters.put(P_FILTER, "%" + Strings2.lower(filter) + "%");

    final String query = String.format(
        "select @rid from (traverse %s, %s from (select from :%s)) where @class = 'asset' and %s.toLowerCase() like :%s limit 1",
        P_CHILDREN_IDS, P_ASSET_ID, P_ID, MetadataNodeEntityAdapter.P_NAME, P_FILTER);

    log.debug("Assembled apply_filter_to_children query: {}", query);
    return query;
  }

  /**
   * Retrieve a query string that will return a single result if any of a particular nodes children (as defined by :id
   * parameter) are allowed based upon security
   *
   * @param assetRepositoryName the repository we are pulling the records from
   * @param authzRepositoryName the repository we are authorizing against (this may differ from assetRepositoryName
   *                            just envision access content via a group repository
   * @param format              format of the repository in question
   * @param filter              Filter to use for comparison
   * @param parameters          A simple map that will be populated with parameters required for the query
   * @return                    The query string that can be passed into orient verbatim to retrieve some results
   */
  public String getAuthorizedChildMaybeMatchingFilter(final String assetRepositoryName,
                                                      final String authzRepositoryName,
                                                      final String format,
                                                      @Nullable final String filter,
                                                      final Map<String, Object> parameters)
  {
    parameters.put(P_REPOSITORY_NAME, assetRepositoryName);
    parameters.put(P_AUTHZ_REPOSITORY_NAME, authzRepositoryName);

    final String authClause = getAuthorizationWhereClause(authzRepositoryName, format, parameters, false);
    final StringBuilder query = new StringBuilder(String
        .format("select @rid from (traverse %s, %s from (select from :%s)) where ", P_CHILDREN_IDS, P_ASSET_ID, P_ID));
    query.append(authClause);

    if (filter != null) {
      parameters.put(P_FILTER, "%" + Strings2.lower(filter) + "%");
      query.append(String.format(" and (@class = 'asset' and %s.toLowerCase() like :%s)", MetadataNodeEntityAdapter.P_NAME, P_FILTER));
    }

    query.append(" limit 1");

    log.debug("Assembled apply_authorization_to_children query: {}", query);
    return query.toString();
  }

  private String getAuthorizationWhereClause(final String repositoryName,
                                             final String format,
                                             final Map<String, Object> parameters,
                                             final boolean browseNodeContext)
  {
    List<SelectorConfiguration> selectors = DatabaseThreadUtils
        .withOtherDatabase(() -> selectorManager.browseActive(asList(repositoryName), asList(format)));

    List<String> selectorsSql = new ArrayList<>(selectors.size());
    boolean allCsel =
        !selectors.isEmpty() && selectors.stream().allMatch(selector -> CselSelector.TYPE.equals(selector.getType()));

    String prefix = browseNodeContext ? "(asset_id is null or (" : "(@class = 'asset' and (";
    StringBuilder authClause = new StringBuilder(prefix);
    for (int i = 0; i < selectors.size(); i++) {
      if (CselSelector.TYPE.equals(selectors.get(i).getType())) {
        CselAssetSql cselAssetSql = cselAssetSqlBuilder
            .buildWhereClause((String) selectors.get(i).getAttributes().get("expression"), format,
                'a' + Integer.toString(i) + 'a', browseNodeContext ? "$asset." : "");
        parameters.putAll(cselAssetSql.getSqlParameters());
        selectorsSql.add('(' + cselAssetSql.getSql() + ')');
      }
    }
    if (!selectorsSql.isEmpty()) {
      authClause.append(Joiner.on(OR).join(selectorsSql));
    }
    if (!allCsel) {
      if (authClause.length() > prefix.length()) {
        authClause.append(OR);
      }
      authClause.append(
          "contentAuth(" + (browseNodeContext ? "@this.asset_id" : "@this") + ", :" + P_AUTHZ_REPOSITORY_NAME
              + ", true) = true");
      parameters.put(P_AUTHZ_REPOSITORY_NAME, repositoryName);
    }
    authClause.append("))");
    return authClause.toString();
  }
}
