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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.yum.YumHosted;
import org.sonatype.nexus.yum.internal.task.GenerateMetadataTask;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static java.lang.Thread.sleep;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class YumHostedImplDeletionsTest
    extends TestSupport
{

  private static final String BASE_PATH = "/base/path";

  private static final String SUB_PATH1 = BASE_PATH + "/subdir/foo.rpm";

  private static final String SUB_PATH2 = BASE_PATH + "/subdir/bar.rpm";

  private static final String SUB_PATH3 = BASE_PATH + "/otherdir/test.rpm";

  private static final long TIMEOUT_IN_SEC = 1;

  private static final String REPO_ID = "snapshots";

  private YumHosted yum;

  private HostedRepository repository;

  private NexusScheduler nexusScheduler;

  @Before
  public void prepareService()
      throws MalformedURLException, URISyntaxException
  {
    repository = mock(HostedRepository.class);
    when(repository.getId()).thenReturn(REPO_ID);
    when(repository.getLocalUrl()).thenReturn("/target");

    nexusScheduler = mock(NexusScheduler.class);
    when(nexusScheduler.createTaskInstance(GenerateMetadataTask.class)).thenReturn(
        mock(GenerateMetadataTask.class)
    );

    yum = new YumHostedImpl(
        nexusScheduler,
        new ScheduledThreadPoolExecutor(10),
        new BlockSqliteDatabasesRequestStrategy(),
        repository,
        new File(util.getTargetDir(), "tmp")
    ).setProcessDeletes(true)
        .setDeleteProcessingDelay(TIMEOUT_IN_SEC);
  }

  @Test
  public void shouldNotRegenerateRepositoryWithoutRpms()
      throws Exception
  {
    yum.regenerateWhenDirectoryIsRemoved(BASE_PATH);
    sleep(TIMEOUT_IN_SEC * 2000);
    verify(nexusScheduler, times(0)).submit(
        Mockito.anyString(), Mockito.any(GenerateMetadataTask.class)
    );
  }

  @Test
  public void shouldRegenerateRepositoryWithRpm()
      throws Exception
  {
    when(repository.retrieveItem(any(ResourceStoreRequest.class)))
        .thenThrow(new ItemNotFoundException(new ResourceStoreRequest("/some/fake/path")));

    yum.regenerateWhenDirectoryIsRemoved(BASE_PATH);
    yum.regenerateWhenPathIsRemoved(SUB_PATH1);

    sleep(TIMEOUT_IN_SEC * 2000);

    verify(nexusScheduler, times(1)).submit(
        Mockito.anyString(), Mockito.any(GenerateMetadataTask.class)
    );
  }

  @Test
  public void shouldRegenerateRepositoryWithRpms()
      throws Exception
  {
    when(repository.retrieveItem(any(ResourceStoreRequest.class)))
        .thenThrow(new ItemNotFoundException(new ResourceStoreRequest("/some/fake/path")));

    yum.regenerateWhenDirectoryIsRemoved(BASE_PATH);
    yum.regenerateWhenPathIsRemoved(SUB_PATH1);
    yum.regenerateWhenPathIsRemoved(SUB_PATH2);
    yum.regenerateWhenPathIsRemoved(SUB_PATH3);

    sleep(TIMEOUT_IN_SEC * 2000);

    verify(nexusScheduler, times(1)).submit(
        Mockito.anyString(), Mockito.any(GenerateMetadataTask.class)
    );
  }

  @SuppressWarnings("deprecation")
  @Test
  public void shouldWaitUntilDirIsDeleted()
      throws Exception
  {
    when(repository.retrieveItem(any(ResourceStoreRequest.class)))
        .thenReturn(null)
        .thenReturn(null)
        .thenThrow(new ItemNotFoundException(new ResourceStoreRequest("/some/fake/path")));

    yum.regenerateWhenDirectoryIsRemoved(BASE_PATH);
    yum.regenerateWhenPathIsRemoved(SUB_PATH1);

    sleep(TIMEOUT_IN_SEC * 1500);
    verify(nexusScheduler, times(0)).submit(
        Mockito.anyString(), Mockito.any(GenerateMetadataTask.class)
    );

    sleep(TIMEOUT_IN_SEC * 2500);
    verify(nexusScheduler, times(1)).submit(
        Mockito.anyString(), Mockito.any(GenerateMetadataTask.class)
    );
  }

  @Test
  public void shouldRegenerateRepositoryAfterDeletionSingleRpm()
      throws Exception
  {
    yum.regenerateWhenPathIsRemoved(SUB_PATH1);
    verify(nexusScheduler, times(1)).submit(
        Mockito.anyString(), Mockito.any(GenerateMetadataTask.class)
    );
  }
}
