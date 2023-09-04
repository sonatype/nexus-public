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
package org.sonatype.nexus.repository.content.tasks.normalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.cleanup.CleanupFeatureCheck;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NormalizationPriorityServiceTest
    extends TestSupport
{
  @Mock
  private CleanupFeatureCheck cleanupFeatureCheck;

  private Map<String, FormatStoreManager> managersByFormat;

  private List<Format> formats;

  @Test
  public void normalizationPriorityWorksAsExpected() {
    formats = populateFormats(5);
    managersByFormat = populateManagers(formats);

    when(cleanupFeatureCheck.isRetainSupported("format-3")).thenReturn(true);
    when(cleanupFeatureCheck.isRetainSupported("format-5")).thenReturn(true);
    when(cleanupFeatureCheck.isRetainSupported("format-1")).thenReturn(false);
    when(cleanupFeatureCheck.isRetainSupported("format-2")).thenReturn(false);
    when(cleanupFeatureCheck.isRetainSupported("format-4")).thenReturn(false);

    NormalizationPriorityService underTest =
        new NormalizationPriorityService(cleanupFeatureCheck, managersByFormat, formats);

    Map<Format, FormatStoreManager> prioritized = underTest.getPrioritizedFormats();

    assertEquals(5, prioritized.size());

    List<String> resultOrder = prioritized.keySet().stream().map(Format::getValue)
        .collect(Collectors.toList());

    //first formats should be the ones with retain enabled
    assertEquals(resultOrder.get(0), "format-3");
    assertEquals(resultOrder.get(1), "format-5");
  }

  @Test
  public void normalizationPriorityKeepsDefaultOrder() {
    formats = populateFormats(10);
    managersByFormat = populateManagers(formats);

    when(cleanupFeatureCheck.isRetainSupported(anyString())).thenReturn(false);

    NormalizationPriorityService underTest =
        new NormalizationPriorityService(cleanupFeatureCheck, managersByFormat, formats);

    Map<Format, FormatStoreManager> prioritized = underTest.getPrioritizedFormats();

    assertEquals(10, prioritized.size());

    List<String> resultOrder = prioritized.keySet().stream().map(Format::getValue)
        .collect(Collectors.toList());

    assertEquals(resultOrder.get(0), "format-1");
    assertEquals(resultOrder.get(resultOrder.size() - 1), "format-10");
  }

  private Map<String, FormatStoreManager> populateManagers(final List<Format> formats) {
    return formats
        .stream()
        .collect(Collectors.toMap(Format::getValue, (f) -> mock(FormatStoreManager.class)));
  }

  private List<Format> populateFormats(final int size) {
    List<Format> formats = new ArrayList<>();
    for (int i = 1; i <= size; i++) {
      Format current = mock(Format.class);
      when(current.getValue()).thenReturn("format-" + i);

      formats.add(current);
    }

    return formats;
  }
}
