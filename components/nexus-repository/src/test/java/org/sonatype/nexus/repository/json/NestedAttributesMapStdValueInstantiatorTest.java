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
package org.sonatype.nexus.repository.json;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

public class NestedAttributesMapStdValueInstantiatorTest
    extends TestSupport
{
  @Mock
  private StdValueInstantiator stdValueInstantiator;

  @Mock
  private NestedAttributesMap nestedAttributesMap;

  @Mock
  private DeserializationContext context;

  private Map<String, Object> map = newHashMap();

  private NestedAttributesMapStdValueInstantiator underTest;

  @Before
  public void setUp() {
    underTest = new NestedAttributesMapStdValueInstantiator(stdValueInstantiator, nestedAttributesMap);
    when(nestedAttributesMap.backing()).thenReturn(map);
  }

  @Test
  public void assure_CreateUsingDefault_Returns_backing() {
    assertThat(underTest.createUsingDefault(context), equalTo(map));
  }
}
