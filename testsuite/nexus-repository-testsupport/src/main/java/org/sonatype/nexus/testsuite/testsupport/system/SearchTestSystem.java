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
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.index.SearchIndexService;
import org.sonatype.nexus.repository.search.query.SearchQueryService;

import com.google.common.collect.Lists;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Named
@Singleton
public class SearchTestSystem
{
  @Inject
  public SearchIndexService indexService;

  @Inject
  public SearchQueryService searchQueryService;

  @Inject
  public EventManager eventManager;

  /**
   * Waits for indexing to finish and makes sure any updates are available to search.
   *
   * General flow is component/asset events -> bulk index requests -> search indexing.
   */
  public void waitForSearch() {
    await().atMost(30, TimeUnit.SECONDS).until(eventManager::isCalmPeriod);
    indexService.flush(false); // no need for full fsync here
    await().atMost(30, TimeUnit.SECONDS).until(indexService::isCalmPeriod);
  }

  public void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final Repository repository,
      final String name,
      final String version,
      final boolean exists)
  {
    String repositoryName = repository.getName();
    List<Map<String, Object>> items = searchForComponent(nexusSearchWebTarget, repositoryName, name, version);
    assertThat(items.size(), is(exists ? 1 : 0));
  }

  public void verifyComponentDoesNotExists(
      final WebTarget nexusSearchWebTarget,
      final Repository repository,
      final Optional<String> group,
      final Optional<String> name,
      final Optional<String> version)
  {
    assertThat(verifyComponentExistsByGAV(nexusSearchWebTarget, repository, group, name, version), is(0));
  }

  public void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final Repository repository,
      final Optional<String> group,
      final Optional<String> name,
      final Optional<String> version)
  {
    assertThat(verifyComponentExistsByGAV(nexusSearchWebTarget, repository, group, name, version), is(1));
  }

  public void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final QueryParam queryParam)
  {
    verifyComponentExists(nexusSearchWebTarget, Lists.newArrayList(queryParam));
  }

  public void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final Collection<QueryParam> queryParams)
  {
    List<Map<String, Object>> items = searchForComponentByParams(nexusSearchWebTarget, queryParams);
    assertThat(items.size(), is(1));
  }

  private int verifyComponentExistsByGAV(
      final WebTarget nexusSearchWebTarget,
      final Repository repository,
      final Optional<String> group,
      final Optional<String> name,
      final Optional<String> version)
  {
    List<QueryParam> queryParams = new ArrayList<>();

    queryParams.add(new QueryParam("repository", repository.getName()));
    group.map(g -> new QueryParam("group", g)).ifPresent(queryParams::add);
    name.map(n -> new QueryParam("name", n)).ifPresent(queryParams::add);
    version.map(v -> new QueryParam("version", v)).ifPresent(queryParams::add);

    List<Map<String, Object>> items = searchForComponentByParams(nexusSearchWebTarget, queryParams);
    return items.size();
  }

  public static class QueryParam {
    public final String name;
    public final String value;

    public QueryParam(final String name, final String value) {
      this.name = name;
      this.value = value;
    }
  }

  public SearchQueryService queryService() {
    return searchQueryService;
  }

  private List<Map<String, Object>> searchForComponentByParams(
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
  private List<Map<String, Object>> searchForComponent(
      final WebTarget nexusSearchUrl,
      final String repository,
      final String artifactId,
      final String version)
  {
    waitForSearch();

    Response response = nexusSearchUrl
        .queryParam("repository", repository)
        .queryParam("maven.artifactId", artifactId)
        .queryParam("maven.baseVersion", version)
        .request()
        .buildGet()
        .invoke();

    Map<String, Object> map = response.readEntity(Map.class);
    return (List<Map<String, Object>>) map.get("items");
  }

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> searchByTag(
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
