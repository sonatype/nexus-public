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
package org.sonatype.nexus.testsuite.testsupport.cleanup;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.testsuite.testsupport.utility.SearchTestHelper;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.collect.Iterables.size;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.StreamSupport.stream;
import static org.awaitility.Awaitility.await;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder.unrestricted;

@Named
@Singleton
@FeatureFlag(name = ORIENT_ENABLED)
public class OrientCleanupTestHelper
    implements CleanupTestHelper
{
  protected static final int ONE_HUNDRED_SECONDS = 100;

  protected static final int FIFTY_SECONDS = 50;

  protected static final int THREE_SECONDS = 3;

  @Inject
  private SearchTestHelper searchTestHelper;

  @Override
  public void waitForComponentsIndexed(final int count) {
    await()
        .untilAsserted(() -> assertThat(size(searchTestHelper.queryService().browse(searchAll())), is(count)));
  }

  @Override
  public void waitForLastDownloadSet(final int count) {
    await().untilAsserted(
        () -> assertThat(size(searchTestHelper.queryService().browse(lastDownloadSet())), is(count)));
  }

  @Override
  public void waitForMixedSearch() {
    BoolQueryBuilder query = boolQuery().must(matchAllQuery());

    query.filter(rangeQuery(LAST_DOWNLOADED_KEY).lte(nowMinusSeconds(FIFTY_SECONDS)))
        .filter(rangeQuery(LAST_BLOB_UPDATED_KEY).lte(nowMinusSeconds(THREE_SECONDS)))
        .must(matchQuery(IS_PRERELEASE_KEY, true));

    await().untilAsserted(
        () -> assertThat(size(searchTestHelper.queryService().browse(unrestricted(query))), greaterThan(0)));
  }

  @Override
  public void awaitLastBlobUpdatedTimePassed(final int time) {
    await().atMost(time + 3, SECONDS).untilAsserted(() ->
      assertThat(browse(lastBlobUpdatedQuery(time)).count(), equalTo(0L)));
  }

  @Override
  public void waitForIndex() {
    searchTestHelper.waitForSearch();
  }

  private Stream<SearchHit> browse(final QueryBuilder queryBuilder) {
    return stream(searchTestHelper.queryService().browse(queryBuilder).spliterator(), false);
  }

  private static BoolQueryBuilder lastBlobUpdatedQuery(final int time) {
    return searchAll().filter(rangeQuery(LAST_BLOB_UPDATED_KEY).gte(nowMinusSeconds(time)));
  }

  private static BoolQueryBuilder searchAll() {
    return boolQuery().must(matchAllQuery());
  }

  private static BoolQueryBuilder lastDownloadSet() {
    return createLastDownloadQuery(boolQuery(), "1");
  }

  private static String nowMinusSeconds(final String value) {
    return nowMinusSeconds(Integer.parseInt(value));
  }

  private static String nowMinusSeconds(final int value) {
    return format("now-%ds", value);
  }

  private static BoolQueryBuilder createLastDownloadQuery(final BoolQueryBuilder query, final String value) {
    BoolQueryBuilder neverDownloadedBuilder = boolQuery()
        .mustNot(existsQuery(LAST_DOWNLOADED_KEY))
        .filter(rangeQuery(LAST_BLOB_UPDATED_KEY).lte(nowMinusSeconds(value)));

    BoolQueryBuilder lastDownloadedBuilder = boolQuery()
        .must(rangeQuery(LAST_DOWNLOADED_KEY).lte(nowMinusSeconds(value)));

    BoolQueryBuilder filterBuilder = boolQuery();
    filterBuilder.should(lastDownloadedBuilder);
    filterBuilder.should(neverDownloadedBuilder);

    query.filter(filterBuilder);

    return query;
  }
}
