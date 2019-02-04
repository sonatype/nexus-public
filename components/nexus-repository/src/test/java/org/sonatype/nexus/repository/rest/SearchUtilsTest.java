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
package org.sonatype.nexus.repository.rest;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.internal.resources.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.search.DefaultSearchContribution;
import org.sonatype.nexus.repository.search.KeywordSearchContribution;
import org.sonatype.nexus.repository.search.SearchContribution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchUtilsTest
    extends TestSupport
{
  static final String VALID_SHA1_ATTRIBUTE_NAME = "assets.attributes.checksum.sha1";

  static final String INVALID_SHA1_ATTRIBUTE_NAME = "asset.attributes.checksum.sha1";

  static final String SHA1_ALIAS = "sha1";

  private static final String QUERY_STRING = "continuationToken=1&parameter=test&wait=false";

  private static final String URI = "http://localhost";

  private static final String CONTEXT_PATH = "/";

  @Mock
  RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  SearchUtils underTest;

  @Before
  public void setup() {

    Map<String, SearchMappings> searchMappings = ImmutableMap.of(
        "default", () -> ImmutableList.of(
            new SearchMapping(SHA1_ALIAS, VALID_SHA1_ATTRIBUTE_NAME, "")
        )
    );

    Map<String, SearchContribution> searchContributions = new HashMap<>();
    searchContributions.put(DefaultSearchContribution.NAME, new DefaultSearchContribution());
    searchContributions.put(KeywordSearchContribution.NAME, new KeywordSearchContribution());
    underTest = new SearchUtils(repositoryManagerRESTAdapter, searchMappings, searchContributions);
  }

  @Test
  public void testIsAssetSearchParam_MappedAlias_Sha1() {
    assertTrue(underTest.isAssetSearchParam(SHA1_ALIAS));
  }

  @Test
  public void testIsAssetSearchParam_UnMapped_FullAssetAttributeName() {
    assertTrue(underTest.isAssetSearchParam(VALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsAssetSearchParam_UnMappedAlias_Returns_False() {
    assertFalse(underTest.isAssetSearchParam("new.asset"));
  }

  @Test
  public void testIsAssetSearchParam_Invalid_Full_AssetAttribute() {
    assertFalse(underTest.isAssetSearchParam(INVALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName() {
    assertTrue(underTest.isFullAssetAttributeName(VALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName_Invalid_LongForm_Attribute_ReturnsFalse() {
    assertFalse(underTest.isFullAssetAttributeName(INVALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName_MappedAlias_ReturnsFalse() {
    assertFalse(underTest.isFullAssetAttributeName(SHA1_ALIAS));
  }

  @Test
  public void buildQueryRemoveContinuationTokenByDefault() {
    ResteasyUriInfo uriInfo = new ResteasyUriInfo(URI, QUERY_STRING, CONTEXT_PATH);
    String query = underTest.buildQuery(uriInfo).toString();

    assertQueryParameters(query, containsString("wait"));
  }

  @Test
  public void buildQueryRemoveSelectedParametersIncludingDefault() {
    ResteasyUriInfo uriInfo = new ResteasyUriInfo(URI, QUERY_STRING, CONTEXT_PATH);
    String query = underTest.buildQuery(uriInfo, singletonList("wait")).toString();

    assertQueryParameters(query, not(containsString("wait")));
  }

  private void assertQueryParameters(final String query, final Matcher<String> parameterMatcher) {
    assertThat(query, containsString("parameter"));
    assertThat(query, parameterMatcher);
    assertThat(query, not(containsString("continuationToken")));
  }
}
