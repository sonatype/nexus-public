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
package org.sonatype.nexus.repository.search.query;

import org.sonatype.nexus.repository.search.query.SearchFilter.FilterOperator;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SearchFilter} including operator support.
 */
public class SearchFilterTest
{
  @Test
  public void testConstructor_withoutOperator() {
    SearchFilter filter = new SearchFilter("property", "value");

    assertThat(filter.getProperty(), is("property"));
    assertThat(filter.getValue(), is("value"));
    assertFalse("Operator should not be present", filter.getOperator().isPresent());
  }

  @Test
  public void testConstructor_withAndOperator() {
    SearchFilter filter = new SearchFilter("property", "value", FilterOperator.AND);

    assertThat(filter.getProperty(), is("property"));
    assertThat(filter.getValue(), is("value"));
    assertTrue("Operator should be present", filter.getOperator().isPresent());
    assertThat(filter.getOperator().get(), is(FilterOperator.AND));
  }

  @Test
  public void testConstructor_withOrOperator() {
    SearchFilter filter = new SearchFilter("property", "value", FilterOperator.OR);

    assertThat(filter.getProperty(), is("property"));
    assertThat(filter.getValue(), is("value"));
    assertTrue("Operator should be present", filter.getOperator().isPresent());
    assertThat(filter.getOperator().get(), is(FilterOperator.OR));
  }

  @Test
  public void testEquals_withSameOperator() {
    SearchFilter filter1 = new SearchFilter("property", "value", FilterOperator.AND);
    SearchFilter filter2 = new SearchFilter("property", "value", FilterOperator.AND);

    assertThat(filter1, equalTo(filter2));
  }

  @Test
  public void testEquals_withDifferentOperator() {
    SearchFilter filter1 = new SearchFilter("property", "value", FilterOperator.AND);
    SearchFilter filter2 = new SearchFilter("property", "value", FilterOperator.OR);

    assertThat(filter1, not(equalTo(filter2)));
  }

  @Test
  public void testEquals_withoutOperator() {
    SearchFilter filter1 = new SearchFilter("property", "value");
    SearchFilter filter2 = new SearchFilter("property", "value");

    assertThat(filter1, equalTo(filter2));
  }

  @Test
  public void testEquals_mixedOperatorPresence() {
    SearchFilter filter1 = new SearchFilter("property", "value");
    SearchFilter filter2 = new SearchFilter("property", "value", FilterOperator.AND);

    assertThat(filter1, not(equalTo(filter2)));
  }

  @Test
  public void testHashCode_withOperator() {
    SearchFilter filter1 = new SearchFilter("property", "value", FilterOperator.AND);
    SearchFilter filter2 = new SearchFilter("property", "value", FilterOperator.AND);

    assertThat(filter1.hashCode(), is(filter2.hashCode()));
  }

  @Test
  public void testHashCode_withDifferentOperator() {
    SearchFilter filter1 = new SearchFilter("property", "value", FilterOperator.AND);
    SearchFilter filter2 = new SearchFilter("property", "value", FilterOperator.OR);

    assertThat(filter1.hashCode(), not(is(filter2.hashCode())));
  }

  @Test
  public void testToString_withOperator() {
    SearchFilter filter = new SearchFilter("property", "value", FilterOperator.AND);
    String toString = filter.toString();

    assertTrue("toString should contain property", toString.contains("property"));
    assertTrue("toString should contain value", toString.contains("value"));
    assertTrue("toString should contain operator", toString.contains("AND"));
  }

  @Test
  public void testToString_withoutOperator() {
    SearchFilter filter = new SearchFilter("property", "value");
    String toString = filter.toString();

    assertTrue("toString should contain property", toString.contains("property"));
    assertTrue("toString should contain value", toString.contains("value"));
    assertTrue("toString should contain null operator", toString.contains("operator=null"));
  }
}
