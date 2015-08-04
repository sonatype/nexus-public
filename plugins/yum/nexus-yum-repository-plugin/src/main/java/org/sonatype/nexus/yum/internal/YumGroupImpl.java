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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.yum.YumGroup;
import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.nexus.yum.internal.task.MergeMetadataTask;

import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 2.7
 */
@Named
public class YumGroupImpl
    implements YumGroup
{

  private final static Logger log = LoggerFactory.getLogger(YumGroupImpl.class);

  private final NexusScheduler nexusScheduler;

  private final GroupRepository repository;

  private final File baseDir;

  private final ReadWriteLock lock;

  private YumRepository yumRepository;

  @Inject
  public YumGroupImpl(final NexusScheduler nexusScheduler,
                      final MergeMetadataRequestStrategy mergeMetadataRequestStrategy,
                      final @Assisted GroupRepository repository)
      throws MalformedURLException, URISyntaxException

  {
    this.nexusScheduler = checkNotNull(nexusScheduler);
    this.repository = checkNotNull(repository);
    this.baseDir = RepositoryUtils.getBaseDir(repository);
    this.lock = new ReentrantReadWriteLock();

    repository.registerRequestStrategy(MergeMetadataRequestStrategy.class.getName(), mergeMetadataRequestStrategy);
  }

  @Override
  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public Repository getNexusRepository() {
    return repository;
  }

  @Override
  public YumRepository getYumRepository() throws Exception {
    lock.readLock().lock();
    try {
      if (yumRepository == null) {
        lock.readLock().unlock();
        lock.writeLock().lock();
        try {
          if (yumRepository == null) {
            yumRepository = MergeMetadataTask.createTaskFor(nexusScheduler, repository).get();
          }
        }
        finally {
          lock.readLock().lock();
          lock.writeLock().unlock();
        }
      }
      return yumRepository;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void markDirty() {
    try {
      lock.writeLock().lock();
      yumRepository = null;
      log.debug("Marked {} as dirty.", repository.getId());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

}
