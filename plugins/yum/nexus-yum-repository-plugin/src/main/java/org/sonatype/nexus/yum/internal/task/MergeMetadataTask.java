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
package org.sonatype.nexus.yum.internal.task;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusTask;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.nexus.yum.internal.MetadataProcessor;
import org.sonatype.nexus.yum.internal.RepoMD;
import org.sonatype.nexus.yum.internal.RepositoryUtils;
import org.sonatype.nexus.yum.internal.YumRepositoryImpl;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.sonatype.nexus.yum.Yum.PATH_OF_REPODATA;
import static org.sonatype.nexus.yum.Yum.PATH_OF_REPOMD_XML;
import static org.sonatype.scheduling.TaskState.RUNNING;

/**
 * @since yum 3.0
 */
@Named(MergeMetadataTask.ID)
public class MergeMetadataTask
    extends AbstractNexusTask<YumRepository>
{

  private static final Logger log = LoggerFactory.getLogger(MergeMetadataTask.class);

  public static final String ID = "MergeMetadataTask";

  private GroupRepository groupRepository;

  private final YumRegistry yumRegistry;

  private final CommandLineExecutor commandLineExecutor;

  @Inject
  public MergeMetadataTask(final EventBus eventBus,
                           final YumRegistry yumRegistry,
                           final CommandLineExecutor commandLineExecutor)
  {
    super(eventBus, null);
    this.yumRegistry = checkNotNull(yumRegistry);
    this.commandLineExecutor = checkNotNull(commandLineExecutor);
  }

  public void setGroupRepository(final GroupRepository groupRepository) {
    this.groupRepository = groupRepository;
  }

  @Override
  protected YumRepository doRun()
      throws Exception
  {
    if (isValidRepository()) {
      deleteYumTempDirs();

      final File repoBaseDir = RepositoryUtils.getBaseDir(groupRepository);
      RepositoryItemUid groupRepoMdUid = groupRepository.createUid("/" + PATH_OF_REPOMD_XML);
      try {
        groupRepoMdUid.getLock().lock(Action.update);

        final List<File> memberReposBaseDirs = getBaseDirsOfMemberRepositories();
        if (memberReposBaseDirs.size() > 1) {
          log.debug("Merging repository group '{}' out of {}", groupRepository.getId(), memberReposBaseDirs);
          commandLineExecutor.exec(buildCommand(repoBaseDir, memberReposBaseDirs));
          MetadataProcessor.processMergedMetadata(groupRepository, memberReposBaseDirs);
          log.debug("Group repository '{}' merged", groupRepository.getId());
        }
        else {
          // delete without using group repository API as group repositories does not allow delete (read only)
          File groupRepodata = new File(repoBaseDir, PATH_OF_REPODATA);
          deleteQuietly(groupRepodata);
          if (memberReposBaseDirs.size() == 1) {
            log.debug(
                "Copying Yum metadata from {} to group repository {}",
                memberReposBaseDirs.get(0), groupRepository.getId()
            );
            copyDirectory(new File(memberReposBaseDirs.get(0), PATH_OF_REPODATA), groupRepodata);
          }
        }
      }
      finally {
        groupRepoMdUid.getLock().unlock();
      }

      deleteYumTempDirs();

      return new YumRepositoryImpl(repoBaseDir, groupRepository.getId(), null);
    }
    return null;
  }

  private List<File> getBaseDirsOfMemberRepositories()
      throws Exception
  {
    final List<File> baseDirs = new ArrayList<File>();
    for (final Repository memberRepository : groupRepository.getMemberRepositories()) {
      log.trace("Looking up latest Yum metadata in {} member of {}", memberRepository.getId(), groupRepository.getId());
      StorageItem repomdItem = null;
      try {
        log.trace("Retrieving {}:{}", memberRepository.getId(), "/" + PATH_OF_REPOMD_XML);
        repomdItem = memberRepository.retrieveItem(
            new ResourceStoreRequest("/" + PATH_OF_REPOMD_XML)
        );
      }
      catch (ItemNotFoundException ignore) {
        // skipping as it looks like member is not an Yum repository
      }
      if (repomdItem != null && repomdItem instanceof StorageFileItem) {
        try (InputStream in = ((StorageFileItem) repomdItem).getInputStream()) {
          final RepoMD repomd = new RepoMD(in);
          for (final String location : repomd.getLocations()) {
            String retrieveLocation = "/" + location;
            if (!retrieveLocation.matches("/" + PATH_OF_REPODATA + "/.*\\.sqlite\\.bz2")) {
              log.trace("Retrieving {}:{}", memberRepository.getId(), retrieveLocation);
              memberRepository.retrieveItem(new ResourceStoreRequest(retrieveLocation));
            }
          }
        }
        // all metadata files are available by now so lets use it
        baseDirs.add(RepositoryUtils.getBaseDir(memberRepository).getCanonicalFile());
      }
    }
    return baseDirs;
  }

  private void deleteYumTempDirs()
      throws IOException
  {
    final String yumTmpDirPrefix = "yum-" + System.getProperty("user.name");
    final File tmpDir = new File("/var/tmp");
    if (tmpDir.exists()) {
      final File[] yumTmpDirs = tmpDir.listFiles(new FilenameFilter()
      {

        @Override
        public boolean accept(File dir, String name) {
          return name.startsWith(yumTmpDirPrefix);
        }
      });
      for (File yumTmpDir : yumTmpDirs) {
        log.debug("Deleting yum temp dir : {}", yumTmpDir);
        deleteQuietly(yumTmpDir);
      }
    }
  }

  @Override
  public boolean allowConcurrentExecution(Map<String, List<ScheduledTask<?>>> activeTasks) {

    if (activeTasks.containsKey(ID)) {
      for (ScheduledTask<?> scheduledTask : activeTasks.get(ID)) {
        if (RUNNING.equals(scheduledTask.getTaskState())) {
          if (conflictsWith((MergeMetadataTask) scheduledTask.getTask())) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean conflictsWith(MergeMetadataTask task) {
    return task.getGroupRepository() != null && this.getGroupRepository() != null
        && task.getGroupRepository().getId().equals(getGroupRepository().getId());
  }

  @Override
  protected String getAction() {
    return "MERGE_YUM_METADATA";
  }

  @Override
  protected String getMessage() {
    return format("Merging Yum metadata in repository '%s'", groupRepository.getId());
  }

  public GroupRepository getGroupRepository() {
    return groupRepository;
  }

  private boolean isValidRepository() {
    return groupRepository != null && !groupRepository.getMemberRepositories().isEmpty();
  }

  private String buildCommand(File repoBaseDir, List<File> memberRepoBaseDirs)
      throws MalformedURLException, URISyntaxException
  {
    final StringBuilder repos = new StringBuilder();
    for (File memberRepoBaseDir : memberRepoBaseDirs) {
      repos.append(" --repo=");
      repos.append(memberRepoBaseDir.toURI().toASCIIString());
    }
    return format(
        "%s --no-database %s -o %s",
        yumRegistry.getMergerepoPath(), repos.toString(), repoBaseDir.getAbsolutePath()
    );
  }

  public static ScheduledTask<YumRepository> createTaskFor(final NexusScheduler nexusScheduler,
                                                           final GroupRepository groupRepository)
  {
    final MergeMetadataTask task = nexusScheduler.createTaskInstance(
        MergeMetadataTask.class
    );
    task.setGroupRepository(groupRepository);
    return nexusScheduler.submit(MergeMetadataTask.ID, task);
  }

}
