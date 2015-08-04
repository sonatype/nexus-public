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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.routing.Config;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.WritablePrefixSource;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link WritablePrefixSource} implementation that is backed by a {@link StorageFileItem} in a {@link
 * MavenRepository}.
 * Also serves as "the main" prefix list source. This is the only implementation of the {@link WritablePrefixSource}.
 *
 * @author cstamas
 * @since 2.4
 */
public class FilePrefixSource
    implements WritablePrefixSource
{
  private final MavenRepository mavenRepository;

  private final String path;

  private final TextFilePrefixSourceMarshaller prefixSourceMarshaller;

  private final RepositoryItemUid repositoryItemUid;

  /**
   * Constructor.
   */
  protected FilePrefixSource(final MavenRepository mavenRepository, final String path, final Config config) {
    this.mavenRepository = checkNotNull(mavenRepository);
    this.path = checkNotNull(path);
    this.prefixSourceMarshaller = new TextFilePrefixSourceMarshaller(config);
    this.repositoryItemUid = mavenRepository.createUid(path);
  }

  /**
   * Returns the repository path that is used to store {@link StorageFileItem} backing this entry source instance.
   *
   * @return the path of the backing file.
   */
  public String getFilePath() {
    return path;
  }

  /**
   * Returns the {@link MavenRepository} instance that is used to store {@link StorageFileItem} backing this entry
   * source instance.
   *
   * @return the repository of the backing file.
   */
  public MavenRepository getMavenRepository() {
    return mavenRepository;
  }

  /**
   * Returns the UID that points to the (existent or nonexistent) file that is (or will be) used to back this
   * {@link PrefixSource}.
   *
   * @return the {@link RepositoryItemUid} of this file item backed {@link PrefixSource}.
   */
  public RepositoryItemUid getRepositoryItemUid() {
    return repositoryItemUid;
  }

  protected TextFilePrefixSourceMarshaller getPrefixSourceMarshaller() {
    return prefixSourceMarshaller;
  }

  @Override
  public boolean exists() {
    try {
      return doReadProtected(new Callable<Boolean>()
      {
        @Override
        public Boolean call()
            throws IOException
        {
          return getFileItem() != null;
        }
      });
    }
    catch (IOException e) {
      // bam
    }
    return false;
  }

  @Override
  public boolean supported() {
    try {
      return doReadProtected(new Callable<Boolean>()
      {
        @Override
        public Boolean call()
            throws IOException
        {
          StorageFileItem file = getFileItem();
          if (file != null) {
            return getPrefixSourceMarshaller().read(file).supported();
          }
          return false;
        }
      });
    }
    catch (IOException e) {
      // bam
    }
    return false;
  }

  @Override
  public long getLostModifiedTimestamp() {
    try {
      return doReadProtected(new Callable<Long>()
      {
        @Override
        public Long call()
            throws Exception
        {
          final StorageFileItem file = getFileItem();
          if (file != null) {
            return file.getModified();
          }
          else {
            return -1L;
          }
        }
      });
    }
    catch (IOException e) {
      // bum
    }
    return -1L;
  }

  @Override
  public List<String> readEntries()
      throws IOException
  {
    return doReadProtected(new Callable<List<String>>()
    {
      @Override
      public List<String> call()
          throws Exception
      {
        final StorageFileItem file = getFileItem();
        if (file == null) {
          return null;
        }
        return getPrefixSourceMarshaller().read(file).entries();
      }
    });
  }

  @Override
  public void writeEntries(final PrefixSource prefixSource)
      throws IOException
  {
    checkNotNull(prefixSource);
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    getPrefixSourceMarshaller().write(prefixSource.readEntries(), bos);
    putFileItem(new ByteArrayContentLocator(bos.toByteArray(), "text/plain"));
  }

  @Override
  public void delete()
      throws IOException
  {
    deleteFileItem();
  }

  // ==

  protected <R> R doReadProtected(final Callable<R> callable)
      throws IOException
  {
    final RepositoryItemUidLock lock = getRepositoryItemUid().getLock();
    lock.lock(Action.read);
    try {
      try {
        return callable.call();
      }
      catch (IOException e) {
        throw e;
      }
      catch (Exception e) {
        Throwables.propagate(e);
      }
    }
    finally {
      lock.unlock();
    }
    return null; // to make compiler happy. but is unreachable
  }

  protected StorageFileItem getFileItem()
      throws IOException
  {
    try {
      final ResourceStoreRequest request = new ResourceStoreRequest(getFilePath());
      request.setRequestLocalOnly(true);
      request.setRequestGroupLocalOnly(true);
      request.getRequestContext().put(Manager.ROUTING_INITIATED_FILE_OPERATION_FLAG_KEY, Boolean.TRUE);
      @SuppressWarnings("deprecation")
      final StorageItem item = getMavenRepository().retrieveItem(true, request);
      if (item instanceof StorageFileItem) {
        return (StorageFileItem) item;
      }
      else {
        return null;
      }
    }
    catch (IllegalOperationException e) {
      // eh?
      return null;
    }
    catch (ItemNotFoundException e) {
      // not present
      return null;
    }
  }

  protected void putFileItem(final ContentLocator content)
      throws IOException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(getFilePath());
    request.setRequestLocalOnly(true);
    request.setRequestGroupLocalOnly(true);
    request.getRequestContext().put(Manager.ROUTING_INITIATED_FILE_OPERATION_FLAG_KEY, Boolean.TRUE);
    final DefaultStorageFileItem file =
        new DefaultStorageFileItem(getMavenRepository(), request, true, true, content);
    try {
      // NXCM-5188: Remark to not get tempted to change these to storeItemWithChecksums() method:
      // Since NEXUS-5418 was fixed (in 2.4), Nexus serves up ALL request for existing items that
      // has extra trailing ".sha1" or ".md5" from item attributes. This means, that when prefix file
      // is published in Nexus, there is no need anymore to save checksums to disk, as they will
      // be served up just fine. This is true for all items in Nexus storage, not just prefix
      // file related ones!
      getMavenRepository().storeItem(true, file);
    }
    catch (UnsupportedStorageOperationException e) {
      // eh?
    }
    catch (IllegalOperationException e) {
      // eh?
    }
  }

  @SuppressWarnings("deprecation")
  protected void deleteFileItem()
      throws IOException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(getFilePath());
    request.setRequestLocalOnly(true);
    request.setRequestGroupLocalOnly(true);
    request.getRequestContext().put(Manager.ROUTING_INITIATED_FILE_OPERATION_FLAG_KEY, Boolean.TRUE);
    try {
      // NXCM-5188: Remark to not get tempted to change these to deleteItemWithChecksums() method:
      // Since NEXUS-5418 was fixed (in 2.4), Nexus serves up ALL request for existing items that
      // has extra trailing ".sha1" or ".md5" from item attributes. This means, that when prefix file
      // is published in Nexus, there is no need anymore to save checksums to disk, as they will
      // be served up just fine. This is true for all items in Nexus storage, not just prefix
      // file related ones!
      getMavenRepository().deleteItem(true, request);
    }
    catch (ItemNotFoundException e) {
      // ignore
    }
    catch (UnsupportedStorageOperationException e) {
      // eh?
    }
    catch (IllegalOperationException e) {
      // ignore
    }
  }

  public void writeUnsupported()
      throws IOException
  {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    getPrefixSourceMarshaller().writeUnsupported(bos);
    putFileItem(new ByteArrayContentLocator(bos.toByteArray(), "text/plain"));
  }
}
