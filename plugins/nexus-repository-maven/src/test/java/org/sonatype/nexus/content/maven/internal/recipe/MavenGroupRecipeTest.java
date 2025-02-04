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
package org.sonatype.nexus.content.maven.internal.recipe;

import javax.inject.Provider;

import org.sonatype.nexus.content.maven.internal.index.MavenContentGroupIndexFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.group.MavenGroupFacet;
import org.sonatype.nexus.repository.maven.internal.group.MergingGroupHandler;
import org.sonatype.nexus.repository.types.GroupType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class MavenGroupRecipeTest
    extends MavenRecipeTestSupport
{
  @Mock
  private Repository mavenGroupRepository;

  @Mock
  private MavenGroupFacet mavenGroupFacet;

  private final Provider<MavenGroupFacet> mavenGroupFacetProvider = () -> mavenGroupFacet;

  @Mock
  private MavenContentGroupIndexFacet mavenContentIndexFacet;

  private final Provider<MavenContentGroupIndexFacet> mavenContentIndexFacetProvider = () -> mavenContentIndexFacet;

  @Mock
  private PurgeUnusedSnapshotsFacet purgeUnusedSnapshotsFacet;

  private final Provider<PurgeUnusedSnapshotsFacet> purgeUnusedSnapshotsFacetProvider = () -> purgeUnusedSnapshotsFacet;

  @Mock
  private MergingGroupHandler mergingGroupHandler;

  @Mock
  private MavenContentIndexGroupHandler mavenContentIndexGroupHandler;

  @Mock
  private GroupHandler groupHandler;

  private MavenGroupRecipe underTest;

  @Before
  public void setup() {
    underTest = new MavenGroupRecipe(new GroupType(), new Maven2Format(), mavenContentIndexFacetProvider,
        mavenGroupFacetProvider, purgeUnusedSnapshotsFacetProvider, groupHandler, mergingGroupHandler,
        mavenContentIndexGroupHandler);
    mockFacets(underTest);
    mockHandlers(underTest);
  }

  @Test
  public void testExpectedFacetsAreAttached() throws Exception {
    underTest.apply(mavenGroupRepository);
    verify(mavenGroupRepository).attach(securityFacet);
    verify(mavenGroupRepository).attach(mavenGroupFacet);
    verify(mavenGroupRepository).attach(mavenContentFacet);
    verify(mavenGroupRepository).attach(mavenContentIndexFacet);
    verify(mavenGroupRepository).attach(browseFacet);
    verify(mavenGroupRepository).attach(purgeUnusedSnapshotsFacet);
    verify(mavenGroupRepository).attach(viewFacet);
    verify(mavenGroupRepository).attach(mavenMaintenanceFacet);
    verify(mavenGroupRepository).attach(removeSnapshotsFacet);
  }

}
