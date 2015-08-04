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
package org.sonatype.nexus.yum.internal;

import java.io.File;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.nexus.yum.internal.support.YumNexusTestSupport;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotSame;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YumHostedImplTest
    extends YumNexusTestSupport
{

  private static final String VERSION_1_0 = "1.0";

  private static final String SNAPSHOTS = "snapshots";

  @Inject
  private RepositoryRegistry repositoryRegistry;

  @Inject
  private YumRegistry yumRegistry;

  private YumHostedImpl yum;

  @Before
  public void activateService() {
    final MavenHostedRepository repository = createRepository(SNAPSHOTS);
    repositoryRegistry.addRepository(repository);
    yum = (YumHostedImpl) yumRegistry.register(repository);
  }

  @Test
  public void shouldCacheRepository()
      throws Exception
  {
    final YumRepository repo1 = yum.getYumRepository(VERSION_1_0);
    final YumRepository repo2 = yum.getYumRepository(VERSION_1_0);

    Assert.assertEquals(repo1, repo2);
  }

  @Test
  public void shouldRecreateRepository()
      throws Exception
  {
    final YumRepository repo1 = yum.getYumRepository(VERSION_1_0);

    yum.markDirty(VERSION_1_0);

    YumRepository repo2 = yum.getYumRepository(VERSION_1_0);

    assertNotSame(repo1, repo2);
  }

  public static MavenHostedRepository createRepository(String id) {
    final MavenHostedRepository repository = mock(MavenHostedRepository.class);
    when(repository.getId()).thenReturn(id);
    when(repository.getLocalUrl()).thenReturn(getTempUrl());
    when(repository.getProviderRole()).thenReturn(Repository.class.getName());
    when(repository.getProviderHint()).thenReturn("maven2");
    when(repository.adaptToFacet(HostedRepository.class)).thenReturn(repository);
    when(repository.adaptToFacet(MavenRepository.class)).thenReturn(repository);
    when(repository.adaptToFacet(MavenHostedRepository.class)).thenReturn(repository);
    final RepositoryItemUid uid = mock(RepositoryItemUid.class);
    when(uid.getLock()).thenReturn(mock(RepositoryItemUidLock.class));
    when(repository.createUid(anyString())).thenReturn(uid);
    when(repository.getRepositoryContentClass()).thenReturn(new Maven2ContentClass());
    when(repository.isExposed()).thenReturn(true);

    final RepositoryKind repositoryKind = mock(RepositoryKind.class);
    when(repository.getRepositoryKind()).thenReturn(repositoryKind);
    Mockito.<Class<?>>when(repositoryKind.getMainFacet()).thenReturn(MavenHostedRepository.class);
    when(repositoryKind.isFacetAvailable(HostedRepository.class)).thenReturn(true);
    when(repositoryKind.isFacetAvailable(MavenRepository.class)).thenReturn(true);
    when(repositoryKind.isFacetAvailable(MavenHostedRepository.class)).thenReturn(true);
    return repository;
  }

  private static String getTempUrl() {
    return new File(System.getProperty("java.io.tmpdir")).toURI().toString();
  }
}
