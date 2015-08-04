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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.yum.YumHosted;
import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.nexus.yum.internal.task.GenerateMetadataTask;
import org.sonatype.nexus.yum.internal.task.TaskAlreadyScheduledException;
import org.sonatype.scheduling.ScheduledTask;

import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.io.File.pathSeparator;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonatype.nexus.yum.internal.task.GenerateMetadataTask.ID;

/**
 * @since yum 3.0
 */
@Named
public class YumHostedImpl
    implements YumHosted
{

  private final static Logger LOG = LoggerFactory.getLogger(YumHostedImpl.class);

  private static final int MAX_EXECUTION_COUNT = 100;

  private final NexusScheduler nexusScheduler;

  private final ScheduledThreadPoolExecutor executor;

  private final HostedRepository repository;

  private final File temporaryDirectory;

  private boolean processDeletes;

  private long deleteProcessingDelay;

  private String yumGroupsDefinitionFile;

  private final File baseDir;

  private final Map<String, String> aliases;

  private final Map<ScheduledFuture<?>, DelayedDirectoryDeletionTask> taskMap =
      new HashMap<ScheduledFuture<?>, DelayedDirectoryDeletionTask>();

  private final Map<DelayedDirectoryDeletionTask, ScheduledFuture<?>> reverseTaskMap =
      new HashMap<DelayedDirectoryDeletionTask, ScheduledFuture<?>>();

  @Inject
  public YumHostedImpl(final NexusScheduler nexusScheduler,
                       final ScheduledThreadPoolExecutor executor,
                       final BlockSqliteDatabasesRequestStrategy blockSqliteDatabasesRequestStrategy,
                       final @Assisted HostedRepository repository,
                       final @Assisted File temporaryDirectory)
      throws MalformedURLException, URISyntaxException

  {
    this.nexusScheduler = checkNotNull(nexusScheduler);
    this.executor = checkNotNull(executor);
    this.repository = checkNotNull(repository);
    this.temporaryDirectory = checkNotNull(temporaryDirectory);

    this.processDeletes = true;
    this.deleteProcessingDelay = DEFAULT_DELETE_PROCESSING_DELAY;

    this.aliases = Maps.newHashMap();

    this.baseDir = RepositoryUtils.getBaseDir(repository);

    this.yumGroupsDefinitionFile = null;

    repository.registerRequestStrategy(
        BlockSqliteDatabasesRequestStrategy.class.getName(), checkNotNull(blockSqliteDatabasesRequestStrategy)
    );
  }

  private final YumRepositoryCache cache = new YumRepositoryCache();

  @Override
  public YumHosted setProcessDeletes(final boolean processDeletes) {
    this.processDeletes = processDeletes;
    return this;
  }

  @Override
  public YumHosted setDeleteProcessingDelay(final long numberOfSeconds) {
    this.deleteProcessingDelay = numberOfSeconds;
    return this;
  }

  @Override
  public YumHosted setYumGroupsDefinitionFile(final String yumGroupsDefinitionFile) {
    this.yumGroupsDefinitionFile = yumGroupsDefinitionFile;
    return this;
  }

  @Override
  public boolean shouldProcessDeletes() {
    return processDeletes;
  }

  @Override
  public long deleteProcessingDelay() {
    return deleteProcessingDelay;
  }

  @Override
  public String getYumGroupsDefinitionFile() {
    return yumGroupsDefinitionFile;
  }

  @Override
  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public YumHosted addAlias(final String alias, final String version) {
    aliases.put(alias, version);
    return this;
  }

  @Override
  public YumHosted removeAlias(final String alias) {
    aliases.remove(alias);
    return this;
  }

  @Override
  public YumHosted setAliases(final Map<String, String> aliases) {
    this.aliases.clear();
    this.aliases.putAll(aliases);

    return this;
  }

  @Override
  public String getVersion(final String alias) {
    return aliases.get(alias);
  }

  @Override
  public Repository getNexusRepository() {
    return repository;
  }

  @Override
  public YumRepository getYumRepository() throws Exception {
    return getYumRepository(null);
  }

  ScheduledTask<YumRepository> createYumRepository(final String version,
                                                   final File yumRepoBaseDir)
  {
    try {
      File rpmBaseDir = RepositoryUtils.getBaseDir(repository);
      GenerateMetadataTask task = createTask();
      task.setRpmDir(rpmBaseDir.getAbsolutePath());
      task.setRepoDir(yumRepoBaseDir);
      task.setRepositoryId(repository.getId());
      task.setVersion(version);
      task.setYumGroupsDefinitionFile(getYumGroupsDefinitionFile());
      return submitTask(task);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to create repository", e);
    }
  }

  @Override
  public YumRepository getYumRepository(final String version)
      throws Exception
  {
    YumRepositoryImpl yumRepository = cache.lookup(repository.getId(), version);
    if ((yumRepository == null) || yumRepository.isDirty()) {
      final ScheduledTask<YumRepository> future = createYumRepository(
          version, createRepositoryTempDir(repository, version)
      );
      yumRepository = (YumRepositoryImpl) future.get();
      cache.cache(yumRepository);
    }
    return yumRepository;
  }

  private ScheduledTask<YumRepository> submitTask(GenerateMetadataTask task) {
    try {
      return nexusScheduler.submit(ID, task);
    }
    catch (TaskAlreadyScheduledException e) {
      return mergeAddedFiles(e.getOriginal(), task);
    }
  }

  @Override
  public ScheduledTask<YumRepository> regenerate() {
    return addRpmAndRegenerate(null);
  }

  @Override
  public void markDirty(final String version) {
    cache.markDirty(repository.getId(), version);
  }

  @Override
  public ScheduledTask<YumRepository> addRpmAndRegenerate(@Nullable String filePath) {
    try {
      LOG.debug("Processing added rpm {}:{}", repository.getId(), filePath);
      final File rpmBaseDir = RepositoryUtils.getBaseDir(repository);
      final GenerateMetadataTask task = createTask();
      task.setRpmDir(rpmBaseDir.getAbsolutePath());
      task.setRepositoryId(repository.getId());
      task.setAddedFiles(filePath);
      task.setYumGroupsDefinitionFile(getYumGroupsDefinitionFile());
      return submitTask(task);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to create repository", e);
    }
  }

  @SuppressWarnings("unchecked")
  private ScheduledTask<YumRepository> mergeAddedFiles(ScheduledTask<?> existingScheduledTask,
                                                       GenerateMetadataTask taskToMerge)
  {
    if (isNotBlank(taskToMerge.getAddedFiles())) {
      final GenerateMetadataTask existingTask = (GenerateMetadataTask) existingScheduledTask.getTask();
      if (isBlank(existingTask.getAddedFiles())) {
        existingTask.setAddedFiles(taskToMerge.getAddedFiles());
      }
      else {
        existingTask.setAddedFiles(
            existingTask.getAddedFiles() + pathSeparator + taskToMerge.getAddedFiles());
      }
    }
    return (ScheduledTask<YumRepository>) existingScheduledTask;
  }

  private GenerateMetadataTask createTask() {
    final GenerateMetadataTask task = nexusScheduler.createTaskInstance(GenerateMetadataTask.class);
    if (task == null) {
      throw new IllegalStateException(
          "Could not create a task fo type " + GenerateMetadataTask.class.getName()
      );
    }
    return task;
  }

  private File createRepositoryTempDir(Repository repository, String version) {
    return new File(temporaryDirectory, repository.getId() + File.separator + version);
  }

  @Override
  public void regenerateWhenPathIsRemoved(String path) {
    if (shouldProcessDeletes()) {
      LOG.debug("Processing deleted rpm {}:{}", repository.getId(), path);
      if (findDelayedParentDirectory(path) == null) {
        regenerate();
      }
    }
  }

  @Override
  public void regenerateWhenDirectoryIsRemoved(String path) {
    if (shouldProcessDeletes()) {
      LOG.debug("Processing deleted dir {}:{}", repository.getId(), path);
      if (findDelayedParentDirectory(path) == null) {
        schedule(new DelayedDirectoryDeletionTask(path));
      }
    }
  }

  private void schedule(DelayedDirectoryDeletionTask task) {
    final ScheduledFuture<?> future = executor.schedule(task, deleteProcessingDelay(), SECONDS);
    taskMap.put(future, task);
    reverseTaskMap.put(task, future);
  }

  private DelayedDirectoryDeletionTask findDelayedParentDirectory(final String path) {
    for (final Runnable runnable : executor.getQueue()) {
      DelayedDirectoryDeletionTask dirTask = taskMap.get(runnable);
      if (dirTask != null && path.startsWith(dirTask.path)) {
        return dirTask;
      }
    }
    return null;
  }

  private boolean isDeleted(String path) {
    try {
      repository.retrieveItem(new ResourceStoreRequest(path));
      return false;
    }
    catch (Exception e) {
      return true;
    }
  }

  private class DelayedDirectoryDeletionTask
      implements Runnable
  {

    private final String path;

    private int executionCount = 0;

    private DelayedDirectoryDeletionTask(final String path) {
      this.path = path;
    }

    @Override
    public void run() {
      executionCount++;
      final ScheduledFuture<?> future = reverseTaskMap.remove(this);
      if (future != null) {
        taskMap.remove(future);
      }
      if (isDeleted(path)) {
        LOG.debug(
            "Recreate yum repository {} because of removed path {}", getNexusRepository().getId(), path
        );
        regenerate();
      }
      else if (executionCount < MAX_EXECUTION_COUNT) {
        LOG.debug(
            "Rescheduling creation of yum repository {} because path {} not deleted.",
            getNexusRepository().getId(), path
        );
        schedule(this);
      }
      else {
        LOG.warn(
            "Deleting path {} in repository {} took too long - retried {} times.",
            path, getNexusRepository().getId(), MAX_EXECUTION_COUNT
        );
      }
    }
  }

}
