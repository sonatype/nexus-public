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
package org.sonatype.nexus.internal.selector;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorFilterBuilder;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.internal.DatastoreCselToSql;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class SelectorFilterBuilderImplTest
    extends TestSupport
{
  @Mock
  private SelectorConfigurationStore selectorConfigurationStore;

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private CacheHelper cacheHelper;

  @Mock
  private Time userCacheTimeout;

  private SelectorFilterBuilder underTest;

  @Before
  public void setup() {
    SelectorFactory selectorFactory = new SelectorFactory(constraintViolationFactory, new DatastoreCselToSql());
    SelectorManager selectorManager =
        new SelectorManagerImpl(selectorConfigurationStore, securitySystem, selectorFactory, cacheHelper,
            userCacheTimeout);
    underTest = new SelectorFilterBuilderImpl(selectorManager);
  }

  @Test
  public void testNoSelectors() {
    Map<String, Object> filterParameters = new HashMap<>();
    String filter = underTest.buildFilter("raw", "request_path", emptyList(), filterParameters);

    assertThat(filterParameters.keySet().size(), is(0));
    assertThat(filter, nullValue());
  }

  @Test
  public void testFormat() {
    SelectorConfiguration selector = createSelectorConfiguration("selector", "format == 'raw'");
    Map<String, Object> filterParameters = new HashMap<>();
    String filter = underTest.buildFilter("raw", "request_path", ImmutableList.of(selector), filterParameters);

    assertThat(filter, is("('raw' = #{filterParams.s0p0})"));
    assertThat(filterParameters.keySet().size(), is(1));
    assertThat(filterParameters.get("s0p0"), is("raw"));
  }

  @Test
  public void testPath() {
    SelectorConfiguration selector = createSelectorConfiguration("selector", "path =~ '/bar/.*'");
    Map<String, Object> filterParameters = new HashMap<>();
    String filter = underTest.buildFilter("raw", "request_path", ImmutableList.of(selector), filterParameters);

    assertThat(filter, is("(request_path ~ #{filterParams.s0p0})"));
    assertThat(filterParameters.keySet().size(), is(1));
    assertThat(filterParameters.get("s0p0"), is("^(/bar/.*)$"));
  }

  @Test
  public void testMultipleExpressions() {
    SelectorConfiguration selector = createSelectorConfiguration("selector", "path =~ '/bar/.*' or format == 'raw'");
    Map<String, Object> filterParameters = new HashMap<>();
    String filter = underTest.buildFilter("raw", "request_path", ImmutableList.of(selector), filterParameters);

    assertThat(filter, is("(request_path ~ #{filterParams.s0p0} or 'raw' = #{filterParams.s0p1})"));
    assertThat(filterParameters.keySet().size(), is(2));
    assertThat(filterParameters.get("s0p0"), is("^(/bar/.*)$"));
    assertThat(filterParameters.get("s0p1"), is("raw"));
  }

  @Test
  public void testMultipleSelectors() {
    SelectorConfiguration selector1 = createSelectorConfiguration("selector", "path =~ '/bar/.*' or format == 'raw'");
    SelectorConfiguration selector2 = createSelectorConfiguration("selector", "path == '/baz/'");

    Map<String, Object> filterParameters = new HashMap<>();
    String filter =
        underTest.buildFilter("raw", "request_path", ImmutableList.of(selector1, selector2), filterParameters);

    assertThat(filter,
        is("((request_path ~ #{filterParams.s0p0} or 'raw' = #{filterParams.s0p1}) or (request_path = #{filterParams.s1p0}))"));
    assertThat(filterParameters.size(), is(3));
    assertThat(filterParameters.get("s0p0"), is("^(/bar/.*)$"));
    assertThat(filterParameters.get("s0p1"), is("raw"));
    assertThat(filterParameters.get("s1p0"), is("/baz/"));
  }

  @Test
  public void testInvalidSelector() {
    // greater than not supported so the whole selector will be ignored
    SelectorConfiguration selector1 = createSelectorConfiguration("selector", "path > 5");
    SelectorConfiguration selector2 = createSelectorConfiguration("selector", "format == 'raw'");

    Map<String, Object> filterParameters = new HashMap<>();
    String filter =
        underTest.buildFilter("raw", "request_path", ImmutableList.of(selector1, selector2), filterParameters);

    // appends extra parenthesis because it expected more than one expression
    assertThat(filter, is("(('raw' = #{filterParams.s0p0}))"));
    assertThat(filterParameters.size(), is(1));
    assertThat(filterParameters.get("s0p0"), is("raw"));
  }

  // Jexl expressions are not put into the filter, they need to be checked in code
  @Test
  public void testJexl() {
    SelectorConfiguration selector = createSelectorConfiguration(JexlSelector.TYPE, "selector", "path.size() > 5");

    Map<String, Object> filterParameters = new HashMap<>();
    String filter = underTest.buildFilter("raw", "request_path", ImmutableList.of(selector), filterParameters);

    assertThat(filter, nullValue());
    assertThat(filterParameters.size(), is(0));
  }

  // Jexl expressions are not put into the filter, they need to be checked in code
  @Test
  public void testMultipleJexl() {
    SelectorConfiguration selector1 = createSelectorConfiguration(JexlSelector.TYPE, "selector1", "path.size() > 5");
    SelectorConfiguration selector2 = createSelectorConfiguration(JexlSelector.TYPE, "selector2", "format == 'raw'");

    Map<String, Object> filterParameters = new HashMap<>();
    String filter =
        underTest.buildFilter("raw", "request_path", ImmutableList.of(selector1, selector2), filterParameters);

    assertThat(filter, nullValue());
    assertThat(filterParameters.size(), is(0));
  }

  // Jexl expressions are not put into the filter, they need to be checked in code
  @Test
  public void testMixedTypes() {
    SelectorConfiguration selector1 = createSelectorConfiguration("selector1", "path == '/baz/'");
    SelectorConfiguration selector2 = createSelectorConfiguration(JexlSelector.TYPE, "selector2", "path.size() < 15");

    Map<String, Object> filterParameters = new HashMap<>();
    String filter =
        underTest.buildFilter("raw", "request_path", ImmutableList.of(selector1, selector2), filterParameters);

    assertThat(filter, is("(request_path = #{filterParams.s0p0})"));
    assertThat(filterParameters.size(), is(1));
    assertThat(filterParameters.get("s0p0"), is("/baz/"));
  }

  private SelectorConfiguration createSelectorConfiguration(final String name, final String expression) {
    return createSelectorConfiguration(CselSelector.TYPE, name, expression);
  }

  private SelectorConfiguration createSelectorConfiguration(
      final String type,
      final String name,
      final String expression)
  {
    SelectorConfigurationData result = new SelectorConfigurationData();

    result.setName(name);
    result.setType(type);
    Map<String, String> attributes = new HashMap<>();
    attributes.put(SelectorConfiguration.EXPRESSION, expression);
    result.setAttributes(attributes);

    return result;
  }
}
