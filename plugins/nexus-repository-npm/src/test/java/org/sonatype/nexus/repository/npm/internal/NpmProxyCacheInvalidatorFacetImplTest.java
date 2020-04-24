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
package org.sonatype.nexus.repository.npm.internal;

import java.util.Collections;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmProxyCacheInvalidatorFacetImplTest
    extends TestSupport
{
  @Mock
  private NpmProxyFacet npmProxyFacet;

  private NpmProxyCacheInvalidatorFacetImpl underTest = new NpmProxyCacheInvalidatorFacetImpl();

  @Test
  public void testOnUrlChange() {
    underTest
        .on(new RepositoryUpdatedEvent(mockRepository("http://example.org"), mockConfiguration("http://example.com")));

    verify(npmProxyFacet).invalidateProxyCaches();
  }

  @Test
  public void testOnNoUrlChange() {
    underTest
        .on(new RepositoryUpdatedEvent(mockRepository("http://example.org"), mockConfiguration("http://example.org")));

    verify(npmProxyFacet, never()).invalidateProxyCaches();
  }

  private Repository mockRepository(final String remoteUrl) {
    Repository repository = mock(Repository.class);
    Configuration configuration = mockConfiguration(remoteUrl);

    when(repository.getConfiguration()).thenReturn(configuration);

    Optional<NpmProxyFacet> facet = Optional.of(npmProxyFacet);
    when(repository.optionalFacet(NpmProxyFacet.class)).thenReturn(facet);

    return repository;
  }

  private Configuration mockConfiguration(final String remoteUrl) {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getAttributes())
        .thenReturn(Collections.singletonMap("proxy", Collections.singletonMap("remoteUrl", remoteUrl)));
    return configuration;
  }
}
