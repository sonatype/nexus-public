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
package org.sonatype.repository.helm.internal.orient.metadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.repository.helm.internal.metadata.ChartIndex;
import org.sonatype.repository.helm.internal.util.YamlParser;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.repository.helm.internal.HelmFormat.HASH_ALGORITHMS;

public class IndexYamlBuilderTest
    extends TestSupport
{
  @Mock
  YamlParser yamlParser;

  @Mock
  StorageFacet storageFacet;

  @Mock
  TempBlob tempBlob;

  private IndexYamlBuilder underTest;

  private ChartIndex index;

  @Before
  public void setUp() throws Exception {
    underTest = new IndexYamlBuilder(this.yamlParser);
    index = new ChartIndex();
  }

  @Test
  public void testChartIndexPassedCorrectly() throws Exception {
    setupRepositoryMock();
    ArgumentCaptor<InputStream> captorStorage = ArgumentCaptor.forClass(InputStream.class);
    ArgumentCaptor<ChartIndex> captor = ArgumentCaptor.forClass(ChartIndex.class);

    TempBlob tempBlob = underTest.build(index, storageFacet);

    verify(storageFacet).createTempBlob(captorStorage.capture(), eq(HASH_ALGORITHMS));
    verify(yamlParser).write(any(OutputStream.class), captor.capture());

    assertEquals(index, captor.getValue());
  }

  @Test
  public void testTempBlobReturned() throws Exception {
    initializeStorageFacet();
    TempBlob result = underTest.build(index, storageFacet);

    assertEquals(result, tempBlob);
  }

  private void initializeStorageFacet() {
    when(storageFacet.createTempBlob(any(InputStream.class), eq(HASH_ALGORITHMS))).thenReturn(tempBlob);
  }

  private void setupRepositoryMock() {
    when(storageFacet.createTempBlob(any(InputStream.class), any(Iterable.class))).thenAnswer(args -> {
      InputStream inputStream = (InputStream) args.getArguments()[0];
      byte[] bytes = IOUtils.toByteArray(inputStream);
      when(tempBlob.get()).thenReturn(new ByteArrayInputStream(bytes));
      return tempBlob;
    });
  }
}
