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
package org.sonatype.nexus.repository.security.internal;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class VariableResolverAdapterManagerImplTest
    extends TestSupport
{
  @Mock
  private VariableResolverAdapter specializedAdapter;

  @Mock
  private VariableResolverAdapter defaultAdapter;

  private VariableResolverAdapterManagerImpl manager;

  @Before
  public void setUp() {
    Map<String, VariableResolverAdapter> adaptersByFormat = new HashMap<>();
    adaptersByFormat.put("special", specializedAdapter);
    adaptersByFormat.put(SimpleVariableResolverAdapter.NAME, defaultAdapter);
    manager = new VariableResolverAdapterManagerImpl(adaptersByFormat);
  }

  @Test
  public void testGet_SpecializedAdapter() {
    assertThat(manager.get("special"), is(specializedAdapter));
  }

  @Test
  public void testGet_DefaultAdapter() {
    assertThat(manager.get("other-format"), is(defaultAdapter));
  }
}
