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
package org.sonatype.nexus.repository.content.browse;

import java.util.Optional;

import org.sonatype.nexus.repository.Repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TrimBrowseNodesTaskTest
{
  @Mock
  private Repository repository;

  @Mock
  private BrowseFacet browseFacet;

  private TrimBrowseNodesTask underTest;

  @Before
  public void setUp() {
    underTest = new TrimBrowseNodesTask();
  }

  @Test
  public void testAppliesToRepositoryWithBrowseFacet() {
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    assertThat(underTest.appliesTo(repository), is(true));
  }

  @Test
  public void testDoesNotApplyToRepositoryWithoutBrowseFacet() {
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    assertThat(underTest.appliesTo(repository), is(false));
  }

  @Test
  public void testDoesNotApplyToNullRepository() {
    assertThat(underTest.appliesTo(null), is(false));
  }
}
