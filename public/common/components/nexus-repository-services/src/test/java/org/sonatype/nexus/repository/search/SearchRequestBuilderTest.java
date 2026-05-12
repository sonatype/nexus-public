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
package org.sonatype.nexus.repository.search;

import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.query.SearchFilter.FilterOperator;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SearchRequest.Builder} including new andFilter/orFilter methods.
 */
public class SearchRequestBuilderTest
{
  @Test
  public void testAndFilter_createsFilterWithAndOperator() {
    SearchRequest request = SearchRequest.builder()
        .andFilter("field", "value")
        .build();

    assertThat(request.getSearchFilters(), hasSize(1));

    SearchFilter filter = request.getSearchFilters().get(0);
    assertThat(filter.getProperty(), is("field"));
    assertThat(filter.getValue(), is("value"));
    assertTrue("Filter should have AND operator", filter.getOperator().isPresent());
    assertThat(filter.getOperator().get(), is(FilterOperator.AND));
  }

  @Test
  public void testOrFilter_createsFilterWithOrOperator() {
    SearchRequest request = SearchRequest.builder()
        .orFilter("field", "value")
        .build();

    assertThat(request.getSearchFilters(), hasSize(1));

    SearchFilter filter = request.getSearchFilters().get(0);
    assertThat(filter.getProperty(), is("field"));
    assertThat(filter.getValue(), is("value"));
    assertTrue("Filter should have OR operator", filter.getOperator().isPresent());
    assertThat(filter.getOperator().get(), is(FilterOperator.OR));
  }

  @Test
  public void testSearchFilter_withExplicitOperator() {
    SearchRequest request = SearchRequest.builder()
        .searchFilter("field", "value", FilterOperator.AND)
        .build();

    assertThat(request.getSearchFilters(), hasSize(1));

    SearchFilter filter = request.getSearchFilters().get(0);
    assertThat(filter.getProperty(), is("field"));
    assertThat(filter.getValue(), is("value"));
    assertTrue("Filter should have explicit operator", filter.getOperator().isPresent());
    assertThat(filter.getOperator().get(), is(FilterOperator.AND));
  }

  @Test
  public void testSearchFilter_backwardCompatibility() {
    // Test that existing searchFilter() without operator still works
    SearchRequest request = SearchRequest.builder()
        .searchFilter("field", "value")
        .build();

    assertThat(request.getSearchFilters(), hasSize(1));

    SearchFilter filter = request.getSearchFilters().get(0);
    assertThat(filter.getProperty(), is("field"));
    assertThat(filter.getValue(), is("value"));
    assertFalse("Filter should not have operator (backward compatible)", filter.getOperator().isPresent());
  }

  @Test
  public void testMixedFilters_explicitAndGlobal() {
    // Test mixing explicit operator filters with traditional filters
    SearchRequest request = SearchRequest.builder()
        .searchFilter("field1", "value1") // No operator (uses global conjunction flag)
        .andFilter("field2", "value2") // Explicit AND
        .searchFilter("field3", "value3") // No operator
        .build();

    assertThat(request.getSearchFilters(), hasSize(3));

    SearchFilter filter1 = request.getSearchFilters().get(0);
    assertFalse("First filter should not have operator", filter1.getOperator().isPresent());

    SearchFilter filter2 = request.getSearchFilters().get(1);
    assertTrue("Second filter should have AND operator", filter2.getOperator().isPresent());
    assertThat(filter2.getOperator().get(), is(FilterOperator.AND));

    SearchFilter filter3 = request.getSearchFilters().get(2);
    assertFalse("Third filter should not have operator", filter3.getOperator().isPresent());
  }

  @Test
  public void testMultipleAndFilters() {
    SearchRequest request = SearchRequest.builder()
        .andFilter("field1", "value1")
        .andFilter("field2", "value2")
        .andFilter("field3", "value3")
        .build();

    assertThat(request.getSearchFilters(), hasSize(3));

    for (SearchFilter filter : request.getSearchFilters()) {
      assertTrue("All filters should have AND operator", filter.getOperator().isPresent());
      assertThat(filter.getOperator().get(), is(FilterOperator.AND));
    }
  }

  @Test
  public void testMultipleOrFilters() {
    SearchRequest request = SearchRequest.builder()
        .orFilter("field1", "value1")
        .orFilter("field2", "value2")
        .build();

    assertThat(request.getSearchFilters(), hasSize(2));

    for (SearchFilter filter : request.getSearchFilters()) {
      assertTrue("All filters should have OR operator", filter.getOperator().isPresent());
      assertThat(filter.getOperator().get(), is(FilterOperator.OR));
    }
  }
}
