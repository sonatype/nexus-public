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
package org.sonatype.nexus.proxy.wastebasket;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.walker.AffirmativeStoreWalkerFilter;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;

@Named
@Singleton
public class DefaultWastebasket
    extends ComponentSupport
    implements Wastebasket
{
  private static final String DEFAULT_DELETE_OPERATION_KEY = DefaultWastebasket.class.getName() + ".defaultDeleteOperation";

  private static final String TRASH_PATH_PREFIX = "/.nexus/trash";

  protected static final long ALL = -1L;

  private final ApplicationConfiguration applicationConfiguration;

  private Walker walker;

  private final RepositoryRegistry repositoryRegistry;

  private DeleteOperation deleteOperation;

  @Inject
  public DefaultWastebasket(final ApplicationConfiguration applicationConfiguration,
                            final Walker walker,
                            final RepositoryRegistry repositoryRegistry)
  {
    this.applicationConfiguration = applicationConfiguration;
    this.walker = walker;
    this.repositoryRegistry = repositoryRegistry;
    this.deleteOperation = getDefaultDeleteOperation();
  }

  protected DeleteOperation getDefaultDeleteOperation() {
    final String defaultOperationString = SystemPropertiesHelper.getString(
        DEFAULT_DELETE_OPERATION_KEY, DeleteOperation.MOVE_TO_TRASH.name());
    try {
      return DeleteOperation.valueOf(defaultOperationString);
    }
    catch (Exception e) {
      // report and ignore
      log.warn("Bad value {}={}", DEFAULT_DELETE_OPERATION_KEY, defaultOperationString, e);
      return DeleteOperation.MOVE_TO_TRASH;
    }
  }

  protected ApplicationConfiguration getApplicationConfiguration() {
    return applicationConfiguration;
  }

  protected Walker getWalker() {
    return walker;
  }

  @VisibleForTesting
  void setWalker(final Walker walker) {
    this.walker = walker;
  }

  protected RepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  // ==

  // ==============================
  // Wastebasket iface

  @Override
  public DeleteOperation getDeleteOperation() {
    return deleteOperation;
  }

  @Override
  public void setDeleteOperation(final DeleteOperation deleteOperation) {
    this.deleteOperation = deleteOperation;
  }

  @Override
  public Long getTotalSize() {
    Long totalSize = null;

    for (Repository repository : getRepositoryRegistry().getRepositories()) {
      Long repoWBSize = getSize(repository);

      if (repoWBSize != null) {
        totalSize += repoWBSize;
      }
    }

    return totalSize;
  }

  @Override
  public void purgeAll()
      throws IOException
  {
    purgeAll(ALL);
  }

  @Override
  public void purgeAll(final long age)
      throws IOException
  {
    for (Repository repository : getRepositoryRegistry().getRepositories()) {
      purge(repository, age);
    }

    // NEXUS-4078: deleting "legacy" trash too for now
    // NEXUS-4468 legacy was not being cleaned up
    final File basketFile =
        getApplicationConfiguration().getWorkingDirectory(AbstractRepositoryFolderCleaner.GLOBAL_TRASH_KEY);

    // check for existence, is this needed at all?
    if (basketFile.isDirectory()) {
      final long limitDate = System.currentTimeMillis() - age;
      Files.walkFileTree(basketFile.toPath(), new SimpleFileVisitor<Path>()
      {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException
        {
          if (age == ALL || file.toFile().lastModified() < limitDate) {
            Files.delete(file);
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException
        {
          if (!basketFile.equals(dir.toFile()) && dir.toFile().list().length == 0) {
            Files.delete(dir);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  @Override
  public Long getSize(final Repository repository) {
    return null;
  }

  @Override
  public void purge(final Repository repository)
      throws IOException
  {
    purge(repository, ALL);
  }

  @Override
  public void purge(final Repository repository, final long age)
      throws IOException
  {
    ResourceStoreRequest req = new ResourceStoreRequest(getTrashPath(repository, RepositoryItemUid.PATH_ROOT));
    // NEXUS-4642 shall not delete the directory, since causes a problem if this has been symlinked to another
    // directory.
    // walker and walk and changes for age
    if (repository.getLocalStorage().containsItem(repository, req)) {
      req.setRequestGroupLocalOnly(true);
      req.setRequestLocalOnly(true);
      DefaultWalkerContext ctx = new DefaultWalkerContext(repository, req, new AffirmativeStoreWalkerFilter());
      ctx.getProcessors().add(new WastebasketWalker(age));
      getWalker().walk(ctx);
    }
  }

  @Override
  public void delete(final LocalRepositoryStorage ls, final Repository repository, final ResourceStoreRequest request)
      throws LocalStorageException
  {
    final DeleteOperation operation;
    if (request.getRequestContext().containsKey(DeleteOperation.DELETE_OPERATION_CTX_KEY)) {
      operation = (DeleteOperation) request.getRequestContext().get(DeleteOperation.DELETE_OPERATION_CTX_KEY);
    }
    else {
      operation = getDeleteOperation();
    }

    delete(ls, repository, request, operation);
  }

  private void delete(final LocalRepositoryStorage ls, final Repository repository,
                      final ResourceStoreRequest request, final DeleteOperation type)
      throws LocalStorageException
  {
    try {
      if (DeleteOperation.MOVE_TO_TRASH.equals(type)) {
        ResourceStoreRequest trashed =
            new ResourceStoreRequest(getTrashPath(repository, request.getRequestPath()));
        ls.moveItem(repository, request, trashed);
      }

      ls.shredItem(repository, request);
    }
    catch (ItemNotFoundException e) {
      // silent
    }
    catch (UnsupportedStorageOperationException e) {
      // yell
      throw new LocalStorageException("Delete operation is unsupported!", e);
    }
  }

  @Override
  public boolean undelete(final LocalRepositoryStorage ls, final Repository repository,
                          final ResourceStoreRequest request)
      throws LocalStorageException
  {
    try {
      ResourceStoreRequest trashed =
          new ResourceStoreRequest(getTrashPath(repository, request.getRequestPath()));
      ResourceStoreRequest untrashed =
          new ResourceStoreRequest(getUnTrashPath(repository, request.getRequestPath()));

      if (!ls.containsItem(repository, untrashed)) {
        ls.moveItem(repository, trashed, untrashed);
        return true;
      }
    }
    catch (ItemNotFoundException e) {
      // silent
    }
    catch (UnsupportedStorageOperationException e) {
      // yell
      throw new LocalStorageException("Undelete operation is unsupported!", e);
    }

    return false;
  }

  protected String getTrashPath(final Repository repository, final String path) {
    if (path.startsWith(TRASH_PATH_PREFIX)) {
      return path;
    }
    else if (path.startsWith(RepositoryItemUid.PATH_SEPARATOR)) {
      return TRASH_PATH_PREFIX + path;
    }
    else {
      return TRASH_PATH_PREFIX + RepositoryItemUid.PATH_SEPARATOR + path;
    }
  }

  protected String getUnTrashPath(final Repository repository, final String path) {
    String result = path;
    if (result.startsWith(TRASH_PATH_PREFIX)) {
      result = result.substring(TRASH_PATH_PREFIX.length(), result.length());
    }

    if (!result.startsWith(RepositoryItemUid.PATH_ROOT)) {
      result = RepositoryItemUid.PATH_ROOT + result;
    }

    return result;
  }
}
