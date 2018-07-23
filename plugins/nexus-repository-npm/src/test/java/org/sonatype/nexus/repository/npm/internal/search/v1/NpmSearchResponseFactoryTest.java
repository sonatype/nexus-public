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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchHitExtractor;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponse;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseFactory;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseObject;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponsePackage;

import org.sonatype.goodies.testsupport.TestSupport;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NpmSearchResponseFactoryTest
    extends TestSupport
{
  @Mock
  NpmSearchHitExtractor npmSearchHitExtractor;

  NpmSearchResponseFactory underTest;

  @Before
  public void setUp() {
    underTest = new NpmSearchResponseFactory(npmSearchHitExtractor);
  }

  @Test
  public void testBuildEmptyResponse() {
    NpmSearchResponse response = underTest.buildEmptyResponse();

    assertThat(response.getObjects(), is(empty()));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(0));
  }

  @Test
  public void testBuildResponseWithResult() {
    NpmSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, true, true), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    NpmSearchResponseObject object = response.getObjects().get(0);
    NpmSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getName(), is("name-0"));
    assertThat(entry.getVersion(), is("version-0"));
    assertThat(entry.getDescription(), is("description-0"));
    assertThat(entry.getKeywords(), is(singletonList("keyword-0")));
    assertThat(entry.getPublisher().getUsername(), is("author-name-0"));
    assertThat(entry.getPublisher().getEmail(), is("author-email-0"));
    assertThat(entry.getDate(), not(nullValue()));

    assertThat(entry.getMaintainers(), hasSize(1));
    assertThat(entry.getMaintainers().get(0).getUsername(), is("author-name-0"));
    assertThat(entry.getMaintainers().get(0).getEmail(), is("author-email-0"));

    assertThat(entry.getLinks(), not(nullValue()));
    assertThat(entry.getLinks().getBugs(), is("bugs-url-0"));
    assertThat(entry.getLinks().getHomepage(), is("homepage-url-0"));
    assertThat(entry.getLinks().getRepository(), is("repository-url-0"));
    assertThat(entry.getLinks().getNpm(), is(nullValue()));

    assertThat(object.getSearchScore(), is(1.0));
    assertThat(object.getScore(), not(nullValue()));
    assertThat(object.getScore().getFinalScore(), is(0.0));
    assertThat(object.getScore().getDetail(), not(nullValue()));
    assertThat(object.getScore().getDetail().getQuality(), is(0.0));
    assertThat(object.getScore().getDetail().getMaintenance(), is(0.0));
    assertThat(object.getScore().getDetail().getPopularity(), is(0.0));
  }

  @Test
  public void testBuildResponseWithSize() {
    NpmSearchResponse response = underTest.buildResponseForResults(generateBuckets(5, true, true), 3, 0);

    assertThat(response.getObjects(), hasSize(3));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(3));

    assertThat(response.getObjects().get(0).getPackageEntry().getName(), is("name-0"));
    assertThat(response.getObjects().get(1).getPackageEntry().getName(), is("name-1"));
    assertThat(response.getObjects().get(2).getPackageEntry().getName(), is("name-2"));
  }

  @Test
  public void testBuildResponseWithFrom() {
    NpmSearchResponse response = underTest.buildResponseForResults(generateBuckets(10, true, true), 20, 5);

    assertThat(response.getObjects(), hasSize(5));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(5));

    assertThat(response.getObjects().get(0).getPackageEntry().getName(), is("name-5"));
    assertThat(response.getObjects().get(1).getPackageEntry().getName(), is("name-6"));
    assertThat(response.getObjects().get(2).getPackageEntry().getName(), is("name-7"));
    assertThat(response.getObjects().get(3).getPackageEntry().getName(), is("name-8"));
    assertThat(response.getObjects().get(4).getPackageEntry().getName(), is("name-9"));
  }

  @Test
  public void testBuildResponseWithoutAuthorInformation() {
    NpmSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, false, false), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    NpmSearchResponseObject object = response.getObjects().get(0);
    NpmSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getPublisher(), is(nullValue()));
    assertThat(entry.getMaintainers(), hasSize(0));
  }

  @Test
  public void testBuildResponseWithAuthorNameOnly() {
    NpmSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, true, false), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    NpmSearchResponseObject object = response.getObjects().get(0);
    NpmSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getPublisher().getUsername(), is("author-name-0"));
    assertThat(entry.getPublisher().getEmail(), is(nullValue()));

    assertThat(entry.getMaintainers(), hasSize(1));
    assertThat(entry.getMaintainers().get(0).getUsername(), is("author-name-0"));
    assertThat(entry.getMaintainers().get(0).getEmail(), is(nullValue()));
  }

  @Test
  public void testBuildResponseWithAuthorEmailOnly() {
    NpmSearchResponse response = underTest.buildResponseForResults(generateBuckets(1, false, true), 20, 0);

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));

    NpmSearchResponseObject object = response.getObjects().get(0);
    NpmSearchResponsePackage entry = object.getPackageEntry();

    assertThat(entry.getPublisher().getUsername(), is(nullValue()));
    assertThat(entry.getPublisher().getEmail(), is("author-email-0"));

    assertThat(entry.getMaintainers(), hasSize(1));
    assertThat(entry.getMaintainers().get(0).getUsername(), is(nullValue()));
    assertThat(entry.getMaintainers().get(0).getEmail(), is("author-email-0"));
  }

  @Test
  public void testBuildResponseFromObjects() {
    NpmSearchResponseObject object = new NpmSearchResponseObject();

    NpmSearchResponse response = underTest.buildResponseForObjects(singletonList(object));

    assertThat(response.getObjects(), hasSize(1));
    assertThat(response.getTime(), not(nullValue()));
    assertThat(response.getTotal(), is(1));
  }

  private List<Terms.Bucket> generateBuckets(final int count,
                                             final boolean includeAuthorName,
                                             final boolean includeAuthorEmail)
  {
    List<Bucket> buckets = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      Terms.Bucket bucket = mock(Terms.Bucket.class);
      Aggregations aggregations = mock(Aggregations.class);
      TopHits topHits = mock(TopHits.class);
      SearchHits searchHits = mock(SearchHits.class);
      SearchHit searchHit = mock(SearchHit.class);

      when(bucket.getAggregations()).thenReturn(aggregations);
      when(aggregations.get("versions")).thenReturn(topHits);
      when(topHits.getHits()).thenReturn(searchHits);
      when(searchHits.getAt(0)).thenReturn(searchHit);
      when(searchHit.getScore()).thenReturn(1.0F);

      if (includeAuthorEmail) {
        when(npmSearchHitExtractor.extractAuthorEmail(searchHit)).thenReturn("author-email-" + index);
      }
      if (includeAuthorName) {
        when(npmSearchHitExtractor.extractAuthorName(searchHit)).thenReturn("author-name-" + index);
      }
      when(npmSearchHitExtractor.extractBugsUrl(searchHit)).thenReturn("bugs-url-" + index);
      when(npmSearchHitExtractor.extractDescription(searchHit)).thenReturn("description-" + index);
      when(npmSearchHitExtractor.extractHomepage(searchHit)).thenReturn("homepage-url-" + index);
      when(npmSearchHitExtractor.extractRepositoryUrl(searchHit)).thenReturn("repository-url-" + index);
      when(npmSearchHitExtractor.extractKeywords(searchHit)).thenReturn(singletonList("keyword-" + index));
      when(npmSearchHitExtractor.extractLastModified(searchHit)).thenReturn(DateTime.now());
      when(npmSearchHitExtractor.extractName(searchHit)).thenReturn("name-" + index);
      when(npmSearchHitExtractor.extractVersion(searchHit)).thenReturn("version-" + index);

      buckets.add(bucket);
    }
    return buckets;
  }
}
