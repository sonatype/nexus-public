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
package org.sonatype.nexus.proxy.storage.local.fs;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageEOFException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteStorageEOFException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsItemAttributeMetacontentAttribute;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.nexus.util.io.StreamSupport;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.Throwables2;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * The default FSPeer implementation, directly implementating it. There might be alternate implementations, like doing
 * 2nd level caching and so on.
 *
 * @author cstamas
 */
@Named
@Singleton
public class DefaultFSPeer
    extends ComponentSupport
    implements FSPeer
{
  /**
   * Filter for "move" and "delete" operations, to leave out folders
   * like ".meta" and ".nexus", with latter causing endless recursion
   * and IOException when deleting "/" folder.
   */
  private static final Predicate<Path> DOTTED_FILE_FILTER = new Predicate<Path>() {
    @Override
    public boolean apply(final Path input) {
      return input.getFileName().toString().startsWith(".");
    }
  };

  private static final String HIDDEN_TARGET_SUFFIX = ".nx-upload";

  private static final String APPENDIX = "nx-tmp";

  private static final String REPO_TMP_FOLDER = ".nexus/tmp";

  @Override
  public boolean isReachable(final Repository repository, final File repositoryBaseDir,
                             final ResourceStoreRequest request, final File target)
      throws LocalStorageException
  {
    return target.exists() && target.canWrite();
  }

  @Override
  public boolean containsItem(final Repository repository, final File repositoryBaseDir,
                              final ResourceStoreRequest request, final File target)
      throws LocalStorageException
  {
    return target.exists();
  }

  @Override
  public File retrieveItem(final Repository repository, final File repositoryBaseDir,
                           final ResourceStoreRequest request, final File target)
      throws ItemNotFoundException, LocalStorageException
  {
    return target;
  }

  @Override
  public void storeItem(final Repository repository, final File repositoryBaseDir, final StorageItem item,
                        final File target, final ContentLocator cl)
      throws UnsupportedStorageOperationException, LocalStorageException
  {
    if (log.isDebugEnabled()) {
      log.debug("Storing file to {}", target.getAbsolutePath());
    }

    // create parents down to the file itself (this will make those if needed, otherwise return silently)
    mkDirs(repository, target.getParentFile());

    if (cl != null) {
      // we have _content_ (content or link), hence we store a file
      final File hiddenTarget = getHiddenTarget(repository, repositoryBaseDir, target, item);

      // NEXUS-4550: Part One, saving to "hidden" (temp) file
      // In case of error cleaning up only what needed
      // No locking needed, AbstractRepository took care of that
      try (final InputStream is = cl.getContent(); final OutputStream os = new BufferedOutputStream(
          new FileOutputStream(hiddenTarget), getCopyStreamBufferSize())) {
        StreamSupport.copy(is, os, getCopyStreamBufferSize());
        os.flush();
      }
      catch (EOFException | RemoteStorageEOFException e)
      // NXCM-4852: Upload premature end (thrown by Jetty org.eclipse.jetty.io.EofException)
      // NXCM-4852: Proxy remote peer response premature end (should be translated by RRS)
      {
        try {
          Files.deleteIfExists(hiddenTarget.toPath());
        }
        catch (IOException e1) {
          // best effort to delete, we already have what to throw
        }
        throw new LocalStorageEOFException(String.format(
            "EOF during storing on path \"%s\" (while writing to hiddenTarget: \"%s\")",
            item.getRepositoryItemUid().toString(), hiddenTarget.getAbsolutePath()), e);
      }
      catch (IOException e) {
        try {
          Files.deleteIfExists(hiddenTarget.toPath());
        }
        catch (IOException e1) {
          // best effort to delete, we already have what to throw
        }
        throw new LocalStorageException(String.format(
            "Got exception during storing on path \"%s\" (while writing to hiddenTarget: \"%s\")",
            item.getRepositoryItemUid().toString(), hiddenTarget.getAbsolutePath()), e);
      }

      // NEXUS-4550: Part Two, moving the "hidden" (temp) file to final location
      // In case of error cleaning up both files
      // Locking is needed, AbstractRepository got shared lock only for destination

      // NEXUS-4550: FSPeer is the one that handles the rename in case of FS LS,
      // so we need here to claim exclusive lock on actual UID to perform the rename
      final RepositoryItemUidLock uidLock = item.getRepositoryItemUid().getLock();
      uidLock.lock(Action.create);

      try {
        handleRenameOperation(hiddenTarget, target);
        target.setLastModified(item.getModified());
      }
      catch (IOException e) {
        // if we ARE NOT handling attributes, do proper cleanup in case of IOEx
        // if we ARE handling attributes, leave backups in case of IOEx
        final boolean isCleanupNeeded =
            !item.getRepositoryItemUid().getBooleanAttributeValue(IsItemAttributeMetacontentAttribute.class);

        if (target != null && (isCleanupNeeded ||
            // NEXUS-4871 prevent zero length/corrupt files
            target.length() == 0)) {
          try {
            Files.delete(target.toPath());
          }
          catch (IOException e1) {
            log.warn("Could not delete file: " + target.getAbsolutePath(), e);
          }
        }

        if (hiddenTarget != null && (isCleanupNeeded ||
            // NEXUS-4871 prevent zero length/corrupt files
            hiddenTarget.length() == 0)) {
          try {
            Files.delete(hiddenTarget.toPath());
          }
          catch (IOException e1) {
            log.warn("Could not delete file: " + target.getAbsolutePath(), e);
          }
        }

        if (!isCleanupNeeded) {
          log.warn(
              "No cleanup done for error that happened while trying to save attibutes of item {}, the backup is left as {}!",
              item.getRepositoryItemUid().toString(), hiddenTarget.getAbsolutePath());
        }

        throw new LocalStorageException(String.format(
            "Got exception during storing on path \"%s\" (while moving to final destination)",
            item.getRepositoryItemUid().toString()), e);
      }
      finally {
        uidLock.unlock();
      }
    }
    else {
      // we have no content, we talk about directory
      try {
        DirSupport.mkdir(target.toPath());
      }
      catch (IOException e) {
        Throwables.propagate(e);
      }
      target.setLastModified(item.getModified());
    }
  }

  @Override
  public void shredItem(final Repository repository, final File repositoryBaseDir,
                        final ResourceStoreRequest request, final File target)
      throws ItemNotFoundException, UnsupportedStorageOperationException, LocalStorageException
  {
    if (log.isDebugEnabled()) {
      log.debug("Deleting file: {}", target.getAbsolutePath());
    }
    try {
      if (!DirSupport.deleteIfExists(target.toPath(), DOTTED_FILE_FILTER)) {
        throw new ItemNotFoundException(reasonFor(request, repository,
            "Path %s not found in local storage of repository %s", request.getRequestPath(),
            RepositoryStringUtils.getHumanizedNameString(repository)));
      }
    }
    catch (IOException e) {
      throw new LocalStorageException(String.format(
          "Could not delete file in repository %s from path \"%s\"",
          RepositoryStringUtils.getHumanizedNameString(repository), target.getAbsolutePath()), e);
    }
  }

  @Override
  public void moveItem(final Repository repository, final File repositoryBaseDir, final ResourceStoreRequest from,
                       final File fromTarget, final ResourceStoreRequest to, final File toTarget)
      throws ItemNotFoundException, UnsupportedStorageOperationException, LocalStorageException
  {
    if (log.isDebugEnabled()) {
      log.debug("Moving file from {} to {}", fromTarget.getAbsolutePath(), toTarget.getAbsolutePath());
    }
    try {
      if (!DirSupport.copyDeleteMoveIfExists(fromTarget.toPath(), toTarget.toPath(), DOTTED_FILE_FILTER)) {
        throw new ItemNotFoundException(reasonFor(from, repository,
            "Path %s not found in local storage of repository %s", from.getRequestPath(),
            RepositoryStringUtils.getHumanizedNameString(repository)));
      }
    }
    catch (IOException e) {
      throw new LocalStorageException("Error during moveItem", e);
    }
  }

  @Override
  public Collection<File> listItems(final Repository repository, final File repositoryBaseDir,
                                    final ResourceStoreRequest request, final File target)
      throws ItemNotFoundException, LocalStorageException
  {
    if (target.isDirectory()) {
      final List<File> result = Lists.newArrayList();
      final File[] files = target.listFiles(new FileFilter()
      {
        @Override
        public boolean accept(File pathname) {
          return !pathname.getName().endsWith(HIDDEN_TARGET_SUFFIX);
        }
      });

      if (files != null) {
        for (File file : files) {
          if (file.isFile() || file.isDirectory()) {
            result.add(file);
          }
        }
      }
      else {
        throw new LocalStorageException("Cannot list directory in repository " + repository + ", path "
            + target.getAbsolutePath());
      }
      return result;
    }
    else if (target.isFile()) {
      return null;
    }
    else {
      throw new ItemNotFoundException(reasonFor(request, repository,
          "Path %s not found in local storage of repository %s", request.getRequestPath(),
          RepositoryStringUtils.getHumanizedNameString(repository)));
    }
  }

  // ==

  protected File getHiddenTarget(final Repository repository, final File repositoryBaseDir, final File target,
                                 final StorageItem item)
      throws LocalStorageException
  {
    // NEXUS-5400: instead of putting "hidden" target in same dir structure as original file would reside (and
    // appending it
    // with some extra cruft), we place the file into repo-level tmp directory (/.nexus/tmp, REPO_TMP_FOLDER)
    // As since Nexus 2.0, due to attributes, it is required that whole repository from it's root must be kept on
    // same
    // volume (no subtree of it should reside on some other volume), meaning, rename would still happen
    // on same volume, hence is fast (is not copy+del on OS level).
    checkNotNull(target);

    try {
      final File repoTmpFolder = new File(repositoryBaseDir, REPO_TMP_FOLDER);
      mkDirs(repository, repoTmpFolder);

      // NEXUS-4955 add APPENDIX to make sure prefix is bigger the 3 chars
      return File.createTempFile(target.getName() + APPENDIX, HIDDEN_TARGET_SUFFIX, repoTmpFolder);
    }
    catch (IOException e) {
      throw new LocalStorageException(e.getMessage(), e);
    }
  }

  protected void mkDirs(final Repository repository, final File target)
      throws LocalStorageException
  {
    try {
      DirSupport.mkdir(target.toPath());
    }
    catch (IOException e) {
      throw new LocalStorageException(String.format(
          "Could not create the directory hierarchy in repository %s to write \"%s\"",
          RepositoryStringUtils.getHumanizedNameString(repository), target.getAbsolutePath()), e);
    }
  }

  // ==

  private static final String FILE_COPY_STREAM_BUFFER_SIZE_KEY = "upload.stream.bufferSize";

  private static final int FILE_COPY_STREAM_BUFFER_SIZE = SystemPropertiesHelper
      .getInteger(FILE_COPY_STREAM_BUFFER_SIZE_KEY, 8192); // align with Java default bufSize (BufferedOutputStream)

  protected int getCopyStreamBufferSize() {
    return FILE_COPY_STREAM_BUFFER_SIZE;
  }

  // ==

  public static final String RENAME_RETRY_COUNT_KEY = "rename.retry.count";

  public static final int RENAME_RETRY_COUNT = SystemPropertiesHelper.getInteger(RENAME_RETRY_COUNT_KEY, 0);

  public static final String RENAME_RETRY_DELAY_KEY = "rename.retry.delay";

  public static final long RENAME_RETRY_DELAY = SystemPropertiesHelper.getLong(RENAME_RETRY_DELAY_KEY, 0L);

  protected void handleRenameOperation(final File hiddenTarget, final File target)
      throws IOException
  {
    if (RENAME_RETRY_COUNT == 0) {
      // just do it once, no retries, no fuss
      Files.move(hiddenTarget.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    else {
      // do it 1 + retries needed, record problems
      final List<IOException> exceptions = Lists.newArrayListWithCapacity(1 + RENAME_RETRY_COUNT);
      boolean success = false;
      for (int i = 1; i <= (RENAME_RETRY_COUNT + 1); i++) {
        try {
          Files.move(hiddenTarget.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
          success = true;
          break;
        }
        catch (IOException e) {
          exceptions.add(e);
          try {
            Thread.sleep(RENAME_RETRY_DELAY);
          }
          catch (InterruptedException e1) {
            // ignore
          }
        }
      }
      if (!success) {
        throw Throwables2.composite(new IOException("Rename operation failed"), exceptions);
      }
    }
  }
}
