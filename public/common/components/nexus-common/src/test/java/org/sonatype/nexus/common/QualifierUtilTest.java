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
package org.sonatype.nexus.common;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.app.WebFilterPriority;

import org.junit.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link QualifierUtil}, focusing on @Order-based sorting behavior.
 */
public class QualifierUtilTest
{
  @Test
  public void compareByOrder_sortsAnnotatedBeforeUnannotated() {
    HighPrecedence high = new HighPrecedence();
    NoOrder noOrder = new NoOrder();

    assertThat(QualifierUtil.compareByOrder(high, noOrder) < 0, is(true));
    assertThat(QualifierUtil.compareByOrder(noOrder, high) > 0, is(true));
  }

  @Test
  public void compareByOrder_unannotatedDefaultsToZero() {
    NoOrder noOrder = new NoOrder();
    MidPrecedence mid = new MidPrecedence();

    // Both should be at 0, so compare should return 0
    assertThat(QualifierUtil.compareByOrder(noOrder, mid), is(0));
  }

  @Test
  public void compareByOrder_unannotatedDefaultsToDefaultPrecedence() {
    NoOrder a = new NoOrder();
    ExplicitDefaultPrecedence b = new ExplicitDefaultPrecedence();

    // No @Order should be equivalent to @Order(DEFAULT_PRECEDENCE)
    assertThat(QualifierUtil.compareByOrder(a, b), is(0));
  }

  @Test
  public void compareByOrder_unannotatedComesBeforeLowestPrecedence() {
    NoOrder noOrder = new NoOrder();
    LowestPrecedence lowest = new LowestPrecedence();

    // No @Order (DEFAULT_PRECEDENCE = 0) should sort before LOWEST_PRECEDENCE
    assertThat(QualifierUtil.compareByOrder(noOrder, lowest) < 0, is(true));
  }

  @Test
  public void compareByOrder_sortsMultiplePrioritiesCorrectly() {
    HighPrecedence high = new HighPrecedence();
    MidPrecedence mid = new MidPrecedence();
    NoOrder noOrder = new NoOrder();
    LowestPrecedence lowest = new LowestPrecedence();

    List<Object> items = Arrays.asList(lowest, noOrder, high, mid);
    items.sort(QualifierUtil::compareByOrder);

    // high (HIGHEST_PRECEDENCE) -> noOrder (0) = mid (0), stable sort preserves input order -> lowest
    assertThat(items, contains(high, noOrder, mid, lowest));
  }

  @Test
  public void compareByOrder_webFilterPriorityOrdering() {
    // Simulates the actual filter ordering scenario
    WebPriority web = new WebPriority();
    AuthPriority auth = new AuthPriority();
    NoOrder unannotated = new NoOrder();
    WebResourcesMinus100 previewUi = new WebResourcesMinus100();
    LowestPrecedence lowest = new LowestPrecedence();

    List<Object> items = Arrays.asList(lowest, previewUi, unannotated, auth, web);
    items.sort(QualifierUtil::compareByOrder);

    // WEB (-1879048192) -> unannotated (0) = AUTH (0) stable sort preserves input order -> WEB_RESOURCES-100 -> LOWEST
    assertThat(items, contains(web, unannotated, auth, previewUi, lowest));
  }

  @Test
  public void buildQualifierBeanMap_preservesOrderAnnotationOrdering() {
    HighPrecedence high = new HighPrecedence();
    NoOrder noOrder = new NoOrder();
    LowestPrecedence lowest = new LowestPrecedence();

    Map<String, Object> map = QualifierUtil.buildQualifierBeanMap(Arrays.asList(lowest, noOrder, high));

    // Map iteration order should follow @Order sorting
    List<Object> values = List.copyOf(map.values());
    assertThat(values, contains(high, noOrder, lowest));
  }

  @Test
  public void buildQualifierBeanMap_returnsNullForNullInput() {
    assertThat(QualifierUtil.buildQualifierBeanMap(null) == null, is(true));
  }

  // -- test components --

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class HighPrecedence
  {
  }

  @Order(0)
  static class MidPrecedence
  {
  }

  @Order(PrecedenceConstants.DEFAULT_PRECEDENCE)
  static class ExplicitDefaultPrecedence
  {
  }

  @Order(WebFilterPriority.WEB)
  static class WebPriority
  {
  }

  @Order(WebFilterPriority.AUTHENTICATION)
  static class AuthPriority
  {
  }

  @Order(WebFilterPriority.WEB_RESOURCES - 100)
  static class WebResourcesMinus100
  {
  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  static class LowestPrecedence
  {
  }

  static class NoOrder
  {
  }
}
