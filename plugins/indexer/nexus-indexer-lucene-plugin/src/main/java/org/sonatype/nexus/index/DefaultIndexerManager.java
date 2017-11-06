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
package org.sonatype.nexus.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.maven.tasks.SnapshotRemover;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.scheduling.TaskInterruptedException;
import org.sonatype.scheduling.TaskUtil;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.Throwables2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.maven.index.AndMultiArtifactInfoFilter;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.ArtifactInfoPostprocessor;
import org.apache.maven.index.Field;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.IteratorResultSet;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.MatchHighlightMode;
import org.apache.maven.index.MatchHighlightRequest;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.artifact.VersionUtils;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.MergedIndexingContext;
import org.apache.maven.index.context.StaticContextMemberProvider;
import org.apache.maven.index.expr.SearchExpression;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.packer.IndexPackingRequest.IndexFormat;
import org.apache.maven.index.treeview.IndexTreeView;
import org.apache.maven.index.treeview.TreeNode;
import org.apache.maven.index.treeview.TreeNodeFactory;
import org.apache.maven.index.treeview.TreeViewRequest;
import org.apache.maven.index.updater.FSDirectoryFactory;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.util.IndexCreatorSorter;

/**
 * <p>
 * Indexer Manager. This is a thin layer above Nexus Indexer and simply manages indexingContext additions, updates and
 * removals. Every Nexus repository (except ShadowRepository, which are completely left out of indexing) has two
 * indexing context maintained: local and remote. In case of hosted/proxy repositories, the local context contains the
 * content/cache content and the remote context contains nothing/downloaded index (if remote index download happened
 * and
 * remote peer is publishing index). In case of group reposes, the things are little different: their local context
 * contains the index of GroupRepository local storage, and remote context contains the merged indexes of it's member
 * repositories.
 * </p>
 * <p>
 * This indexer manager supports Maven2 repositories only (hosted/proxy/groups).
 * </p>
 * <p>
 * This indexer manager is (mostly) thread safe. Item add/remote operations and index queries do not block each other.
 * Repository add/remove, download remote index and reindex operations are serialized for each repository and may block
 * index queries and item add/remove operations.<br/>
 * The following resources require some level of coordination of concurrent access
 * <ul>
 * <li>Lucene index files. Even though opened Lucene index is thread-safe, some Maven Indexer operations either close
 * or
 * close/reopen Lucene index and manipulate Lucene index files on filesystem directly.</li>
 * <li>.gz index download area. If two or more threads will attempt to download the same remote index concurrently,
 * this
 * will be wasteful at best, but most likely result in corrupted downloaded index files.</li>
 * <li>.gz index publishing area. Similar to gz index download area, publishing gz index concurrently from multiple
 * threads will likely result in corrupted index files.</li>
 * <li>Repository local storage. Coordination is required between reindex local storage and other operations that
 * maintain consistency of index with local storage.</li>
 * </ul>
 * </p>
 * <p>
 * Access to Lucene index files is protected by <code>repositoryLocks</code> read-write locks. Operations that go
 * through normal Lucene API to access the index need to acquire read, i.e. shared, lock. Operations that manipulate
 * index files directly need to acquire write, i.e. exclusive, lock. Group repository index queries need to acquire
 * shared lock on all member repositories. Most index operations use shared(), sharedSingle(), exclusive() or
 * temporary() helper methods that acquire and release appropriate lock(s). <br/>
 * Methods that return search result iterator acquire shared lock(s) on involved repositories but the caller MUST close
 * the iterator in order to release the lock(s).<br/>
 * Methods that return TreeNode uses special read-only IndexingContext implementation that acquires/release shared
 * locks
 * on involved repositories as part of acquireIndexSearcher()/releaseIndexSearcher() logic. Additionally, the indexing
 * context implementation returns empty searcher if underlying Lucene index is closed by a concurrent thread. This
 * allows clients use TreeNode.listChildren without explicit lock/unlock calls (TreeNode does not have relevant
 * callbacks).
 * </p>
 * <p>
 * Access to gz index download and publishing areas and to repository local storage is protected by
 * <code>reindexLocks</code> reentrant locks.
 * </p>
 *
 * @author Tamas Cservenak
 */
