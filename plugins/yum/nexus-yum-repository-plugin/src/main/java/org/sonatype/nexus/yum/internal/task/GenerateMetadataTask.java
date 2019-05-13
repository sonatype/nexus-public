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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusTask;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.YumGroup;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.nexus.yum.internal.ListFileFactory;
import org.sonatype.nexus.yum.internal.RepositoryUtils;
import org.sonatype.nexus.yum.internal.RpmListWriter;
import org.sonatype.nexus.yum.internal.RpmScanner;
import org.sonatype.nexus.yum.internal.YumRepositoryImpl;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.schedules.RunNowSchedule;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.StorageItem;

import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonatype.nexus.yum.Yum.PATH_OF_REPOMD_XML;
import static org.sonatype.scheduling.TaskState.RUNNING;
import static org.sonatype.scheduling.TaskState.SLEEPING;
import static org.sonatype.scheduling.TaskState.SUBMITTED;

/**
 * Create a yum-repository directory via 'createrepo' command line tool.
 *
 * @since yum 3.0
 */
@Named(GenerateMetadataTask.ID)
public class GenerateMetadataTask
    extends AbstractNexusTask<YumRepository>
    implements ListFileFactory
{

  public static final String ID = "GenerateMetadataTask";

  private static final String PACKAGE_FILE_DIR_NAME = ".packageFiles";

  private static final String CACHE_DIR_PREFIX = ".cache-";

  private static final Logger LOG = LoggerFactory.getLogger(GenerateMetadataTask.class);

  public static final String PARAM_REPO_ID = "repoId";

  public static final String PARAM_RPM_DIR = "rpmDir";

  public static final String PARAM_REPO_DIR = "repoDir";

  public static final String PARAM_VERSION = "version";

  public static final String PARAM_ADDED_FILES = "addedFiles";

  public static final String PARAM_SINGLE_RPM_PER_DIR = "singleRpmPerDir";

  public static final String PARAM_FORCE_FULL_SCAN = "forceFullScan";

  public static final String PARAM_YUM_GROUPS_DEFINITION_FILE = "yumGroupsDefinitionFile";

  private final RepositoryRegistry repositoryRegistry;

  private final RpmScanner scanner;

  private final YumRegistry yumRegistry;

  private final Manager routingManager;

  private final CommandLineExecutor commandLineExecutor;

  private final DigestCalculatingInspector digestCalculatingInspector;

  @Inject
  public GenerateMetadataTask(final EventBus eventBus,
                              final RepositoryRegistry repositoryRegistry,
                              final YumRegistry yumRegistry,
                              final RpmScanner scanner,
                              final Manager routingManager,
                              final CommandLineExecutor commandLineExecutor,
                              final DigestCalculatingInspector digestCalculatingInspector)
  {
    super(eventBus, null);

    this.yumRegistry = checkNotNull(yumRegistry);
    this.scanner = checkNotNull(scanner);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.routingManager = checkNotNull(routingManager);
    this.commandLineExecutor = checkNotNull(commandLineExecutor);
    this.digestCalculatingInspector = digestCalculatingInspector;

    getParameters().put(PARAM_SINGLE_RPM_PER_DIR, Boolean.toString(true));
  }

  @Override
  protected YumRepository doRun()
      throws Exception
  {
    String repositoryId = getRepositoryId();

    if (!StringUtils.isEmpty(repositoryId)) {
      checkState(
          yumRegistry.isRegistered(repositoryId),
          "Metadata regeneration can only be run on repositories that have an enabled 'Yum: Generate Metadata' capability"
      );
      Yum yum = yumRegistry.get(repositoryId);
      checkState(
          yum.getNexusRepository().getRepositoryKind().isFacetAvailable(HostedRepository.class),
          "Metadata generation can only be run on hosted repositories"
      );
    }

    setDefaults();

    final Repository repository = findRepository();
    final RepositoryItemUid mdUid = repository.createUid("/" + PATH_OF_REPOMD_XML);
    try {
      mdUid.getLock().lock(Action.update);

      LOG.debug("Generating Yum-Repository for '{}' ...", getRpmDir());
      try {
        // NEXUS-6680: Nuke cache dir if force rebuild in effect
        if (shouldForceFullScan()) {
          DirSupport.deleteIfExists(getCacheDir().toPath());
        }
        DirSupport.mkdir(getRepoDir().toPath());

        File rpmListFile = createRpmListFile();
        commandLineExecutor.exec(buildCreateRepositoryCommand(rpmListFile));

        StorageItem item = repository.retrieveItem(new ResourceStoreRequest("/" + PATH_OF_REPOMD_XML));

        if (item != null) {
          digestCalculatingInspector.processStorageItem(item);
          repository.getAttributesHandler().storeAttributes(item);
        }
      }
      catch (IOException e) {
        LOG.warn("Yum metadata generation failed", e);
        throw new IOException("Yum metadata generation failed", e);
      }
    }
    finally {
      mdUid.getLock().unlock();
    }

    // TODO dubious
    Thread.sleep(100);

    final MavenRepository mavenRepository = repository.adaptToFacet(MavenRepository.class);
    if (mavenRepository != null) {
      try {
        routingManager.forceUpdatePrefixFile(mavenRepository);
      }
      catch (Exception e) {
        logger.warn("Could not update Whitelist for repository '{}'", mavenRepository, e);
      }
    }

    regenerateMetadataForGroups();
    return new YumRepositoryImpl(getRepoDir(), repositoryId, getVersion());
  }

  protected void setDefaults()
      throws MalformedURLException, URISyntaxException
  {
    final Repository repository = findRepository();
    if (isBlank(getRpmDir()) && repository != null) {
      setRpmDir(RepositoryUtils.getBaseDir(repository).getAbsolutePath());
    }
    if (isBlank(getParameter(PARAM_REPO_DIR)) && isNotBlank(getRpmDir())) {
      setRepoDir(new File(getRpmDir()));
    }
  }

  private Repository findRepository() {
    try {
      return repositoryRegistry.getRepository(getRepositoryId());
    }
    catch (NoSuchRepositoryException e) {
      return null;
    }
  }

  @Override
  protected String getAction() {
    return "GENERATE_YUM_METADATA";
  }

  @Override
  protected String getMessage() {
    return format("Generate Yum metadata of repository '%s'", getRepositoryId());
  }

  @Override
  public boolean allowConcurrentExecution(Map<String, List<ScheduledTask<?>>> activeTasks) {

    if (activeTasks.containsKey(ID)) {
      int activeRunningTasks = 0;
      for (ScheduledTask<?> scheduledTask : activeTasks.get(ID)) {
        if (RUNNING.equals(scheduledTask.getTaskState())) {
          if (conflictsWith((GenerateMetadataTask) scheduledTask.getTask())) {
            return false;
          }
          activeRunningTasks++;
        }
      }
      return activeRunningTasks < yumRegistry.maxNumberOfParallelThreads();
    }

    return true;
  }

  @Override
  public boolean allowConcurrentSubmission(Map<String, List<ScheduledTask<?>>> activeTasks) {
    if (activeTasks.containsKey(ID)) {
      for (ScheduledTask<?> scheduledTask : activeTasks.get(ID)) {
        if (isSubmitted(scheduledTask)
            && conflictsWith((GenerateMetadataTask) scheduledTask.getTask())
            && scheduledTask.getSchedule() instanceof RunNowSchedule) {
          throw new TaskAlreadyScheduledException(scheduledTask, "Found same task in scheduler queue.");
        }
      }
    }

    return true;
  }

  private boolean isSubmitted(ScheduledTask<?> scheduledTask) {
    return SUBMITTED.equals(scheduledTask.getTaskState()) || SLEEPING.equals(scheduledTask.getTaskState());
  }

  private void regenerateMetadataForGroups() {
    if (StringUtils.isBlank(getVersion())) {
      try {
        final Repository repository = repositoryRegistry.getRepository(getRepositoryId());
        for (GroupRepository groupRepository : repositoryRegistry.getGroupsOfRepository(repository)) {
          Yum yum = yumRegistry.get(groupRepository.getId());
          if (yum != null && yum instanceof YumGroup) {
            ((YumGroup) yum).markDirty();
          }
        }
      }
      catch (NoSuchRepositoryException e) {
        logger.warn(
            "Repository '{}' does not exist anymore. Backing out from triggering group merge for it.",
            getRepositoryId()
        );
      }
    }
  }

  private boolean conflictsWith(GenerateMetadataTask task) {
    if (StringUtils.equals(getRepositoryId(), task.getRepositoryId())) {
      return StringUtils.equals(getVersion(), task.getVersion());
    }
    return false;
  }

  private File createRpmListFile()
      throws IOException
  {
    return new RpmListWriter(
        new File(getRpmDir()),
        getAddedFiles(),
        getVersion(),
        isSingleRpmPerDirectory(),
        shouldForceFullScan(),
        this,
        scanner
    ).writeList();
  }

  private String getRepositoryIdVersion() {
    return getRepositoryId() + (isNotBlank(getVersion()) ? ("-version-" + getVersion()) : "");
  }

  private String buildCreateRepositoryCommand(File packageList) {
    StringBuilder commandLine = new StringBuilder();
    commandLine.append(yumRegistry.getCreaterepoPath());
    if (!shouldForceFullScan()) {
      commandLine.append(" --update");
    }
    commandLine.append(" --verbose --no-database");
    commandLine.append(" --outputdir ").append(getRepoDir().getAbsolutePath());
    commandLine.append(" --pkglist ").append(packageList.getAbsolutePath());
    commandLine.append(" --cachedir ").append(createCacheDir().getAbsolutePath());
    final String yumGroupsDefinitionFile = getYumGroupsDefinitionFile();
    if (yumGroupsDefinitionFile != null) {
      final File file = new File(getRepoDir().getAbsolutePath(), yumGroupsDefinitionFile);
      final String path = file.getAbsolutePath();
      if (file.exists()) {
        if (file.getName().toLowerCase().endsWith(".xml")) {
          commandLine.append(" --groupfile ").append(path);
        }
        else {
          LOG.warn("Yum groups definition file '{}' must have an '.xml' extension, ignoring", path);
        }
      }
      else {
        LOG.warn("Yum groups definition file '{}' doesn't exist, ignoring", path);
      }
    }
    commandLine.append(" ").append(getRpmDir());

    return commandLine.toString();
  }

  @Override
  public File getRpmListFile() {
    return new File(createPackageDir(), getRepositoryId() + ".txt");
  }

  private File createCacheDir() {
    return getCacheDir(getRepositoryIdVersion());
  }

  private File createPackageDir() {
    return getCacheDir(PACKAGE_FILE_DIR_NAME);
  }

  private File getCacheDir(final String name) {
    final File cacheDir = new File(getCacheDir(), name);
    try {
      DirSupport.mkdir(cacheDir.toPath());
    }
    catch (IOException e) {
      Throwables.propagate(e);
    }
    return cacheDir;
  }

  private File getCacheDir() {
    return new File(yumRegistry.getTemporaryDirectory(), CACHE_DIR_PREFIX + getRepositoryId());
  }

  @Override
  public File getRpmListFile(String version) {
    return new File(createPackageDir(), getRepositoryId() + "-" + version + ".txt");
  }

  public String getRepositoryId() {
    return getParameter(PARAM_REPO_ID);
  }

  public void setRepositoryId(String repositoryId) {
    getParameters().put(PARAM_REPO_ID, repositoryId);
  }

  public String getAddedFiles() {
    return getParameter(PARAM_ADDED_FILES);
  }

  public void setAddedFiles(String addedFiles) {
    getParameters().put(PARAM_ADDED_FILES, addedFiles);
  }

  public File getRepoDir() {
    return new File(getParameter(PARAM_REPO_DIR));
  }

  public void setRepoDir(File repoDir) {
    getParameters().put(PARAM_REPO_DIR, repoDir.getAbsolutePath());
  }

  public String getRpmDir() {
    return getParameter(PARAM_RPM_DIR);
  }

  public void setRpmDir(String rpmDir) {
    getParameters().put(PARAM_RPM_DIR, rpmDir);
  }

  public String getVersion() {
    return getParameter(PARAM_VERSION);
  }

  public void setVersion(String version) {
    getParameters().put(PARAM_VERSION, version);
  }

  public String getYumGroupsDefinitionFile() {
    return getParameter(PARAM_YUM_GROUPS_DEFINITION_FILE);
  }

  public void setYumGroupsDefinitionFile(String file) {
    getParameters().put(PARAM_YUM_GROUPS_DEFINITION_FILE, file);
  }

  public boolean isSingleRpmPerDirectory() {
    return Boolean.valueOf(getParameter(PARAM_SINGLE_RPM_PER_DIR));
  }

  public boolean shouldForceFullScan() {
    return Boolean.valueOf(getParameter(PARAM_FORCE_FULL_SCAN));
  }

  public void setSingleRpmPerDirectory(boolean singleRpmPerDirectory) {
    getParameters().put(PARAM_SINGLE_RPM_PER_DIR, Boolean.toString(singleRpmPerDirectory));
  }
}
