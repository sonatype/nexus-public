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
package org.sonatype.nexus.testsuite.testsupport.utility;

import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.index.SearchIndexService;
import org.sonatype.nexus.repository.search.query.SearchQueryService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@FeatureFlag(name = "nexus.orient.store.content")
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class OrientSearchTestHelper
    implements SearchTestHelper
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
  @Override
  public void waitForSearch() throws Exception {
    NexusPaxExamSupport.waitFor(eventManager::isCalmPeriod);
    indexService.flush(false); // no need for full fsync here
    NexusPaxExamSupport.waitFor(indexService::isCalmPeriod);
  }

  @Override
  public void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final Repository repository,
      final String name,
      final String version,
      final boolean exists) throws Exception
  {
    String repositoryName = repository.getName();
    List<Map<String, Object>> items = searchForComponent(nexusSearchWebTarget, repositoryName, name, version);
    assertThat(items.size(), is(exists ? 1 : 0));
  }

  @Override
  public SearchQueryService queryService() {
    return searchQueryService;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> searchForComponent(
      final WebTarget nexusSearchUrl, final String repository,
      final String artifactId,
      final String version)
      throws Exception
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
}
