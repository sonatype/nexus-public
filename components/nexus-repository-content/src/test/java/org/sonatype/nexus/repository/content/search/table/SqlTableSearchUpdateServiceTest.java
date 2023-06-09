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
package org.sonatype.nexus.repository.content.search.table;

import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.index.SearchUpdateService.SEARCH_INDEX_OUTDATED;

/**
 * Tests for {@link SqlTableSearchUpdateService}
 */
public class SqlTableSearchUpdateServiceTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private SearchTableStore searchTableStore;

  private NestedAttributesMap attributes = new NestedAttributesMap("root", new HashMap<>());

  private SqlTableSearchUpdateService underTest;

  @Before
  public void setup() {
    when(repository.getType()).thenReturn(new HostedType());
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.attributes()).thenReturn(attributes);

    underTest = new SqlTableSearchUpdateService(searchTableStore);
  }

  @Test
  public void needsReindex_groupRepositoryAlwaysFalse() {
    when(repository.getType()).thenReturn(new GroupType());
    assertFalse(underTest.needsReindex(repository));
  }

  @Test
  public void needsReindex_noIndexesInStore() {
    when(repository.getName()).thenReturn("test-repo");
    when(searchTableStore.repositoryNeedsReindex(repository.getName())).thenReturn(true);

    assertTrue(underTest.needsReindex(repository));
  }

  @Test
  public void needsReindex_indexesExistInStore() {
    when(repository.getName()).thenReturn("test-repo");
    when(searchTableStore.repositoryNeedsReindex(repository.getName())).thenReturn(false);

    assertFalse(underTest.needsReindex(repository));
  }

  @Test
  public void needsReindex_byFlag() {
    attributes.set(SEARCH_INDEX_OUTDATED, true);

    assertTrue(underTest.needsReindex(repository));
  }
}
