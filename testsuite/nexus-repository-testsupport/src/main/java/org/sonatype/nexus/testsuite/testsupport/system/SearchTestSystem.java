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
package org.sonatype.nexus.testsuite.testsupport.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import org.awaitility.core.ConditionFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public interface SearchTestSystem
{

  /**
   * Waits for indexing to finish and makes sure any updates are available to search.
   *
   * General flow is component/asset events -> bulk index requests -> search indexing.
   */
  void waitForSearch();

  /**
   * Create the {@link ConditionFactory} which is suitable for a search requests.
   * @return the {@link ConditionFactory} object.
   */
  ConditionFactory waitForSearchResults();

  default void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final String repositoryName,
      final Optional<String> group,
      final Optional<String> name,
      final Optional<String> version)
  {
    assertThat(verifyComponentExistsByGAV(nexusSearchWebTarget, repositoryName, group, name, version), is(1));
  }

  default void verifyComponentDoesNotExist(
      final WebTarget nexusSearchWebTarget,
      final String repositoryName,
      final Optional<String> group,
      final Optional<String> name,
      final Optional<String> version)
  {
    assertThat(verifyComponentExistsByGAV(nexusSearchWebTarget, repositoryName, group, name, version), is(0));
  }

  default void verifyComponentDoesNotExist(final WebTarget nexusSearchWebTarget,
                                           final QueryParam queryParam){
    verifyComponentDoesNotExist(nexusSearchWebTarget, Lists.newArrayList(queryParam));
  }

  default void verifyComponentDoesNotExist(final WebTarget nexusSearchWebTarget,
                                           Collection<QueryParam> queryParams){
    List<Map<String, Object>> items = searchForComponentByParams(nexusSearchWebTarget, queryParams);
    assertThat(items.size(), is(0));
  }

  default void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final QueryParam queryParam)
  {
    verifyComponentExists(nexusSearchWebTarget, Lists.newArrayList(queryParam));
  }

  default void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final Collection<QueryParam> queryParams)
  {
    List<Map<String, Object>> items = searchForComponentByParams(nexusSearchWebTarget, queryParams);
    assertThat(items.size(), is(1));
  }

  default void verifyNumberOfComponentsAppearances(
      final WebTarget nexusSearchWebTarget,
      final QueryParam queryParam,
      final int numberOfAppearances)
  {
    verifyNumberOfComponentsAppearances(nexusSearchWebTarget, Lists.newArrayList(queryParam), numberOfAppearances);
  }

  default void verifyNumberOfComponentsAppearances(
      final WebTarget nexusSearchWebTarget,
      final Collection<QueryParam> queryParams,
      final int numberOfAppearances)
  {
    List<Map<String, Object>> items = searchForComponentByParams(nexusSearchWebTarget, queryParams);
    assertThat(items.size(), is(numberOfAppearances));
  }

  default int verifyComponentExistsByGAV(
      final WebTarget nexusSearchWebTarget,
      final String repositoryName,
      final Optional<String> group,
      final Optional<String> name,
      final Optional<String> version)
  {
    List<QueryParam> queryParams = new ArrayList<>();

    queryParams.add(new QueryParam("repository", repositoryName));
    group.map(g -> new QueryParam("group", g)).ifPresent(queryParams::add);
    name.map(n -> new QueryParam("name", n)).ifPresent(queryParams::add);
    version.map(v -> new QueryParam("version", v)).ifPresent(queryParams::add);

    List<Map<String, Object>> items = searchForComponentByParams(nexusSearchWebTarget, queryParams);
    return items.size();
  }

  class QueryParam
  {
    public final String name;
    public final String value;

    public QueryParam(final String name, final String value) {
      this.name = name;
      this.value = value;
    }
  }

  default List<Map<String, Object>> searchForComponentByParams(
      final WebTarget nexusSearchUrl,
      final Collection<QueryParam> queryParams)
  {
    waitForSearch();

    WebTarget request = nexusSearchUrl;
    for (QueryParam param : queryParams) {
      request = request.queryParam(param.name, param.value);
    }

    Response response = request
        .request()
        .buildGet()
        .invoke();

    Map<String, Object> map = response.readEntity(Map.class);
    return (List<Map<String, Object>>) map.get("items");
  }

  @SuppressWarnings("unchecked")
  default List<Map<String, Object>> searchByTag(
      final WebTarget nexusSearchUrl,
      final String repository,
      final String tag)
  {
    waitForSearch();

    Response response = nexusSearchUrl
        .queryParam("repository", repository)
        .queryParam("tag", tag)
        .request()
        .buildGet()
        .invoke();

    Map<String, Object> map = response.readEntity(Map.class);
    return (List<Map<String, Object>>) map.get("items");
  }
}
