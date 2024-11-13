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

import org.sonatype.nexus.content.maven.internal.index.MavenContentHostedIndexFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

public class MavenHostedRecipeTest
    extends MavenRecipeTestSupport
{
  @Mock
  private Repository mavenHostedRepository;

  @Mock
  private MavenContentHostedIndexFacet mavenHostedIndexFacet;

  private final Provider<MavenContentHostedIndexFacet> mavenHostedIndexFacetProvider = () -> mavenHostedIndexFacet;

  @Mock
  private PurgeUnusedSnapshotsFacet purgeUnusedSnapshotsFacet;

  private final Provider<PurgeUnusedSnapshotsFacet> mavenPurgeUnusedSnapshotsFacetProvider =
      () -> purgeUnusedSnapshotsFacet;

  private MavenHostedRecipe underTest;

  @Before
  public void setup() {
    underTest = new MavenHostedRecipe(new HostedType(), new Maven2Format());
    mockFacets(underTest);
    mockHandlers(underTest);
    underTest.setMavenIndexFacet(mavenHostedIndexFacetProvider);
    underTest.setMavenPurgeSnapshotsFacet(mavenPurgeUnusedSnapshotsFacetProvider);
  }

  @Test
  public void testExpectedFacetsAreAttached() throws Exception {
    underTest.apply(mavenHostedRepository);
    verify(mavenHostedRepository).attach(securityFacet);
    verify(mavenHostedRepository).attach(viewFacet);
    verify(mavenHostedRepository).attach(mavenMetadataRebuildFacet);
    verify(mavenHostedRepository).attach(mavenContentFacet);
    verify(mavenHostedRepository).attach(searchFacet);
    verify(mavenHostedRepository).attach(browseFacet);
    verify(mavenHostedRepository).attach(mavenArchetypeCatalogFacet);
    verify(mavenHostedRepository).attach(mavenHostedIndexFacet);
    verify(mavenHostedRepository).attach(mavenMaintenanceFacet);
    verify(mavenHostedRepository).attach(removeSnapshotsFacet);
    verify(mavenHostedRepository).attach(purgeUnusedSnapshotsFacet);
  }
}
