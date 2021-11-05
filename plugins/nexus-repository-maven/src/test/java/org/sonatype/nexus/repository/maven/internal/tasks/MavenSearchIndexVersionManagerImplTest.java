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
package org.sonatype.nexus.repository.maven.internal.tasks;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class MavenSearchIndexVersionManagerImplTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private MavenContentFacet contentFacet;

  private final NestedAttributesMap attributes = new NestedAttributesMap();

  private final MavenSearchIndexVersionManagerImpl underTest = new MavenSearchIndexVersionManagerImpl();

  @Before
  public void setup() {
    when(repository.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(repository.getType()).thenReturn(new HostedType());
    when(contentFacet.attributes()).thenReturn(attributes);
  }

  @Test
  public void needsReindex_groupRepositoryAlwaysFalse() {
    when(repository.getType()).thenReturn(new GroupType());
    assertFalse(underTest.needsReindex(repository));
  }

  @Test
  public void needsReindex_noFlag() {
    assertFalse(underTest.needsReindex(repository));
  }

  @Test
  public void needsReindex_invalidFlag() {
    attributes.set("maven_search_index_outdated", "hello");
    assertFalse(underTest.needsReindex(repository));
  }

  @Test
  public void needsReindex_falseFlag() {
    attributes.set("maven_search_index_outdated", false);
    assertFalse(underTest.needsReindex(repository));
  }

  @Test
  public void needsReindex_trueFlag() {
    attributes.set("maven_search_index_outdated", true);
    assertTrue(underTest.needsReindex(repository));
  }
}
