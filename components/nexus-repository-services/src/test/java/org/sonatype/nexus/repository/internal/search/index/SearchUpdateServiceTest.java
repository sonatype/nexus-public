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
package org.sonatype.nexus.repository.internal.search.index;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.index.SearchIndexUpdateManager;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchUpdateServiceTest
    extends TestSupport
{
  @Mock
  private Format managedFormat;

  @Mock
  private Format unmanagedFormat;

  @Mock
  private Repository managedRepository;

  @Mock
  private Repository unmanagedRepository;

  @Mock
  private SearchIndexUpdateManager versionManager;

  private SearchUpdateService underTest;

  @Before
  public void setup() {
    when(managedFormat.getValue()).thenReturn("managed");
    when(unmanagedFormat.getValue()).thenReturn("unmanaged");
    when(managedRepository.getFormat()).thenReturn(managedFormat);
    when(unmanagedRepository.getFormat()).thenReturn(unmanagedFormat);
    Map<String, SearchIndexUpdateManager> searchIndexVersionManagers =
        ImmutableMap.of("managed", versionManager);
    underTest = new SearchUpdateService(searchIndexVersionManagers);
  }

  @Test
  public void needsReindex_unmanaged_returnsFalse() {
    assertFalse(underTest.needsReindex(unmanagedRepository));
  }

  @Test
  public void needsReindex_managed_returnsResultFromManager() {
    when(versionManager.needsReindex(managedRepository)).thenReturn(true);
    assertTrue(underTest.needsReindex(managedRepository));
    when(versionManager.needsReindex(managedRepository)).thenReturn(false);
    assertFalse(underTest.needsReindex(managedRepository));
  }
}
