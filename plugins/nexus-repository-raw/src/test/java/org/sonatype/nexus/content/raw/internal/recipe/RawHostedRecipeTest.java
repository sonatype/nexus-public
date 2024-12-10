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

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class RawHostedRecipeTest
    extends RawRecipeTestSupport
{
  @Mock
  private Repository rawHostedRepository;

  private RawHostedRecipe underTest;

  @Before
  public void setup() {
    underTest = new RawHostedRecipe(new HostedType(), new RawFormat());
    mockDependencies(underTest);
  }

  @Test
  public void testExpectedFacetsAreAttached() throws Exception {
    underTest.apply(rawHostedRepository);
    verify(rawHostedRepository).attach(securityFacet);
    verify(rawHostedRepository).attach(viewFacet);
    verify(rawHostedRepository).attach(contentFacet);
    verify(rawHostedRepository).attach(maintenanceFacet);
    verify(rawHostedRepository).attach(searchFacet);
    verify(rawHostedRepository).attach(browseFacet);
  }

}
