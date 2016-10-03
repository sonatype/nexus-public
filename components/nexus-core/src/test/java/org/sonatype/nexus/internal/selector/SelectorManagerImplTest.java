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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.VariableSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class SelectorManagerImplTest
    extends TestSupport
{
  @Mock
  private SelectorConfigurationStore store;

  @Mock
  private VariableSource variableSource;

  @Mock
  private SelectorConfiguration selectorConfiguration;

  private SelectorManagerImpl manager;

  @Before
  public void setUp() {
    this.manager = new SelectorManagerImpl(store);
  }

  @Test
  public void testEvaluate_True() throws Exception {
    SelectorConfiguration selectorConfiguration = getSelectorConfiguration("jexl", "true");
    assertThat(manager.evaluate(selectorConfiguration, variableSource), is(true));
  }

  @Test
  public void testEvaluate_False() throws Exception {
    SelectorConfiguration selectorConfiguration = getSelectorConfiguration("jexl", "false");
    assertThat(manager.evaluate(selectorConfiguration, variableSource), is(false));
  }

  @Test(expected = SelectorEvaluationException.class)
  public void testEvaluate_InvalidSelectorType() throws Exception {
    SelectorConfiguration selectorConfiguration = getSelectorConfiguration("junk", "");
    manager.evaluate(selectorConfiguration, variableSource);
  }

  private SelectorConfiguration getSelectorConfiguration(String type, String expression) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("expression", expression);
    when(selectorConfiguration.getAttributes()).thenReturn(attributes);
    when(selectorConfiguration.getType()).thenReturn(type);
    return selectorConfiguration;
  }
}
