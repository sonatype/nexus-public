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
package org.sonatype.nexus.repository.cache;

import java.util.Optional;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepositoryCacheInvalidationServiceTest
{
  @Mock
  private Repository repository;

  @Mock
  private GroupFacet groupFacet;

  @Mock
  private ProxyFacet proxyFacet;

  @Mock
  private NegativeCacheFacet negativeCacheFacet;

  private RepositoryCacheInvalidationService underTest;

  @BeforeEach
  public void setup() {
    underTest = new RepositoryCacheInvalidationService();
  }

  @Test
  void testProcessCachesInvalidation_group() {
    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    when(repository.optionalFacet(NegativeCacheFacet.class)).thenReturn(Optional.empty());
    when(repository.optionalFacet(ProxyFacet.class)).thenReturn(Optional.empty());

    underTest.processCachesInvalidation(repository);

    verify(groupFacet).invalidateGroupCaches();
  }

  @Test
  void testProcessCachesInvalidation_proxy() {
    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    when(repository.optionalFacet(NegativeCacheFacet.class)).thenReturn(Optional.of(negativeCacheFacet));
    when(repository.optionalFacet(ProxyFacet.class)).thenReturn(Optional.of(proxyFacet));

    underTest.processCachesInvalidation(repository);

    verify(proxyFacet).invalidateProxyCaches();
    verify(negativeCacheFacet).invalidate();
  }
}
