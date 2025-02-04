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
package org.sonatype.nexus.content.raw.internal.recipe;

import javax.inject.Provider;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.types.GroupType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class RawGroupRecipeTest
    extends RawRecipeTestSupport
{
  @Mock
  private Repository rawGroupRepository;

  @Mock
  private GroupHandler groupHandler;

  @Mock
  private GroupFacet groupFacet;

  private final Provider<GroupFacet> groupFacetProvider = () -> groupFacet;

  private RawGroupRecipe underTest;

  @Before
  public void setup() {
    underTest = new RawGroupRecipe(new GroupType(), new RawFormat(), groupFacetProvider, groupHandler);
    mockDependencies(underTest);
  }

  @Test
  public void testExpectedFacetsAreAttached() throws Exception {
    underTest.apply(rawGroupRepository);
    verify(rawGroupRepository).attach(securityFacet);
    verify(rawGroupRepository).attach(viewFacet);
    verify(rawGroupRepository).attach(groupFacet);
    verify(rawGroupRepository).attach(contentFacet);
    verify(rawGroupRepository).attach(browseFacet);
  }
}
