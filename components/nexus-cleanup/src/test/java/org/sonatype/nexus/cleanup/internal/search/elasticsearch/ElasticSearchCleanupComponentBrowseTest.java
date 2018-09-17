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
package org.sonatype.nexus.cleanup.internal.search.elasticsearch;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections.IteratorUtils.toList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY;

public class ElasticSearchCleanupComponentBrowseTest
    extends TestSupport
{
  private static final String REPO_NAME = "repoName";

  private static final String COMPONENT_ID_1 = "id1";

  private static final String COMPONENT_ID_2 = "id2";

  private static final String NOW_MINUS_SECONDS = "now-%ss";

  @Mock
  private Repository repository;

  @Mock
  private Component component1, component2;

  @Mock
  private SearchHit searchHit1, searchHit2;

  @Mock
  private SearchService searchService;

  @Mock
  private StorageTx tx;

  private ElasticSearchCleanupComponentBrowse underTest;

  @Before
  public void setup() throws Exception {
    underTest = new ElasticSearchCleanupComponentBrowse(ImmutableMap.of(
        LAST_DOWNLOADED_KEY, new LastDownloadedCriteriaAppender(),
        LAST_BLOB_UPDATED_KEY, new LastBlobUpdatedCriteriaAppender(),
        IS_PRERELEASE_KEY, new PrereleaseCriteriaAppender()
    ), searchService);

    when(repository.getName()).thenReturn(REPO_NAME);

    when(searchHit1.getId()).thenReturn(COMPONENT_ID_1);
    when(searchHit2.getId()).thenReturn(COMPONENT_ID_2);

    UnitOfWork.beginBatch(tx);
  }
  
  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void browseLastBlobUpdatedPolicy() throws Exception {
    String numberOfSeconds = "10";
    Map<String, String> criteria = ImmutableMap.of(LAST_BLOB_UPDATED_KEY, numberOfSeconds);

    assertComponentsReturned(criteria);
    assertQuery(LAST_BLOB_UPDATED_KEY, timeQuery(LAST_BLOB_UPDATED_KEY, format(NOW_MINUS_SECONDS, numberOfSeconds)));
  }

  @Test
  public void browseLastDownloadedPolicy() throws Exception {
    String numberOfSeconds = "100";
    Map<String, String> criteria = ImmutableMap.of(LAST_DOWNLOADED_KEY, numberOfSeconds);

    assertComponentsReturned(criteria);
    assertQuery(LAST_DOWNLOADED_KEY, timeQuery(LAST_DOWNLOADED_KEY, format(NOW_MINUS_SECONDS, numberOfSeconds)));
  }

  @Test
  public void browsePrereleasePolicy() throws Exception {
    boolean isPrerelease = true;
    Map<String, String> criteria = ImmutableMap.of(IS_PRERELEASE_KEY, Boolean.toString(isPrerelease));

    assertComponentsReturned(criteria);
    assertQuery(IS_PRERELEASE_KEY, booleanQuery(IS_PRERELEASE_KEY, isPrerelease));
  }

  @Test
  public void browseByAndQuery() throws Exception {
    String lastBlobUpdatedSeconds = "5";
    String lastDownloadedSeconds = "20";
    boolean isPrerelease = false;
    
    Map<String, String> criteria = ImmutableMap.of(
        LAST_BLOB_UPDATED_KEY, lastBlobUpdatedSeconds,
        LAST_DOWNLOADED_KEY, lastDownloadedSeconds,
        IS_PRERELEASE_KEY, Boolean.toString(isPrerelease)
    );

    assertComponentsReturned(criteria);
    assertQuery(IS_PRERELEASE_KEY, andQuery(criteria));
  }

  @Test
  public void returnEmptyWhenNoCriteria() throws Exception {
    Map<String, String> criteria = emptyMap();

    CleanupPolicy lastBlobUpdatedPolicy = new CleanupPolicy();
    lastBlobUpdatedPolicy.setCriteria(criteria);

    when(searchService.browseUnrestrictedInRepos(any(), any())).thenReturn(ImmutableList.of(searchHit1, searchHit2));

    Iterable<EntityId> componentsIterable = underTest.browse(lastBlobUpdatedPolicy, repository);

    assertThat(componentsIterable.iterator().hasNext(), is(false));

    verify(searchService, never()).browseUnrestrictedInRepos(any(), any());
  }
  
  private void assertComponentsReturned(final Map<String, String> criteria) {
    CleanupPolicy lastBlobUpdatedPolicy = new CleanupPolicy();
    lastBlobUpdatedPolicy.setCriteria(criteria);

    when(searchService.browseUnrestrictedInRepos(any(), any())).thenReturn(ImmutableList.of(searchHit1, searchHit2));

    Iterable<EntityId> componentsIterable = underTest.browse(lastBlobUpdatedPolicy, repository);

    List<EntityId> components = toList(componentsIterable.iterator());

    assertThat(components.get(0), is(equalTo(new DetachedEntityId(COMPONENT_ID_1))));
    assertThat(components.get(1), is(equalTo(new DetachedEntityId(COMPONENT_ID_2))));
  }

  private void assertQuery(final String key, final String expectedQuery) {
    ArgumentCaptor<QueryBuilder> queryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);
    ArgumentCaptor<Collection> repoNameCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(searchService).browseUnrestrictedInRepos(queryBuilderCaptor.capture(), repoNameCaptor.capture());

    assertThat(queryBuilderCaptor.getValue().toString(), is(equalTo(expectedQuery)));
    assertThat(repoNameCaptor.getValue().iterator().next(), is(equalTo(REPO_NAME)));
  }

  private String timeQuery(final String matchOn, final String time) {
    return boolQuery().must(matchAllQuery()).filter(rangeQuery(matchOn).lte(time)).toString();
  }

  private String booleanQuery(final String matchOn, final boolean value) {
    return boolQuery().must(matchAllQuery()).must(matchQuery(matchOn, value)).toString();
  }

  private String andQuery(final Map<String, String> criteria) {
    return boolQuery().must(matchAllQuery())
        .filter(rangeQuery(LAST_BLOB_UPDATED_KEY).lte(format(NOW_MINUS_SECONDS, criteria.get(LAST_BLOB_UPDATED_KEY))))
        .filter(rangeQuery(LAST_DOWNLOADED_KEY).lte(format(NOW_MINUS_SECONDS, criteria.get(LAST_DOWNLOADED_KEY))))
        .must(matchQuery(IS_PRERELEASE_KEY, parseBoolean(criteria.get(IS_PRERELEASE_KEY)))).toString();
  }
}