@Named
@Singleton
public class DefaultIndexerManager
    extends ComponentSupport
    implements IndexerManager
{
  private static final String ARTIFICIAL_EXCEPTION =
      "This is an artificial exception that provides caller backtrace.";

  /**
   * The key used in working directory.
   */
  public static final String INDEXER_WORKING_DIRECTORY_KEY = "indexer";

  /**
   * Context id local suffix
   */
  public static final String CTX_SUFIX = "-ctx";

  /**
   * Path prefix where index publishing happens
   */
  public static final String PUBLISHING_PATH_PREFIX = "/.index";

  /**
   * Indexing is supported for this repository.
   */
  private boolean SUPPORTED(Repository repository) {
    return !repository.getRepositoryKind().isFacetAvailable(ShadowRepository.class)
        && repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)
        && repository.getRepositoryContentClass().isCompatible(maven2);
  }

  /**
   * Index is maintained for the repository. Implies SUPPORTED.
   */
  private boolean INDEXABLE(Repository repository) {
    return SUPPORTED(repository) && repository.isIndexable();
  }

  /**
   * The repository is in service for indexing purposes
   */
  private boolean INSERVICE(Repository repository) {
    return LocalStatus.IN_SERVICE.equals(repository.getLocalStatus());
  }

  /**
   * The repository is capable of remote access for indexing purposes.
   *
   * @since 2.7.0
   */
  private boolean REMOTEACCESSALLOWED(Repository repository) {
    final ProxyRepository proxyRepository = repository.adaptToFacet(ProxyRepository.class);
    if (proxyRepository != null) {
      return proxyRepository.getProxyMode().shouldProxy();
    }
    else {
      return false;
    }
  }

  /**
   * Repository is a proxy repository.
   */
  private boolean ISPROXY(Repository repository) {
    return repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class);
  }

  /**
   * Repository is a group repository.
   */
  private boolean ISGROUP(Repository repository) {
    return repository.getRepositoryKind().isFacetAvailable(GroupRepository.class);
  }

  private boolean INCLUDEINSEARCH(Repository repository) {
    return INSERVICE(repository) && (ISGROUP(repository) || (INDEXABLE(repository)));
  }

  @Inject
  private NexusIndexer mavenIndexer;

  @Inject
  private IndexUpdater indexUpdater;

  @Inject
  private IndexPacker indexPacker;

  @Inject
  private NexusConfiguration nexusConfiguration;

  @Inject
  private RepositoryRegistry repositoryRegistry;

  @Inject
  @Named("maven2")
  private ContentClass maven2;

  @Inject
  private List<IndexCreator> indexCreators;

  @Inject
  private IndexArtifactFilter indexArtifactFilter;

  @Inject
  private ArtifactContextProducer artifactContextProducer;

  @Inject
  private MimeSupport mimeSupport;

  @Inject
  private IndexTreeView indexTreeView;

  @Inject
  private Scanner scanner;

  /**
   * As of 3.6.1, Lucene provides three FSDirectory implementations, all with there pros and cons.
   * <ul>
   * <li>mmap -- {@link MMapDirectory}</li>
   * <li>nio -- {@link NIOFSDirectory}</li>
   * <li>simple -- {@link SimpleFSDirectory}</li>
   * </ul>
   * By default, Lucene selects FSDirectory implementation based on specifics of the operating system and JRE used,
   * but this configuration parameter allows override.
   */
  @Inject
  @Nullable
  @Named("${lucene.fsdirectory.type}")
  private String luceneFSDirectoryType;

  /**
   * Timeout, in seconds, acquiring index locks. This is a safety net meant to prevent complete system lockup in case
   * of index lock leaks.
   */
  @Inject
  @Named("${nexus.indexer.locktimeout:-60}")
  private int lockTimeoutSeconds;

  /**
   * Locks that protect access to repository index. Item-level add/remove and search operations must acquire read
   * lock. Index-level add/remove/reindex must acquire exclusive lock.
   * <p>
   * Note that locks are only added to the map, never removed. This introduces a minor memory leak for each deleted
   * repository id, but makes synchronization logic much easier.
   */
  private final Map<String, ReadWriteLock> repositoryLocks = new HashMap<String, ReadWriteLock>();

  /**
   * Locks that protect operations that keep repository index and local storage in sync, such as item add/remove and
   * reindex.
   * <p>
   * Note that locks are only added to the map, never removed. This introduces a minor memory leak for each deleted
   * repository id, but makes synchronization logic much easier.
   */
  private final Map<String, ForceableReentrantLock> reindexLocks = new HashMap<String, ForceableReentrantLock>();

  /**
   * Threads attempting to delete repository indexing contexts. Used as a marker to index requests for repositories
   * that are being deleted.
   */
  private final ConcurrentMap<String, Thread> deleteThreads = new ConcurrentHashMap<String, Thread>();

  private File workingDirectory;

  private File tempDirectory;

  private final FSDirectoryFactory luceneDirectoryFactory = new FSDirectoryFactory()
  {
    @Override
    public FSDirectory open(File indexDir)
        throws IOException
    {
      return openFSDirectory(indexDir);
    }
  };

  /**
   * Performs the same operation on all immediate members of a group repository. Exceptions thrown during processing
   * of individual members are collected and return to the caller. Does nothing if provided repository is not a group
   * repository.
   */
  private abstract class GroupOperation
  {
    private final Repository repository;

    public GroupOperation(Repository repository) {
      this.repository = repository;
    }

    public List<IOException> perform() {
      final List<IOException> exceptions = new ArrayList<IOException>();
      if (ISGROUP(repository)) {
        List<Repository> members = repository.adaptToFacet(GroupRepository.class).getMemberRepositories();
        for (Repository member : members) {
          TaskUtil.checkInterruption();
          try {
            perform(member);
          }
          catch (IOException e) {
            exceptions.add(e);
          }
        }
      }
      return exceptions;
    }

    /**
     * Operation to perform on individual member repositories.
     */
    protected abstract void perform(Repository member)
        throws IOException;
  }

  @VisibleForTesting
  protected void setIndexUpdater(final IndexUpdater indexUpdater) {
    this.indexUpdater = indexUpdater;
  }

  @VisibleForTesting
  protected void setScanner(final Scanner scanner) {
    this.scanner = scanner;
  }

  protected File getWorkingDirectory() {
    if (workingDirectory == null) {
      workingDirectory = nexusConfiguration.getWorkingDirectory(INDEXER_WORKING_DIRECTORY_KEY);
    }

    return workingDirectory;
  }

  protected File getTempDirectory() {
    if (tempDirectory == null) {
      tempDirectory = nexusConfiguration.getTemporaryDirectory();
    }
    return tempDirectory;
  }

  /**
   * Used to close all indexing context explicitly.
   */
  public void shutdown(boolean deleteFiles)
      throws IOException
  {
    log.info("Shutting down Nexus IndexerManager");

    for (IndexingContext ctx : mavenIndexer.getIndexingContexts().values()) {
      mavenIndexer.removeIndexingContext(ctx, false);
    }

    synchronized (repositoryLocks) {
      repositoryLocks.clear();
    }

    synchronized (reindexLocks) {
      reindexLocks.clear();
    }
  }

  public void resetConfiguration() {
    workingDirectory = null;

    tempDirectory = null;
  }

  // ----------------------------------------------------------------------------
  // Context management et al
  // ----------------------------------------------------------------------------

  public void addRepositoryIndexContext(String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    addRepositoryIndexContext(repository);
  }

  public void addRepositoryIndexContext(final Repository repository)
      throws IOException, NoSuchRepositoryException
  {
    if (!INDEXABLE(repository)) {
      return;
    }

    if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      // group repository
      // just to throw NoSuchRepositoryGroupException if not existing
      repositoryRegistry.getRepositoryWithFacet(repository.getId(), GroupRepository.class);
    }
    else {
      repositoryRegistry.getRepositoryWithFacet(repository.getId(), Repository.class);
    }

    exclusiveSingle(repository, new Runnable()
    {
      @Override
      public void run(IndexingContext context)
          throws IOException
      {
        addRepositoryIndexContext(repository, context);
      }
    });
  }

  private void addRepositoryIndexContext(final Repository repository, IndexingContext oldContext)
      throws IOException
  {
    log.debug("Adding indexing context for repository {}", repository.getId());

    if (oldContext != null) {
      // this is an error, oldContext can have filesystem locks or long-running threads
      log.error("Old/stale indexing context {} for repository {} will be removed.", oldContext.getId(),
          repository.getId());
      mavenIndexer.removeIndexingContext(oldContext, true);
    }

    IndexingContext ctx = null;

    File indexDirectory = getRepositoryIndexDirectory(repository);

    final File repoRoot = getRepositoryLocalStorageAsFile(repository);

    if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      // this is a marker context, it is not used for anything useful
      ctx =
          mavenIndexer.addMergedIndexingContext(getContextId(repository.getId()), repository.getId(),
              repoRoot, indexDirectory, repository.isSearchable(), Collections.<IndexingContext>emptyList());
    }
    else {
      // add context for repository
      ctx = new NexusIndexingContext(getContextId(repository.getId()), // id
          repository.getId(), // repositoryId
          repoRoot, // repository
          openFSDirectory(indexDirectory), // indexDirectory
          null, // repositoryUrl
          null, // indexUpdateUrl
          IndexCreatorSorter.sort(indexCreators), //
          true, // reclaimIndex
          ISPROXY(repository));
      mavenIndexer.addIndexingContext(ctx);
    }
    ctx.setSearchable(repository.isSearchable());

    log.debug("Added indexing context {} for repository {}", ctx.getId(), repository.getId());
  }

  private File getRepositoryIndexDirectory(final Repository repository) throws IOException {
    File indexDirectory = new File(getWorkingDirectory(), getContextId(repository.getId()));
    DirSupport.mkdir(indexDirectory.toPath());
    return indexDirectory;
  }

  public void removeRepositoryIndexContext(String repositoryId, final boolean deleteFiles)
      throws IOException, NoSuchRepositoryException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    removeRepositoryIndexContext(repository, deleteFiles);
  }

  public void removeRepositoryIndexContext(final Repository repository, final boolean deleteFiles)
      throws IOException
  {
    Thread otherThread = deleteThreads.putIfAbsent(repository.getId(), Thread.currentThread());
    if (otherThread != null) {
      log.debug("Indexing context for repository {} is being deleted by thread {}", repository.getId(),
          otherThread.getName());
      return;
    }

    try {
      final boolean[] removed = new boolean[1];
      final ForceableReentrantLock lock = getReindexLock(repository);
      if (lock.tryForceLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
        try {
          exclusiveSingle(repository, new Runnable()
          {
            @Override
            public void run(final IndexingContext context)
                throws IOException
            {
              removeRepositoryIndexingContext(repository, deleteFiles, context);
              removed[0] = true;
            }
          });
        }
        finally {
          lock.unlock();
        }
      }
      if (!removed[0]) {
        throw new IOException("Could not remove indexing context for repository " + repository.getId());
      }
    }
    finally {
      deleteThreads.remove(repository.getId());
    }
  }

  private void removeRepositoryIndexingContext(final Repository repository, final boolean deleteFiles,
                                               final IndexingContext context)
      throws IOException
  {
    if (context != null) {
      log.debug("Removing indexing context for repository {} deleteFiles={}", repository.getId(), deleteFiles);

      mavenIndexer.removeIndexingContext(context, deleteFiles);

      log.debug("Removed indexing context {} for repository {}", context.getId(), repository.getId());
    }
    else {
      log.debug("Could not remove <null> indexing context for repository {}", repository.getId());
    }
  }

  public void updateRepositoryIndexContext(final String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);

    // cannot do "!repository.isIndexable()" since we may be called to handle that config change (using events)!
    // the repo might be already non-indexable, but the context would still exist!
    if (!SUPPORTED(repository)) {
      return;
    }

    // the point of this if-else is to trigger NoSuchRepositoryException
    if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      // group repository
      repositoryRegistry.getRepositoryWithFacet(repositoryId, GroupRepository.class);
    }
    else {
      repositoryRegistry.getRepositoryWithFacet(repositoryId, Repository.class);
    }

    exclusiveSingle(repository, new Runnable()
    {
      @Override
      public void run(IndexingContext context)
          throws IOException
      {
        log.debug("Updating indexing context for repository {}", repository.getId());

        File repoRoot = getRepositoryLocalStorageAsFile(repository);

        // remove context, if it already existed (ctx != null) and any of the following is true:
        // is a group OR repo path changed OR we have an isIndexed transition happening
        if (context != null
            && (ISGROUP(repository) || !INDEXABLE(repository)
            || !context.getRepository().getAbsolutePath().equals(repoRoot.getAbsolutePath()) ||
            context.isSearchable() != repository.isSearchable())) {
          // remove the context
          removeRepositoryIndexContext(repository, false);
          context = null;
        }

        // add context, if it did not existed yet (ctx == null) or any of the following is true:
        // is a group OR repo path changed OR we have an isIndexed transition happening
        if (INDEXABLE(repository) && context == null) {
          // recreate the context
          try {
            addRepositoryIndexContext(repository);
          }
          catch (NoSuchRepositoryException e) {
            // this can only happen if the repository was removed or changed type by another thread
            log.debug("Could not add indexing context for repository {}", repositoryId, e);
          }
        }

        log.debug("Updated indexing context for repository {}", repository.getId());
      }
    });
  }

  /**
   * Returns "raw" unprotected repository IndexingContext. Most clients should use shared() or exclusive() methods to
   * manipulate repository indexes.
   *
   * @noreference this method is public for test purposes only
   */
  public IndexingContext getRepositoryIndexContext(Repository repository) {
    return mavenIndexer.getIndexingContexts().get(getContextId(repository.getId()));
  }

  /**
   * Extracts the repo root on local FS as File. It may return null!
   */
  protected File getRepositoryLocalStorageAsFile(Repository repository) {
    if (repository.getLocalUrl() != null
        && repository.getLocalStorage() instanceof DefaultFSLocalRepositoryStorage) {
      try {
        File baseDir =
            ((DefaultFSLocalRepositoryStorage) repository.getLocalStorage()).getBaseDir(repository,
                new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT));

        return baseDir;
      }
      catch (LocalStorageException e) {
        log.warn(String.format("Cannot determine \"%s\" (ID=%s) repository's basedir:",
            repository.getName(), repository.getId()), e);
      }
    }

    return null;
  }

  // ----------------------------------------------------------------------------
  // Publish the used NexusIndexer
  // ----------------------------------------------------------------------------

  protected NexusIndexer getNexusIndexer() {
    return mavenIndexer;
  }

  // ----------------------------------------------------------------------------
  // adding/removing on the fly
  // ----------------------------------------------------------------------------
  public void addItemToIndex(final Repository repository, final StorageItem item)
      throws IOException
  {
    if (!INDEXABLE(repository) || !INSERVICE(repository)) {
      return;
    }

    // is this hidden path?
    if (item.getRepositoryItemUid().getBooleanAttributeValue(IsHiddenAttribute.class)) {
      return;
    }

    // never index generated items
    if (item instanceof StorageFileItem && ((StorageFileItem) item).isContentGenerated()) {
      return;
    }

    // by calculating GAV we check whether the request is against a repo artifact at all
    // signatures and hashes are not considered for processing
    // reason (NEXUS-814 related): the actual artifact and it's POM will (or already did)
    // emitted events about modifying them
    Gav gav = ((MavenRepository) repository).getGavCalculator().pathToGav(item.getRepositoryItemUid().getPath());
    if (gav == null || gav.isSignature() || gav.isHash()) {
      return;
    }

    // do the work
    // Maybe detect Merged context and NOT do the work? Everything works transparently, but still... a lot of calls
    // for nothing

    sharedSingle(repository, new Runnable()
    {
      @Override
      public void run(IndexingContext context)
          throws IOException
      {
        addItemToIndex(repository, item, context);
      }
    });
  }

  private void addItemToIndex(Repository repository, StorageItem item, IndexingContext context)
      throws LocalStorageException, IOException
  {
    final RepositoryItemUidLock uidLock = item.getRepositoryItemUid().getLock();

    uidLock.lock(Action.read);

    try {

      ArtifactContext ac = null;

      // if we have a valid indexing context and have access to a File
      if (DefaultFSLocalRepositoryStorage.class.isAssignableFrom(repository.getLocalStorage().getClass())) {
        File file =
            ((DefaultFSLocalRepositoryStorage) repository.getLocalStorage()).getFileFromBase(repository,
                new ResourceStoreRequest(item));

        if (file.exists()) {
          try {
            ac = artifactContextProducer.getArtifactContext(context, file);
          }
          catch (IllegalArgumentException e) {
            // cannot create artifact context, forget it
            return;
          }

          if (ac != null) {
            if (log.isDebugEnabled()) {
              log.debug("The ArtifactContext created from file is fine, continuing.");
            }

            ArtifactInfo ai = ac.getArtifactInfo();

            if (ai.sha1 == null) {
              // if repo has no sha1 checksum, odd nexus one
              ai.sha1 =
                  item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY);
            }
          }
        }
      }

      // and finally: index it
      getNexusIndexer().addArtifactToIndex(ac, context);
    }
    finally {
      uidLock.unlock();
    }
  }

  public void removeItemFromIndex(final Repository repository, final StorageItem item)
      throws IOException
  {
    if (!INDEXABLE(repository) || !INSERVICE(repository)) {
      return;
    }

    // index for proxy repos shouldn't change just because you deleted something locally
    if (ISPROXY(repository)) {
      return;
    }

    // do the work
    sharedSingle(repository, new Runnable()
    {
      @Override
      public void run(IndexingContext context)
          throws IOException
      {
        removeItemFromIndex(repository, item, context);
      }
    });
  }

  private void removeItemFromIndex(Repository repository, StorageItem item, IndexingContext context)
      throws IOException
  {
    // by calculating GAV we check wether the request is against a repo artifact at all
    Gav gav = null;

    gav = ((MavenRepository) repository).getGavCalculator().pathToGav(item.getRepositoryItemUid().getPath());

    // signatures and hashes are not considered for processing
    // reason (NEXUS-814 related): the actual artifact and it's POM will (or already did)
    // emitted events about modifying them
    if (gav == null || gav.isSignature() || gav.isHash()) {
      return;
    }

    ArtifactInfo ai =
        new ArtifactInfo(context.getRepositoryId(), gav.getGroupId(), gav.getArtifactId(), gav.getBaseVersion(),
            gav.getClassifier());

    // store extension if classifier is not empty
    if (!Strings.isNullOrEmpty(ai.classifier)) {
      ai.packaging = gav.getExtension();
    }

    ArtifactContext ac = null;

    // we need to convert Nexus Gav to Indexer Gav
    org.apache.maven.index.artifact.Gav igav = GavUtils.convert(gav);

    try {
      ac = new ArtifactContext(null, null, null, ai, igav);
    }
    catch (IllegalArgumentException e) {
      // ac cannot be created, just forget it being indexed
      return;
    }

    // remove file from index
    if (log.isDebugEnabled()) {
      log.debug("Deleting artifact " + ai.groupId + ":" + ai.artifactId + ":" + ai.version
          + " from index (DELETE).");
    }

    // NEXUS-814: we should not delete always
    if (!item.getItemContext().containsKey(SnapshotRemover.MORE_TS_SNAPSHOTS_EXISTS_FOR_GAV)) {
      final RepositoryItemUidLock uidLock = item.getRepositoryItemUid().getLock();

      uidLock.lock(Action.read);

      try {
        getNexusIndexer().deleteArtifactFromIndex(ac, context);
      }
      finally {
        uidLock.unlock();
      }
    }
    else {
      // do NOT remove file from index
      if (log.isDebugEnabled()) {
        log.debug("NOT deleting artifact " + ac.getArtifactInfo().groupId + ":"
            + ac.getArtifactInfo().artifactId + ":" + ac.getArtifactInfo().version
            + " from index (DELETE), since it is a timestamped snapshot and more builds exists.");
      }
    }
  }

  // ----------------------------------------------------------------------------
  // TODO: NEXUS-4052 and NEXUS-4053
  // when sorted out, these constants will help the change, just remove them

  // reindex() method does publishing too (currently yes)
  private static final boolean REINDEX_PUBLISHES = true;

  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // Reindexing related
  // ----------------------------------------------------------------------------

  public void reindexAllRepositories(final String fromPath, final boolean fullReindex)
      throws IOException
  {
    log.debug("Reindexing all repositories fromPath={} fullReindex={}", fromPath, fullReindex);

    final List<Repository> reposes = repositoryRegistry.getRepositories();
    final ArrayList<IOException> exceptions = new ArrayList<IOException>();
    for (Repository repository : reposes) {
      TaskUtil.checkInterruption();
      try {
        // going directly to single-shot, we are iterating over all reposes anyway
        reindexRepository(repository, fromPath, fullReindex);
      }
      catch (IOException e) {
        exceptions.add(e);
      }
    }
    // this has to happen after _every_ reindex happened,
    // as otherwise publish of a group might publish index
    // containing a member that is not yet updated
    if (REINDEX_PUBLISHES) {
      for (Repository repository : reposes) {
        TaskUtil.checkInterruption();
        try {
          publishRepositoryIndex(repository);
        }
        catch (IOException e) {
          exceptions.add(e);
        }
      }
    }

    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during reindexAllRepositories()"), exceptions);
    }
  }

  public void reindexRepository(final String path, final String repositoryId, final boolean fullReindex)
      throws NoSuchRepositoryException, IOException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    reindexRepository(path, repository, fullReindex, new HashSet<String>());
  }

  protected void reindexRepository(final String path, final Repository repository, final boolean fullReindex,
                                   final Set<String> processedRepositoryIds)
      throws IOException
  {
    if (!processedRepositoryIds.add(repository.getId())) {
      // already processed, bail out
      return;
    }

    final List<IOException> exceptions = new GroupOperation(repository)
    {
      @Override
      protected void perform(Repository member)
          throws IOException
      {
        reindexRepository(path, member, fullReindex, processedRepositoryIds);
      }
    }.perform();

    TaskUtil.checkInterruption();
    reindexRepository(repository, path, fullReindex);

    if (REINDEX_PUBLISHES) {
      publishRepositoryIndex(repository);
    }
    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during reindexAllRepositories()"), exceptions);
    }
  }

  /**
   * Updates repository index from remote repository and local storage. Group repositories are silently ignored. This
   * method dos NOT publish index.
   * <p>
   * This method acquires necessary repository lock but the caller is required to acquire reindex lock.
   */
  private void reindexRepository(final Repository repository, final String fromPath, final boolean fullReindex)
      throws IOException
  {
    if (!INDEXABLE(repository) || !INSERVICE(repository)) {
      return;
    }

    if (ISGROUP(repository)) {
      if (fullReindex) {
        deleteIndexItems(repository); // groups must reset the chain too
      }
      return;
    }

    ForceableReentrantLock reindexLock = getReindexLock(repository);
    if (reindexLock.tryLock()) {
      try {
        log.debug("Reindexing repository {} fromPath={} fullReindex={}", repository.getId(), fromPath,
            fullReindex);

        if (!fullReindex) {
          //Try incremental update first
          try {
            Runnable runnable = new IndexUpdateRunnable(repository, fromPath, false);
            sharedSingle(repository, runnable);
            log.debug("Reindexed repository {}", repository.getId());
            return;
          }
          catch (IncrementalIndexUpdateException e) {
            //This exception is an indication that an incremental
            //update is not possible, and a full update is necessary
            log.info("Unable to incrementally update index for repository {}. Trying full index update",
                repository.getId());

            //Let execution continue to below to try full index update
          }
        }

        //Perform full index update
        Runnable runnable = new IndexUpdateRunnable(repository, fromPath, true);

        // delete published stuff too, as we are breaking incremental downstream chain
        deleteIndexItems(repository);

        // creates a temp ctx and finally replaces the "real" with temp
        temporary(repository, runnable);

        log.debug("Reindexed repository {}", repository.getId());
      }
      finally {
        reindexLock.unlock();
      }
    }
    else {
      log.info(
          "Repository '{}' is already in the process of being re-indexed. Skipping additional reindex requests.",
          repository.getId());
    }
  }

  /**
   * Runnable implementation for updating an index
   */
  private class IndexUpdateRunnable
      implements Runnable
  {
    Repository repository;

    String fromPath;

    boolean fullReindex;

    IndexUpdateRunnable(Repository repository, String fromPath, boolean fullReindex) {
      this.repository = repository;
      this.fromPath = fromPath;
      this.fullReindex = fullReindex;
    }

    @Override
    public void run(final IndexingContext context)
        throws IOException
    {
      if (ISPROXY(this.repository)) {
        updateRemoteIndex(this.repository.adaptToFacet(ProxyRepository.class), context, this.fullReindex);
      }

      TaskUtil.checkInterruption();

      // igorf, this needs be merged back to maven indexer, see MINDEXER-65
      final IndexSearcher contextIndexSearcher = context.acquireIndexSearcher();
      try {
        final NexusScanningListener scanListener =
            new NexusScanningListener(context, contextIndexSearcher, this.fullReindex,
                ISPROXY(this.repository));
        scanner.scan(new ScanningRequest(context, scanListener, this.fromPath));
      }
      finally {
        context.releaseIndexSearcher(contextIndexSearcher);
      }
    }
  }

  // ----------------------------------------------------------------------------
  // Downloading remote indexes (will do remote-download, merge only)
  // ----------------------------------------------------------------------------

  public void downloadAllIndex()
      throws IOException
  {
    log.debug("Downloading remote indexes for all repositories");

    final List<ProxyRepository> reposes = repositoryRegistry.getRepositoriesWithFacet(ProxyRepository.class);
    final ArrayList<IOException> exceptions = new ArrayList<IOException>();
    for (ProxyRepository repository : reposes) {
      try {
        downloadRepositoryIndex(repository, false);
      }
      catch (IOException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during downloadAllIndex()"), exceptions);
    }
  }

  public void downloadRepositoryIndex(final String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    downloadRepositoryIndex(repository, new HashSet<String>());
  }

  public void downloadRepositoryIndex(final Repository repository, final Set<String> processedRepositoryIds)
      throws IOException
  {
    if (!processedRepositoryIds.add(repository.getId())) {
      // already processed, bail out
      return;
    }

    final List<IOException> exceptions = new GroupOperation(repository)
    {
      @Override
      protected void perform(Repository member)
          throws IOException
      {
        downloadRepositoryIndex(member, processedRepositoryIds);
      }
    }.perform();

    if (ISPROXY(repository)) {
      TaskUtil.checkInterruption();
      downloadRepositoryIndex(repository.adaptToFacet(ProxyRepository.class), false);
    }
    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during reindexAllRepositories()"), exceptions);
    }
  }

  protected void downloadRepositoryIndex(final ProxyRepository repository, final boolean forceFullUpdate)
      throws IOException
  {
    TaskUtil.checkInterruption();

    if (!INDEXABLE(repository) || !INSERVICE(repository) || !ISPROXY(repository)) {
      return;
    }

    ForceableReentrantLock reindexLock = getReindexLock(repository);
    if (reindexLock.tryLock()) {
      try {
        if (!forceFullUpdate) {
          //Try incremental update first
          try {
            Runnable runnable = new Runnable()
            {
              @Override
              public void run(IndexingContext context)
                  throws IOException
              {
                updateRemoteIndex(repository, context, false);
              }
            };

            sharedSingle(repository, runnable);
            return;
          }
          catch (IncrementalIndexUpdateException e) {
            //This exception is an indication that an incremental
            //update is not possible, and a full update is necessary
            log.info("Unable to incrementally update index for repository {}. Trying full index update",
                repository.getId());

            //Let execution continue to below to try full index update
          }
        }

        //If we're here, either a full update was requested, or incremental failed
        //Try full index update

        Runnable runnable = new Runnable()
        {
          @Override
          public void run(IndexingContext context)
              throws IOException
          {
            updateRemoteIndex(repository, context, true);
          }
        };

        temporary(repository, runnable);
      }
      finally {
        reindexLock.unlock();
      }
    }
    else {
      log.info(
          "Repository '%s' is already in the process of being re-indexed. Skipping additional download index requests.",
          repository.getId());
    }
  }

  /**
   * Downloads full or incremental remote index and applies it to the provided indexing context. Callers are expected
   * to acquire all required repository and download area locks.
   *
   * @return true if index was updated, false otherwise.
   */
  private void updateRemoteIndex(final ProxyRepository repository, final IndexingContext context,
                                 final boolean forceFullUpdate)
      throws IOException
  {
    // ensure this is a proxy repo, since download may happen with proxies only
    if (!INDEXABLE(repository) || !ISPROXY(repository)) {
      return;
    }

    if (!repository.adaptToFacet(MavenProxyRepository.class).isDownloadRemoteIndexes()) {
      return;
    }

    log.info(RepositoryStringUtils.getFormattedMessage("Trying to get remote index for repository %s",
        repository));

    // this will force remote check for newer files
    repository.expireCaches(new ResourceStoreRequest(PUBLISHING_PATH_PREFIX));

    IndexUpdateRequest updateRequest = new IndexUpdateRequest(context, new ResourceFetcher()
    {
      public void connect(String id, String url)
          throws IOException
      {
      }

      public void disconnect()
          throws IOException
      {
      }

      public InputStream retrieve(String name)
          throws IOException
      {
        TaskUtil.checkInterruption();

        ResourceStoreRequest req = new ResourceStoreRequest(PUBLISHING_PATH_PREFIX + "/" + name);

        try {
          StorageFileItem item = null;

          // XXX: ensure it goes to remote only and throws FileNotFoundException if nothing found on remote
          // kinda turn off transparent proxying for this method
          // We need to use ProxyRepository and get it's RemoteStorage stuff to completely
          // avoid "transparent" proxying, and even the slightest possibility to return
          // some stale file from cache to the updater.
          if (ISPROXY(repository) && REMOTEACCESSALLOWED(repository)) {
            item =
                (StorageFileItem) repository.getRemoteStorage()
                    .retrieveItem(repository, req, repository.getRemoteUrl());
          }
          else {
            throw new ItemNotFoundException(req, repository);
          }

          return item.getInputStream();
        }
        catch (ItemNotFoundException ex) {
          final FileNotFoundException fne = new FileNotFoundException(name + " (remote item not found)");
          fne.initCause(ex);
          throw fne;
        }
      }
    });

    //Set request for either full or incremental-only update
    if (forceFullUpdate) {
      updateRequest.setForceFullUpdate(true);
      updateRequest.setIncrementalOnly(false);
    }
    else {
      updateRequest.setForceFullUpdate(false);
      updateRequest.setIncrementalOnly(true);
    }

    updateRequest.setFSDirectoryFactory(luceneDirectoryFactory);

    if (repository instanceof MavenRepository) {
      MavenRepository mrepository = (MavenRepository) repository;

      updateRequest.setDocumentFilter(getFilterFor(mrepository.getRepositoryPolicy()));
    }

    try {
      IndexUpdateResult result = indexUpdater.fetchAndUpdateIndex(updateRequest);

      //Check if successful
      if (!result.isSuccessful()) {
        //This condition occurs when we have requested an incremental-only update,
        //but it could not be completed. In this case, we need to request a full update
        //This needs to be communicated upstream so that the proper locking can take place
        throw new IncrementalIndexUpdateException("Cannot incrementally update index. Request a full update");
      }
      boolean hasRemoteIndexUpdate = result.getTimestamp() != null;

      if (hasRemoteIndexUpdate) {
        log.info(RepositoryStringUtils.getFormattedMessage(
            "Remote indexes updated successfully for repository %s", repository));
      }
      else {
        log.info(RepositoryStringUtils.getFormattedMessage(
            "Remote indexes unchanged (no update needed) for repository %s", repository));
      }
    }
    catch (FileNotFoundException e) {
      // here, FileNotFoundException literally means ResourceFetcher -- that is HTTP based -- hit a 404 on
      // remote, so we neglect this, this is not an error state actually
      if (log.isDebugEnabled()) {
        log.info(RepositoryStringUtils.getFormattedMessage(
            "Cannot fetch remote index for repository %s as it does not publish indexes.", repository), e);
      }
      else {
        log.info(RepositoryStringUtils.getFormattedMessage(
            "Cannot fetch remote index for repository %s as it does not publish indexes.", repository));
      }
    }
    catch (TaskInterruptedException e) {
      log.warn(RepositoryStringUtils.getFormattedMessage(
          "Cannot fetch remote index for repository %s, task cancelled.", repository));
    }
    catch (IncrementalIndexUpdateException e) {
      //This is an indication that an incremental index update is not possible, and a full index
      //update must be performed.
      //Just log this, and pass this exception upstream so that it can be handled appropriately
      log.info("Cannot incrementally update index for repository {}", repository.getId());
      throw e;
    }
    catch (IOException e) {
      log.warn(RepositoryStringUtils.getFormattedMessage(
          "Cannot fetch remote index for repository %s due to IO problem.", repository), e);
      throw e;
    }
    catch (Exception e) {
      final String message =
          RepositoryStringUtils.getFormattedMessage(
              "Cannot fetch remote index for repository %s, error occurred.", repository);
      log.warn(message, e);
      throw new IOException(message, e);
    }
  }

  // TODO Toni Prior Snownexus, this was contained in RepositoryPolicy split to separate concerns (NEXUS-2872)
  private DocumentFilter getFilterFor(final RepositoryPolicy repositoryPolicy) {
    return new DocumentFilter()
    {
      public boolean accept(Document doc) {
        String uinfo = doc.get(ArtifactInfo.UINFO);

        if (uinfo == null) {
          return true;
        }

        String[] r = ArtifactInfo.FS_PATTERN.split(uinfo);
        if (repositoryPolicy == RepositoryPolicy.SNAPSHOT) {
          return VersionUtils.isSnapshot(r[2]);
        }
        else if (repositoryPolicy == RepositoryPolicy.RELEASE) {
          return !VersionUtils.isSnapshot(r[2]);
        }
        else {
          return true;
        }
      }
    };
  }

  // ----------------------------------------------------------------------------
  // Publishing index (will do publish only)
  // ----------------------------------------------------------------------------

  public void publishAllIndex()
      throws IOException
  {
    log.debug("Publishing indexes for all repositories");

    final List<Repository> reposes = repositoryRegistry.getRepositories();
    final ArrayList<IOException> exceptions = new ArrayList<IOException>();
    // just publish all, since we use merged context, no need for double pass
    for (Repository repository : reposes) {
      TaskUtil.checkInterruption();
      try {
        publishRepositoryIndex(repository);
      }
      catch (IOException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during publishAllIndex()"), exceptions);
    }
  }

  public void publishRepositoryIndex(final String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    publishRepositoryIndex(repository, new HashSet<String>());
  }

  protected void publishRepositoryIndex(final Repository repository, final Set<String> processedRepositoryIds)
      throws IOException
  {
    if (!processedRepositoryIds.add(repository.getId())) {
      // already processed, bail out
      return;
    }

    final List<IOException> exceptions = new GroupOperation(repository)
    {
      @Override
      protected void perform(Repository member)
          throws IOException
      {
        publishRepositoryIndex(member, processedRepositoryIds);
      }
    }.perform();

    TaskUtil.checkInterruption();
    publishRepositoryIndex(repository);
    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during reindexAllRepositories()"), exceptions);
    }
  }

  protected void publishRepositoryIndex(final Repository repository)
      throws IOException
  {
    if (!INDEXABLE(repository) || !INSERVICE(repository)) {
      return;
    }

    ForceableReentrantLock reindexLock = getReindexLock(repository);
    if (reindexLock.tryLock()) {
      try {
        shared(repository, new Runnable()
        {
          @Override
          public void run(IndexingContext context)
              throws IOException
          {
            publishRepositoryIndex(repository, context);
          }
        });
      }
      finally {
        reindexLock.unlock();
      }
    }
    else {
      log.info(
          "Repository '{}' is already in the process of being re-indexed. Skipping additional publish index requests.",
          repository.getId());
    }
  }

  private void publishRepositoryIndex(final Repository repository, IndexingContext context)
      throws IOException
  {
    log.debug("Publishing index for repository {}", repository.getId());

    File targetDir = null;

    try {
      TaskUtil.checkInterruption();

      log.info("Publishing index for repository " + repository.getId());

      targetDir = new File(getTempDirectory(), "nx-index-" + Long.toHexString(System.nanoTime()));

      DirSupport.mkdir(targetDir.toPath());

      IndexPackingRequest packReq = new IndexPackingRequest(context, targetDir);
      packReq.setCreateIncrementalChunks(true);

      // not publishing legacy format anymore
      packReq.setFormats(Arrays.asList(IndexFormat.FORMAT_V1));
      indexPacker.packIndex(packReq);

      File[] files = targetDir.listFiles();

      if (files != null) {
        for (File file : files) {
          TaskUtil.checkInterruption();

          storeIndexItem(repository, file, context);
        }
      }

      log.debug("Published index for repository {}", repository.getId());
    }
    finally {
      Exception lastException = null;

      if (targetDir != null) {
        try {
          if (log.isDebugEnabled()) {
            log.debug("Cleanup of temp files...");
          }

          DirSupport.deleteIfExists(targetDir.toPath());
        }
        catch (IOException e) {
          lastException = e;

          log.warn("Cleanup of temp files FAILED...", e);
        }
      }

      if (lastException != null) {
        IOException eek = new IOException(lastException);

        throw eek;
      }
    }
  }

  @SuppressWarnings("deprecation")
  protected void deleteIndexItems(Repository repository) {
    ResourceStoreRequest request = new ResourceStoreRequest(PUBLISHING_PATH_PREFIX);

    try {
      // kill the chain, deleting the props file basically resets the chain
      shared(repository, new Runnable() {
        @Override
        public void run(IndexingContext context) throws IOException {
          if (context.getIndexDirectoryFile() != null) {
            new File( context.getIndexDirectoryFile(), IndexingContext.INDEX_PACKER_PROPERTIES_FILE ).delete();
          }
        }});
      repository.deleteItem(false, request);
    }
    catch (ItemNotFoundException e) {
      // nothing serious, no index was published yet, keep it silent
    }
    catch (Exception e) {
      log.error("Cannot delete index items!", e);
    }
  }

  protected void storeIndexItem(Repository repository, File file, IndexingContext context) {
    String path = PUBLISHING_PATH_PREFIX + "/" + file.getName();

    try {
      ResourceStoreRequest request = new ResourceStoreRequest(path);
      DefaultStorageFileItem fItem =
          new DefaultStorageFileItem(repository, request, true, true, new FileContentLocator(file,
              mimeSupport.guessMimeTypeFromPath(repository.getMimeRulesSource(), file.getAbsolutePath())));

      if (context.getTimestamp() == null) {
        fItem.setModified(0);

        fItem.setCreated(0);
      }
      else {
        fItem.setModified(context.getTimestamp().getTime());

        fItem.setCreated(context.getTimestamp().getTime());
      }

      if (repository instanceof MavenRepository) {
        // this is maven repo, so use the checksumming facility
        ((MavenRepository) repository).storeItemWithChecksums(false, fItem);
      }
      else {
        // simply store it
        repository.storeItem(false, fItem);
      }
    }
    catch (Exception e) {
      log.error("Cannot store index file " + path, e);
    }
  }

  // ----------------------------------------------------------------------------
  // Optimize
  // ----------------------------------------------------------------------------

  public void optimizeAllRepositoriesIndex()
      throws IOException
  {
    log.debug("Optimizing indexes for all repositories");

    final List<Repository> repos = repositoryRegistry.getRepositories();
    final ArrayList<IOException> exceptions = new ArrayList<IOException>();
    for (Repository repository : repos) {
      TaskUtil.checkInterruption();
      try {
        optimizeRepositoryIndex(repository);
      }
      catch (IOException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during optimizeAllRepositoriesIndex()"),
          exceptions);
    }
  }

  public void optimizeRepositoryIndex(final String repositoryId)
      throws NoSuchRepositoryException, IOException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    optimizeIndex(repository, new HashSet<String>());
  }

  protected void optimizeIndex(final Repository repository, final Set<String> processedRepositoryIds)
      throws CorruptIndexException, IOException
  {
    if (!processedRepositoryIds.add(repository.getId())) {
      // already processed, bail out
      return;
    }

    final List<IOException> exceptions = new GroupOperation(repository)
    {
      @Override
      protected void perform(Repository member)
          throws IOException
      {
        optimizeIndex(member, processedRepositoryIds);
      }
    }.perform();

    TaskUtil.checkInterruption();
    optimizeRepositoryIndex(repository);
    if (!exceptions.isEmpty()) {
      throw Throwables2.composite(new IOException("Exception(s) happened during reindexAllRepositories()"), exceptions);
    }
  }

  protected void optimizeRepositoryIndex(final Repository repository)
      throws CorruptIndexException, IOException
  {
    if (!INDEXABLE(repository)) {
      return;
    }

    // this does not do much useful with maven-indexer 5.0 and lucene 3.6+
    // and according to lucene javadoc should be fully thread-safe
    sharedSingle(repository, new Runnable()
    {
      @Override
      public void run(IndexingContext context)
          throws IOException
      {
        TaskUtil.checkInterruption();

        log.debug("Optimizing index for repository {} ", repository.getId());

        context.optimize();

        log.debug("Optimized index for repository {} ", repository.getId());
      }
    });
  }

  // ----------------------------------------------------------------------------
  // Identify
  // ----------------------------------------------------------------------------

  public Collection<ArtifactInfo> identifyArtifact(Field field, String data)
      throws IOException
  {
    return mavenIndexer.identify(field, data);
  }

  // ----------------------------------------------------------------------------
  // Combined searching
  // ----------------------------------------------------------------------------

  @Deprecated
  public FlatSearchResponse searchArtifactFlat(String term, String repositoryId, Integer from, Integer count,
                                               Integer hitLimit)
      throws NoSuchRepositoryException
  {
    Query q1 = mavenIndexer.constructQuery(MAVEN.GROUP_ID, term, SearchType.SCORED);

    Query q2 = mavenIndexer.constructQuery(MAVEN.ARTIFACT_ID, term, SearchType.SCORED);

    BooleanQuery bq = new BooleanQuery();

    bq.add(q1, BooleanClause.Occur.SHOULD);

    bq.add(q2, BooleanClause.Occur.SHOULD);

    FlatSearchRequest req = new FlatSearchRequest(bq, ArtifactInfo.REPOSITORY_VERSION_COMPARATOR);

    // if ( from != null )
    // {
    // req.setStart( from );
    // }

    // MINDEXER-14: no hit limit anymore. But to make change least obtrusive, we set hitLimit as count 1st, and if
    // user set count, it will override it anyway
    if (hitLimit != null) {
      req.setCount(hitLimit);
    }

    if (count != null) {
      req.setCount(count);
    }

    // if ( hitLimit != null )
    // {
    // req._setResultHitLimit( hitLimit );
    // }

    return searchFlat(repositoryId, req);
  }

  @Deprecated
  public FlatSearchResponse searchArtifactClassFlat(String term, String repositoryId, Integer from, Integer count,
                                                    Integer hitLimit)
      throws NoSuchRepositoryException
  {
    if (term.endsWith(".class")) {
      term = term.substring(0, term.length() - 6);
    }

    Query q = mavenIndexer.constructQuery(MAVEN.CLASSNAMES, term, SearchType.SCORED);

    FlatSearchRequest req = new FlatSearchRequest(q, ArtifactInfo.REPOSITORY_VERSION_COMPARATOR);

    // if ( from != null )
    // {
    // req.setStart( from );
    // }

    // MINDEXER-14: no hit limit anymore. But to make change least obtrusive, we set hitLimit as count 1st, and if
    // user set count, it will override it anyway
    if (hitLimit != null) {
      req.setCount(hitLimit);
    }

    if (count != null) {
      req.setCount(count);
    }

    // if ( hitLimit != null )
    // {
    // req._setResultHitLimit( hitLimit );
    // }

    return searchFlat(repositoryId, req);
  }

  @Deprecated
  public FlatSearchResponse searchArtifactFlat(String gTerm, String aTerm, String vTerm, String pTerm, String cTerm,
                                               String repositoryId, Integer from, Integer count, Integer hitLimit)
      throws NoSuchRepositoryException
  {
    if (gTerm == null && aTerm == null && vTerm == null) {
      return new FlatSearchResponse(null, -1, new HashSet<ArtifactInfo>());
    }

    BooleanQuery bq = new BooleanQuery();

    if (gTerm != null) {
      bq.add(constructQuery(MAVEN.GROUP_ID, gTerm, SearchType.SCORED), BooleanClause.Occur.MUST);
    }

    if (aTerm != null) {
      bq.add(constructQuery(MAVEN.ARTIFACT_ID, aTerm, SearchType.SCORED), BooleanClause.Occur.MUST);
    }

    if (vTerm != null) {
      bq.add(constructQuery(MAVEN.VERSION, vTerm, SearchType.SCORED), BooleanClause.Occur.MUST);
    }

    if (pTerm != null) {
      bq.add(constructQuery(MAVEN.PACKAGING, pTerm, SearchType.SCORED), BooleanClause.Occur.MUST);
    }

    if (cTerm != null) {
      bq.add(constructQuery(MAVEN.CLASSIFIER, cTerm, SearchType.SCORED), BooleanClause.Occur.MUST);
    }

    FlatSearchRequest req = new FlatSearchRequest(bq, ArtifactInfo.REPOSITORY_VERSION_COMPARATOR);

    // if ( from != null )
    // {
    // req.setStart( from );
    // }

    // MINDEXER-14: no hit limit anymore. But to make change least obtrusive, we set hitLimit as count 1st, and if
    // user set count, it will override it anyway
    if (hitLimit != null) {
      req.setCount(hitLimit);
    }

    if (count != null) {
      req.setCount(count);
    }

    // if ( hitLimit != null )
    // {
    // req._setResultHitLimit( hitLimit );
    // }

    return searchFlat(repositoryId, req);
  }

  @Deprecated
  protected void postprocessResults(Collection<ArtifactInfo> res) {
    for (Iterator<ArtifactInfo> i = res.iterator(); i.hasNext(); ) {
      ArtifactInfo ai = i.next();

      if (indexArtifactFilter.filterArtifactInfo(ai)) {
        ai.context = formatContextId(ai);
      }
      else {
        // remove the artifact, the user does not have access to it
        i.remove();
      }
    }
  }

  @Deprecated
  protected String formatContextId(ArtifactInfo ai) {
    String result = ai.context;

    try {
      Repository sourceRepository = repositoryRegistry.getRepository(ai.repository);

      result = sourceRepository.getName();
    }
    catch (NoSuchRepositoryException e) {
      // nothing
    }

    return result;
  }

  // == NG stuff

  protected IteratorSearchRequest createRequest(Query bq, Integer from, Integer count, Integer hitLimit,
                                                boolean uniqueRGA, List<ArtifactInfoFilter> extraFilters)
  {
    IteratorSearchRequest req = new IteratorSearchRequest(bq);

    List<ArtifactInfoFilter> filters = new ArrayList<ArtifactInfoFilter>();

    // security filter
    filters.add(new ArtifactInfoFilter()
    {
      public boolean accepts(IndexingContext ctx, ArtifactInfo ai) {
        return indexArtifactFilter.filterArtifactInfo(ai);
      }
    });

    if (extraFilters != null && extraFilters.size() > 0) {
      filters.addAll(extraFilters);
    }

    req.setArtifactInfoFilter(new AndMultiArtifactInfoFilter(filters));

    if (uniqueRGA) {
      req.setArtifactInfoPostprocessor(new ArtifactInfoPostprocessor()
      {
        public void postprocess(IndexingContext ctx, ArtifactInfo ai) {
          ai.context = "Aggregated";
          ai.repository = null;
        }
      });
    }
    else {
      // we may do this only when !uniqueRGA, otherwise UniqueGAArtifactFilterPostprocessor nullifies
      // ai.repository and ai.context
      req.setArtifactInfoPostprocessor(new ArtifactInfoPostprocessor()
      {
        public void postprocess(IndexingContext ctx, ArtifactInfo ai) {
          String result = ai.context;

          try {
            Repository sourceRepository = repositoryRegistry.getRepository(ai.repository);

            result = sourceRepository.getName();
          }
          catch (NoSuchRepositoryException e) {
            // nothing
          }

          ai.context = result;
        }
      });
    }

    if (from != null) {
      req.setStart(from);
    }

    // MINDEXER-14: no hit limit anymore. But to make change least obtrusive, we set hitLimit as count 1st, and if
    // user set count, it will override it anyway
    if (hitLimit != null) {
      req.setCount(hitLimit);
    }

    if (count != null) {
      req.setCount(count);
    }

    return req;
  }

  public IteratorSearchResponse searchQueryIterator(Query query, String repositoryId, Integer from, Integer count,
                                                    Integer hitLimit, boolean uniqueRGA,
                                                    List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    IteratorSearchRequest req = createRequest(query, from, count, hitLimit, uniqueRGA, filters);

    return searchIterator(repositoryId, req);
  }

  public IteratorSearchResponse searchArtifactIterator(String term, String repositoryId, Integer from,
                                                       Integer count, Integer hitLimit, boolean uniqueRGA,
                                                       SearchType searchType, List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    Query q1 = constructQuery(MAVEN.GROUP_ID, term, searchType);

    q1.setBoost(2.0f);

    Query q2 = constructQuery(MAVEN.ARTIFACT_ID, term, searchType);

    q2.setBoost(2.0f);

    BooleanQuery bq = new BooleanQuery();

    bq.add(q1, BooleanClause.Occur.SHOULD);

    bq.add(q2, BooleanClause.Occur.SHOULD);

    // switch for "extended" keywords
    // if ( false )
    // {
    // Query q3 = constructQuery( MAVEN.VERSION, term, searchType );
    //
    // Query q4 = constructQuery( MAVEN.CLASSIFIER, term, searchType );
    //
    // Query q5 = constructQuery( MAVEN.NAME, term, searchType );
    //
    // Query q6 = constructQuery( MAVEN.DESCRIPTION, term, searchType );
    //
    // bq.add( q3, BooleanClause.Occur.SHOULD );
    //
    // bq.add( q4, BooleanClause.Occur.SHOULD );
    //
    // bq.add( q5, BooleanClause.Occur.SHOULD );
    //
    // bq.add( q6, BooleanClause.Occur.SHOULD );
    // }

    IteratorSearchRequest req = createRequest(bq, from, count, hitLimit, uniqueRGA, filters);

    req.getMatchHighlightRequests().add(new MatchHighlightRequest(MAVEN.GROUP_ID, q1, MatchHighlightMode.HTML));
    req.getMatchHighlightRequests().add(new MatchHighlightRequest(MAVEN.ARTIFACT_ID, q2, MatchHighlightMode.HTML));

    return searchIterator(repositoryId, req);
  }

  public IteratorSearchResponse searchArtifactClassIterator(String term, String repositoryId, Integer from,
                                                            Integer count, Integer hitLimit, SearchType searchType,
                                                            List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    if (term.endsWith(".class")) {
      term = term.substring(0, term.length() - 6);
    }

    Query q = constructQuery(MAVEN.CLASSNAMES, term, searchType);

    IteratorSearchRequest req = createRequest(q, from, count, hitLimit, false, filters);

    req.getMatchHighlightRequests().add(new MatchHighlightRequest(MAVEN.CLASSNAMES, q, MatchHighlightMode.HTML));

    return searchIterator(repositoryId, req);
  }

  public IteratorSearchResponse searchArtifactIterator(String gTerm, String aTerm, String vTerm, String pTerm,
                                                       String cTerm, String repositoryId, Integer from,
                                                       Integer count, Integer hitLimit, boolean uniqueRGA,
                                                       SearchType searchType, List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    if (gTerm == null && aTerm == null && vTerm == null) {
      return IteratorSearchResponse.TOO_MANY_HITS_ITERATOR_SEARCH_RESPONSE;
    }

    BooleanQuery bq = new BooleanQuery();

    if (gTerm != null) {
      bq.add(constructQuery(MAVEN.GROUP_ID, gTerm, searchType), BooleanClause.Occur.MUST);
    }

    if (aTerm != null) {
      bq.add(constructQuery(MAVEN.ARTIFACT_ID, aTerm, searchType), BooleanClause.Occur.MUST);
    }

    if (vTerm != null) {
      bq.add(constructQuery(MAVEN.VERSION, vTerm, searchType), BooleanClause.Occur.MUST);
    }

    if (pTerm != null) {
      bq.add(constructQuery(MAVEN.PACKAGING, pTerm, searchType), BooleanClause.Occur.MUST);
    }

    // we can do this, since we enforce (above) that one of GAV is not empty, so we already have queries added
    // to bq
    if (cTerm != null) {
      if (Field.NOT_PRESENT.equalsIgnoreCase(cTerm)) {
        // bq.add( createQuery( MAVEN.CLASSIFIER, Field.NOT_PRESENT, SearchType.KEYWORD ),
        // BooleanClause.Occur.MUST_NOT );
        // This above should work too! -- TODO: fixit!
        filters.add(0, new ArtifactInfoFilter()
        {
          public boolean accepts(IndexingContext ctx, ArtifactInfo ai) {
            return Strings.isNullOrEmpty(ai.classifier);
          }
        });
      }
      else {
        bq.add(constructQuery(MAVEN.CLASSIFIER, cTerm, searchType), BooleanClause.Occur.MUST);
      }
    }

    IteratorSearchRequest req = createRequest(bq, from, count, hitLimit, uniqueRGA, filters);

    return searchIterator(repositoryId, req);
  }

  public IteratorSearchResponse searchArtifactSha1ChecksumIterator(String sha1Checksum, String repositoryId,
                                                                   Integer from, Integer count, Integer hitLimit,
                                                                   List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    if (sha1Checksum == null || sha1Checksum.length() > 40) {
      return IteratorSearchResponse.TOO_MANY_HITS_ITERATOR_SEARCH_RESPONSE;
    }

    SearchType searchType = sha1Checksum.length() == 40 ? SearchType.EXACT : SearchType.SCORED;

    BooleanQuery bq = new BooleanQuery();

    if (sha1Checksum != null) {
      bq.add(constructQuery(MAVEN.SHA1, sha1Checksum, searchType), BooleanClause.Occur.MUST);
    }

    IteratorSearchRequest req = createRequest(bq, from, count, hitLimit, false, filters);

    return searchIterator(repositoryId, req);
  }

  private FlatSearchResponse searchFlat(String repositoryId, FlatSearchRequest req)
      throws NoSuchRepositoryException
  {
    LockedIndexingContexts lockedContexts = lockSearchTargetIndexingContexts(repositoryId);

    if (lockedContexts == null) {
      return new FlatSearchResponse(req.getQuery(), 0, Collections.<ArtifactInfo>emptySet());
    }

    try {
      req.getContexts().addAll(lockedContexts.contexts.values());

      FlatSearchResponse result = mavenIndexer.searchFlat(req);

      postprocessResults(result.getResults());

      return result;
    }
    catch (BooleanQuery.TooManyClauses e) {
      if (log.isDebugEnabled()) {
        log.debug("Too many clauses exception caught:", e);
      }

      // XXX: a hack, I am sending too many results by setting the totalHits value to -1!
      return new FlatSearchResponse(req.getQuery(), -1, new HashSet<ArtifactInfo>());
    }
    catch (IOException e) {
      log.error("Got I/O exception while searching for query \"" + req.getQuery() + "\"", e);

      return new FlatSearchResponse(req.getQuery(), 0, new HashSet<ArtifactInfo>());
    }
    finally {
      lockedContexts.lock.unlock();
    }
  }

  private IteratorSearchResponse searchIterator(String repositoryId, IteratorSearchRequest req)
      throws NoSuchRepositoryException
  {
    LockedIndexingContexts lockedContexts = lockSearchTargetIndexingContexts(repositoryId);

    if (lockedContexts == null) {
      return IteratorSearchResponse.empty(req.getQuery());
    }

    // RuntimeException and ThreadDeath will leave locks locked. Not sure if there is a nice way to avoid this

    try {
      req.getContexts().addAll(lockedContexts.contexts.values());

      IteratorSearchResponse result = mavenIndexer.searchIterator(req);

      Query query = result.getQuery();
      int totalHints = result.getTotalHitsCount();
      IteratorResultSet results = new LockingIteratorResultSet(result.getResults(), lockedContexts.lock);

      return new IteratorSearchResponse(query, totalHints, results);
    }
    catch (BooleanQuery.TooManyClauses e) {
      lockedContexts.lock.unlock();

      if (log.isDebugEnabled()) {
        log.debug("Too many clauses exception caught:", e);
      }

      // XXX: a hack, I am sending too many results by setting the totalHits value to -1!
      return IteratorSearchResponse.TOO_MANY_HITS_ITERATOR_SEARCH_RESPONSE;
    }
    catch (IOException e) {
      lockedContexts.lock.unlock();

      log.error("Got I/O exception while searching for query \"" + req.getQuery().toString() + "\"", e);

      return IteratorSearchResponse.empty(req.getQuery());
    }
    catch (RuntimeException e) {
      lockedContexts.lock.unlock();

      throw e;
    }
  }

  // ----------------------------------------------------------------------------
  // Query construction
  // ----------------------------------------------------------------------------

  @Deprecated
  public Query constructQuery(Field field, String query, SearchType type)
      throws IllegalArgumentException
  {
    return mavenIndexer.constructQuery(field, query, type);
  }

  public Query constructQuery(Field field, SearchExpression expression)
      throws IllegalArgumentException
  {
    return mavenIndexer.constructQuery(field, expression);
  }

  // ----------------------------------------------------------------------------
  // Tree nodes
  // ----------------------------------------------------------------------------

  public TreeNode listNodes(final TreeNodeFactory factory, final String path, final String repositoryId)
      throws NoSuchRepositoryException, IOException
  {
    return listNodes(factory, path, null, null, repositoryId);
  }

  public TreeNode listNodes(final TreeNodeFactory factory, final String path, final Map<Field, String> hints,
                            final ArtifactInfoFilter artifactInfoFilter, final String repositoryId)
      throws NoSuchRepositoryException, IOException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);

    if (!INDEXABLE(repository) || !INSERVICE(repository)) {
      return null;
    }

    final TreeNode[] result = new TreeNode[1];
    shared(repository, new Runnable()
    {
      @Override
      public void run(IndexingContext context)
          throws IOException
      {
        TreeViewRequest request = new TreeViewRequest(factory, path, hints, artifactInfoFilter, context);

        // TODO igorf
        // TreeNode.listChildren lists children on demand using provided context
        // if context is closed asynchronously, the method will return empty list.
        // should through IllegalStateException instead.

        result[0] = indexTreeView.listNodes(request);
      }
    });

    return result[0];
  }

  // ----------------------------------------------------------------------------
  // Locking
  // ----------------------------------------------------------------------------

  public static interface Runnable
  {
    public void run(IndexingContext context)
        throws IOException;
  }

  /**
   * Simple value object meant to carry a number of indexing contexts and a lock that protects access to them.
   */
  private static class LockedIndexingContexts
  {
    final Map<String, IndexingContext> contexts;

    final Lock lock;

    public LockedIndexingContexts(Map<String, IndexingContext> contexts, Lock lock) {
      this.contexts = contexts;
      this.lock = lock;
    }
  }

  /**
   * Executes the runnable while holding shared lock on the specified repository index. If the repository is a group,
   * also acquires shared locks on all member repositories. Repositories without indexing contexts are silently
   * ignored. The context passed to the runnable is read-only, operations that modify index throw
   * UnsupportedOperationException. The indexing context passed to the runnable can be used after return from this
   * method, the caveat is that the context will return empty results after underlying Lucene index is closed.
   */
  public void shared(Repository repository, Runnable runnable)
      throws IOException
  {
    Lock lock = null;
    IndexingContext lockedContext = null;

    if (!INCLUDEINSEARCH(repository)) {
      return;
    }

    if (ISGROUP(repository)) {
      Map<String, Repository> members = new HashMap<String, Repository>();
      addGroupMembers(members, (GroupRepository) repository);
      members.put(repository.getId(), repository); // lock group itself too

      LockedIndexingContexts lockedContexts = lockIndexingContexts(members.values(), repository.getId());
      if (lockedContexts != null) {
        IndexingContext groupContext = lockedContexts.contexts.get(repository.getId());
        List<IndexingContext> memberContexts = new ArrayList<IndexingContext>();
        for (IndexingContext context : lockedContexts.contexts.values()) {
          if (context != null && !repository.getId().equals(context.getRepositoryId())) {
            memberContexts.add(context);
          }
        }

        lock = lockedContexts.lock;

        if (groupContext instanceof LockingIndexingContext) {
          groupContext = ((LockingIndexingContext) groupContext).getContext();
        }

        lockedContext = new MergedIndexingContext(groupContext.getId(), //
            groupContext.getRepositoryId(), //
            groupContext.getRepository(), //
            groupContext.getIndexDirectory(), //
            groupContext.isSearchable(), //
            new StaticContextMemberProvider(memberContexts))
        {
          @Override
          public Directory getIndexDirectory() {
            // igorf, yes, I am a paranoiac
            throw new UnsupportedOperationException();
          }
        };
      }
    }
    else {
      lock = getRepositoryLock(repository, false /* shared */);
      if (lock != null) {
        IndexingContext context = getRepositoryIndexContext(repository);
        if (context != null) {
          lockedContext = new LockingIndexingContext(context, lock);
        }
        else {
          lock.unlock();
          lock = null;
        }
      }
    }

    if (lockedContext != null && lock != null) {
      try {
        runnable.run(lockedContext);
      }
      finally {
        lock.unlock();
      }
    }
    else {
      log.warn("Could not perform index operation on repository {}", repository.getId(), new Exception(
          ARTIFICIAL_EXCEPTION));
    }
  }

  /**
   * Executes the runnable while holding shared lock on the specified repository index. Indexing context passed to
   * the
   * runnable must not be used after return from this method.
   */
  private void sharedSingle(Repository repository, Runnable runnable)
      throws IOException
  {
    Lock lock = getRepositoryLock(repository, false /* shared */);
    if (lock != null) {
      try {
        IndexingContext ctx = getRepositoryIndexContext(repository);
        if (ctx != null) {
          runnable.run(ctx);
        }
        else {
          log.warn("Could not perform index operation on repository {}", repository.getId(),
              new Exception(ARTIFICIAL_EXCEPTION));
        }
      }
      finally {
        lock.unlock();
      }
    }
  }

  /**
   * Executes the runnable with new empty temporary indexing context. The repository index is the replaced with the
   * contents of the temporary context.
   */
  // XXX igorf, better name
  private void temporary(final Repository repository, final Runnable runnable)
      throws IOException
  {
    String indexId = repository.getId() + "-tmp-ctx";

    File location = File.createTempFile(indexId, null, getTempDirectory());

    DirSupport.delete(location.toPath());
    DirSupport.mkdir(location.toPath());

    final DefaultIndexingContext temporary = new DefaultIndexingContext(indexId, //
        repository.getId(), //
        getRepositoryLocalStorageAsFile(repository), // repository local storage
        openFSDirectory(location), //
        null, // repository url
        null, // repository update url
        indexCreators, true);
    log.debug("Created temporary indexing context " + location + " for repository " + repository.getId());

    try {
      runnable.run(temporary);

      temporary.updateTimestamp(true);

      exclusiveSingle(repository, new Runnable()
      {
        @Override
        public void run(IndexingContext target)
            throws IOException
        {
          // TODO igorf guard against concurrent configuration changes
          // it is possible that Repository and/or target IndexingContext configuration have changed
          // and temporary context is populated based contains old/stale configuration
          // need to detect when this happens based on target timestamp for example and skip replace

          if (target != null) {
            target.replace(temporary.getIndexDirectory());
          }
          else {
            log.warn("Could not perform index operation on repository {}", repository.getId(),
                new Exception());
          }
        }
      });
    }
    finally {
      temporary.close(false);
      DirSupport.deleteIfExists(location.toPath());
    }
  }

  private FSDirectory openFSDirectory(File location)
      throws IOException
  {
    if (luceneFSDirectoryType == null) {
      // NEXUS-5752: default: nio
      return new NIOFSDirectory(location);
    }
    else if ("mmap".equals(luceneFSDirectoryType)) {
      return new MMapDirectory(location);
    }
    else if ("nio".equals(luceneFSDirectoryType)) {
      return new NIOFSDirectory(location);
    }
    else if ("simple".equals(luceneFSDirectoryType)) {
      return new SimpleFSDirectory(location);
    }
    else {
      throw new IllegalArgumentException(
          "''"
              + luceneFSDirectoryType
              + "'' is not valid/supported Lucene FSDirectory type. Only ''mmap'', ''nio'' and ''simple'' are allowed");
    }
  }

  /**
   * Executes the runnable while holding exclusive lock on the specified repository index. Indexing context passed to
   * the runnable must not be used after return from this methos.
   */
  private void exclusiveSingle(Repository repository, Runnable runnable)
      throws IOException
  {
    Lock lock = getRepositoryLock(repository, true /* exclusive */);
    if (lock != null) {
      try {
        IndexingContext ctx = getRepositoryIndexContext(repository);
        runnable.run(ctx);
      }
      finally {
        lock.unlock();
      }
    }
  }

  /**
   * Acquires either shared or exclusive "repository" lock. The lock is used to protect access to the repository
   * Lucene index. Returns null if requested lock cannot be acquired due to timeout or interruption.
   */
  private Lock getRepositoryLock(Repository repository, boolean exclusive) {
    final String lockName = exclusive ? "exclusive" : "shared";

    Thread deleteThread = deleteThreads.get(repository.getId());
    if (deleteThread != null && deleteThread != Thread.currentThread()) {
      log.debug("Could not acquire {} lock on repository {}. The repository is being deleted by thread {}.",
          lockName, repository.getId(), deleteThread.getName());
      return null;
    }

    ReadWriteLock rwlock;
    synchronized (repositoryLocks) {
      rwlock = repositoryLocks.get(repository.getId());
      if (rwlock == null) {
        rwlock = NamedReadWriteLock.decorate(new ReentrantReadWriteLock(), repository.getId());
        repositoryLocks.put(repository.getId(), rwlock);
      }
    }

    try {
      Lock lock = exclusive ? rwlock.writeLock() : rwlock.readLock();
      if (lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
        return lock;
      }
      if (log.isDebugEnabled()) {
        log.warn("Could not acquire {} lock on repository {} in {} seconds. " //
            + "Consider increasing value of ''nexus.indexer.locktimeout'' parameter. " //
            + "The operation has been aborted.", //
            lockName, repository.getId(), lockTimeoutSeconds, new Exception(ARTIFICIAL_EXCEPTION));
      }
      else {
        log.warn("Could not acquire {} lock on repository {} in {} seconds. " //
            + "Consider increasing value of ''nexus.indexer.locktimeout'' parameter. " //
            + "Enable debug log to recieve more information.", //
            lockName, repository.getId(), lockTimeoutSeconds);
      }
    }
    catch (InterruptedException e) {
      // TODO consider throwing IOException instead
      log.debug("Interrupted {} lock request on repository {}", lockName, repository.getId(), new Exception(
          ARTIFICIAL_EXCEPTION));
    }
    return null;
  }

  /**
   * Returns "reindex" reentrant lock that corresponds to the repository. The lock is used to protect access to
   * repository gz index download and publishing areas and to the repository local storage.
   */
  private ForceableReentrantLock getReindexLock(final Repository repository) {
    synchronized (reindexLocks) {
      ForceableReentrantLock lock = reindexLocks.get(repository.getId());
      if (lock == null) {
        lock = new ForceableReentrantLock();
        reindexLocks.put(repository.getId(), lock);
      }
      return lock;
    }
  }

  /**
   * Resolves and acquires shared lock on the specified repositoryId. If repositoryId corresponds to a group
   * repository, locks and returns all searchable members with corresponding indexing contexts. If repositoryId is
   * null, locks and returns all searchable repositories with corresponding indexing contexts.
   */
  private LockedIndexingContexts lockSearchTargetIndexingContexts(String repositoryId)
      throws NoSuchRepositoryException
  {
    List<Repository> repositories = new ArrayList<Repository>();
    if (repositoryId != null) {
      final Repository repository = repositoryRegistry.getRepository(repositoryId);
      if (INCLUDEINSEARCH(repository)) {
        if (ISGROUP(repository)) {
          Map<String, Repository> members = new HashMap<String, Repository>();
          addGroupMembers(members, (GroupRepository) repository);
          repositories.addAll(members.values());
        }
        else {
          repositories.add(repository);
        }
      }
    }
    else {
      for (Repository repository : repositoryRegistry.getRepositories()) {
        if (!ISGROUP(repository) && INCLUDEINSEARCH(repository) && repository.isSearchable()) {
          repositories.add(repository);
        }
      }
    }

    return lockIndexingContexts(repositories, null);
  }

  /**
   * Acquires shared locks on specified repositories. Repositories without indexing context are silently ignored.
   * Returns read-only contexts that are safe to use without explicit repository index locking/unlocking.
   */
  private LockedIndexingContexts lockIndexingContexts(Collection<Repository> repositories, String force) {
    // requirements:
    // - we are only interested in searchable indexing context
    // - repositories can be added/removed asynchronously and so can change their searchable flag
    // - we need to guarantee consistent lock order

    ArrayList<Repository> sorted = new ArrayList<Repository>(repositories);
    if (sorted.size() > 1) {
      Collections.sort(sorted, new Comparator<Repository>()
      {
        @Override
        public int compare(Repository o1, Repository o2) {
          return o1.getId().compareTo(o2.getId());
        }
      });
    }

    List<Lock> locks = new ArrayList<Lock>();
    Map<String, IndexingContext> contexts = new LinkedHashMap<String, IndexingContext>();
    for (Repository repository : sorted) {
      Lock lock = getRepositoryLock(repository, false /* shared */);
      if (lock != null) {
        // at this point repository index cannot be added or removed, we can safely use it
        IndexingContext context = getRepositoryIndexContext(repository);

        if (!repository.getId().equals(force) && context == null) {
          lock.unlock();
          continue;
        }

        locks.add(lock);
        contexts.put(repository.getId(), new LockingIndexingContext(context, lock));
      }
    }

    if (contexts.isEmpty()) {
      return null;
    }

    return new LockedIndexingContexts(contexts, new MultiLock(locks));
  }

  /**
   * Adds direct and indirect group repository "leaf" members.
   */
  private Map<String, Repository> addGroupMembers(Map<String, Repository> repositories, GroupRepository group) {
    for (Repository member : group.getMemberRepositories()) {
      if (INCLUDEINSEARCH(member) && !repositories.containsKey(member.getId())) {
        if (ISGROUP(member)) {
          addGroupMembers(repositories, (GroupRepository) member);
        }
        else {
          repositories.put(member.getId(), member);
        }
      }
    }
    return repositories;
  }

  // ----------------------------------------------------------------------------
  // PRIVATE
  // ----------------------------------------------------------------------------

  protected String getContextId(String repoId) {
    return repoId + CTX_SUFIX;
  }

  /**
   * @noreference this method is meant for unit tests only
   */
  public IndexingContext getRepositoryIndexContext(String repositoryId) {
    return mavenIndexer.getIndexingContexts().get(getContextId(repositoryId));
  }

  private static class IncrementalIndexUpdateException
      extends IOException
  {
    private static final long serialVersionUID = 6444842181110866037L;

    public IncrementalIndexUpdateException(String message) {
      super(message);
    }
  }
}