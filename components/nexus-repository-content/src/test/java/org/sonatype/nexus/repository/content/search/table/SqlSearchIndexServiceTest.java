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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqlSearchIndexServiceTest
    extends TestSupport
{
  @Mock
  private SearchTableDataProducer searchTableDataProducer;

  @Mock
  private SearchTableStore searchStore;

  @Mock
  private Repository repository;

  private SqlSearchIndexService underTest;

  @Before
  public void setup() {
    underTest = new SqlSearchIndexService(searchTableDataProducer, searchStore);
  }

  @Test
  public void testIndexBatch_handlesException() {
    List<FluentComponent> components = Arrays.asList(mockComponent());
    SearchTableData searchTableData = mockSearchTableData();
    when(searchTableDataProducer.createSearchTableData(components.get(0), repository))
        .thenReturn(Optional.of(searchTableData));

    doThrow(RuntimeException.class).when(searchStore).saveBatch(any());
    doThrow(RuntimeException.class).when(searchStore).save(any());

    underTest.indexBatch(components, repository);

    // this is really a sanity check, the real test is that no exceptions bubble up
    verify(searchStore).save(searchTableData);
  }

  private static SearchTableData mockSearchTableData() {
    return mock(SearchTableData.class);
  }

  private static FluentComponent mockComponent() {
    return mock(FluentComponent.class);
  }
}
