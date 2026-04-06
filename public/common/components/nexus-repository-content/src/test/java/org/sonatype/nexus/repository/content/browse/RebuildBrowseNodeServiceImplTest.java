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
import java.util.function.Consumer;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RebuildBrowseNodeServiceImplTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private BrowseFacet browseFacet;

  private RebuildBrowseNodeServiceImpl underTest;

  @Before
  public void setUp() {
    underTest = new RebuildBrowseNodeServiceImpl();
  }

  @Test
  public void testRebuildDelegatesToBrowseFacet() throws Exception {
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    Consumer<String> progressUpdater = msg -> {
    };
    underTest.rebuild(repository, progressUpdater);

    verify(browseFacet).rebuildBrowseNodes(progressUpdater);
  }

  @Test
  public void testRebuildDoesNothingWhenNoBrowseFacet() throws Exception {
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    underTest.rebuild(repository, msg -> {
    });

    verify(browseFacet, never()).rebuildBrowseNodes(any());
  }
}
