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
package org.sonatype.nexus.repository.search.index;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.SearchUtils;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.query.SearchFilter.FilterOperator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SearchUtilsTest
{
  static final String VALID_SHA1_ATTRIBUTE_NAME = "assets.attributes.checksum.sha1";

  static final String INVALID_SHA1_ATTRIBUTE_NAME = "asset.attributes.checksum.sha1";

  static final String SHA1_ALIAS = "sha1";

  @Mock
  RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  SearchUtils underTest;

  private MockedStatic<QualifierUtil> mockedStatic;

  @Before
  public void setup() {
    mockedStatic = mockStatic(QualifierUtil.class);
    when(QualifierUtil.buildQualifierBeanMap(anyList())).thenReturn(Map.of(
        "default", new SearchMappingTest()));
    underTest = new SearchUtils(repositoryManagerRESTAdapter, List.of());
  }

  @After
  public void tearDown() {
    mockedStatic.close();
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
  public void testGetRepository() {
    String repositoryId = "repositoryId";
    underTest.getRepository(repositoryId);

    verify(repositoryManagerRESTAdapter).getReadableRepository(repositoryId);
  }

  @Test
  public void testGetComponentSearchFilters_ExcludesAssetParameters() {
    // Setup UriInfo with both component-level and asset-level parameters
    UriInfo uriInfo = mock(UriInfo.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

    // Component-level parameters (should be included)
    queryParams.add("repository", "maven-central");
    queryParams.add("name", "junit");

    // Asset-level parameters (should be excluded)
    queryParams.add("maven.extension", "jar");
    queryParams.add("maven.classifier", "sources");
    queryParams.add(SHA1_ALIAS, "abc123");

    when(uriInfo.getQueryParameters()).thenReturn(queryParams);

    // Get all filters (includes asset parameters)
    List<SearchFilter> allFilters = underTest.getSearchFilters(uriInfo);

    // Get component filters (excludes asset parameters)
    List<SearchFilter> componentFilters = underTest.getComponentSearchFilters(uriInfo);

    // Verify that component filters exclude asset-level parameters
    // Note: allFilters now includes 6 items (5 original + 1 auto-injected format filter)
    assertThat("All filters should include asset parameters and format", allFilters, hasSize(6));
    // Component filters exclude asset parameters but include auto-injected format filter
    assertThat("Component filters should exclude asset parameters", componentFilters, hasSize(3));

    // Verify only component-level filters remain (order is not guaranteed)
    List<String> componentProperties = componentFilters.stream()
        .map(SearchFilter::getProperty)
        .collect(toList());
    assertTrue("Component filters should contain 'repository'", componentProperties.contains("repository"));
    assertTrue("Component filters should contain 'name'", componentProperties.contains("name"));
    assertTrue("Component filters should contain auto-injected 'format'", componentProperties.contains("format"));
  }

  @Test
  public void testAutoInjectFormatFilterWhenFormatSpecificFieldUsed() {
    // Setup UriInfo with a format-specific field but no format parameter
    UriInfo uriInfo = mock(UriInfo.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.add("swift.scope", "Alamofire");
    queryParams.add("name", "test");

    when(uriInfo.getQueryParameters()).thenReturn(queryParams);

    // Get component filters
    List<SearchFilter> filters = underTest.getComponentSearchFilters(uriInfo);

    // Verify format filter was auto-injected
    List<String> properties = filters.stream()
        .map(SearchFilter::getProperty)
        .collect(toList());
    assertTrue("Format filter should be auto-injected", properties.contains("format"));

    // Verify the format value is correct
    SearchFilter formatFilter = filters.stream()
        .filter(f -> "format".equals(f.getProperty()))
        .findFirst()
        .orElse(null);
    assertThat("Format filter should exist", formatFilter != null);
    assertThat("Format value should be 'swift'", formatFilter.getValue().equals("swift"));
  }

  @Test
  public void testAutoInjectFormatFilterDoesNotDuplicateWhenFormatAlreadyExists() {
    // Setup UriInfo with both format-specific field and format parameter
    UriInfo uriInfo = mock(UriInfo.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.add("swift.scope", "Alamofire");
    queryParams.add("format", "swift");

    when(uriInfo.getQueryParameters()).thenReturn(queryParams);

    // Get component filters
    List<SearchFilter> filters = underTest.getComponentSearchFilters(uriInfo);

    // Count format filters
    long formatFilterCount = filters.stream()
        .filter(f -> "format".equals(f.getProperty()))
        .count();

    assertThat("Should only have one format filter", formatFilterCount == 1);
  }

  @Test
  public void testAutoInjectFormatFilterNotInjectedWhenNoFormatSpecificField() {
    // Setup UriInfo with only generic fields
    UriInfo uriInfo = mock(UriInfo.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.add("name", "test");
    queryParams.add("repository", "maven-central");

    when(uriInfo.getQueryParameters()).thenReturn(queryParams);

    // Get component filters
    List<SearchFilter> filters = underTest.getComponentSearchFilters(uriInfo);

    // Verify format filter was NOT auto-injected
    List<String> properties = filters.stream()
        .map(SearchFilter::getProperty)
        .collect(toList());
    assertFalse("Format filter should not be auto-injected", properties.contains("format"));
  }

  @Test
  public void testAutoInjectFormatFilterWorksWithMultipleFormats() {
    // Test with Maven-specific field
    UriInfo uriInfo = mock(UriInfo.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.add("maven.groupId", "org.junit");

    when(uriInfo.getQueryParameters()).thenReturn(queryParams);

    List<SearchFilter> filters = underTest.getComponentSearchFilters(uriInfo);

    SearchFilter formatFilter = filters.stream()
        .filter(f -> "format".equals(f.getProperty()))
        .findFirst()
        .orElse(null);
    assertThat("Format filter should exist", formatFilter != null);
    assertThat("Format value should be 'maven'", formatFilter.getValue().equals("maven2"));
  }

  @Test
  public void testMultipleFormatParamsUseExplicitOr() {
    UriInfo uriInfo = mock(UriInfo.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
    queryParams.add("format", "maven2");
    queryParams.add("format", "npm");

    when(uriInfo.getQueryParameters()).thenReturn(queryParams);

    List<SearchFilter> filters = underTest.getSearchFilters(uriInfo);

    List<SearchFilter> formatFilters = filters.stream()
        .filter(f -> "format".equals(f.getProperty()))
        .collect(toList());
    assertThat("Should have two format filters", formatFilters, hasSize(2));
    for (SearchFilter f : formatFilters) {
      assertTrue("Format filter should use OR operator", f.getOperator().orElse(null) == FilterOperator.OR);
    }
  }

  private static class SearchMappingTest
      implements SearchMappings
  {
    @Override
    public Iterable<SearchMapping> get() {
      return List.of(
          new SearchMapping(SHA1_ALIAS, VALID_SHA1_ATTRIBUTE_NAME, "", SearchField.SHA1),
          new SearchMapping("maven.extension", "assets.attributes.maven2.extension",
              "Maven extension", SearchField.FORMAT_FIELD_2),
          new SearchMapping("maven.classifier", "assets.attributes.maven2.classifier",
              "Maven classifier", SearchField.FORMAT_FIELD_3),
          new SearchMapping("swift.scope", "attributes.swift.scope",
              "Swift scope", SearchField.FORMAT_FIELD_1),
          new SearchMapping("maven.groupId", "attributes.maven2.groupId",
              "Maven groupId", SearchField.NAMESPACE));
    }
  }
}
