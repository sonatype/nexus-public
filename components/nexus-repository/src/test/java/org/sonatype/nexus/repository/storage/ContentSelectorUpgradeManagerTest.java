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
package org.sonatype.nexus.repository.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ContentSelectorUpgradeManagerTest
    extends TestSupport
{
  @InjectMocks
  private ContentSelectorUpgradeManager manager;

  @Mock
  private SelectorFactory selectorFactory;

  @Mock
  private SelectorManager selectorManager;

  @Test
  public void doStartConvertsValidJexlToCsel() throws Exception {
    SelectorConfiguration jexlSelector = createSelector("valid", JexlSelector.TYPE);

    when(selectorManager.browseJexl()).thenReturn(asList(jexlSelector));

    manager.doStart();

    verify(selectorManager).browseJexl();
    assertThat(jexlSelector.getType(), is(CselSelector.TYPE));
    verify(selectorManager).update(jexlSelector);
    verifyNoMoreInteractions(selectorManager);
  }

  @Test
  public void doStartSkipsInvalidCsel() throws Exception {
    SelectorConfiguration jexlSelector = createSelector("invalid", JexlSelector.TYPE);

    when(selectorManager.browseJexl()).thenReturn(asList(jexlSelector));

    String expression = jexlSelector.getAttributes().get("expression");
    doThrow(SelectorEvaluationException.class).when(selectorFactory).validateSelector(CselSelector.TYPE, expression);

    manager.doStart();

    verify(selectorManager).browseJexl();
    assertThat(jexlSelector.getType(), is(JexlSelector.TYPE));
    verifyNoMoreInteractions(selectorManager);
  }

  @Test
  public void doStartDoesNothing() throws Exception {
    when(selectorManager.browseJexl()).thenReturn(Collections.emptyList());

    manager.doStart();

    verify(selectorManager).browseJexl();
    verifyNoMoreInteractions(selectorManager);
  }

  private SelectorConfiguration createSelector(String expression, String type) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("expression", expression);

    SelectorConfiguration config = new SelectorConfiguration();
    config.setType(type);
    config.setAttributes(attributes);

    return config;
  }
}
