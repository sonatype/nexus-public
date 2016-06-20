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
package org.sonatype.nexus.plugins.ruby;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.ruby.Directory;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * ???
 *
 * @since 2.11
 */
public class NexusRubygemsFacade
    extends ComponentSupport
{
  private final RubygemsFileSystem filesystem;

  public NexusRubygemsFacade(RubygemsFileSystem filesystem) {
    this.filesystem = filesystem;
  }

  public RubygemsFile get(ResourceStoreRequest request) {
    String[] pathAndQuery = extractGemsQuery(request);
    log.debug("get :: {} :: query={}", request.getRequestPath(), pathAndQuery);
    return filesystem.get(pathAndQuery[0], pathAndQuery[1]);
  }

  private String[] extractGemsQuery(ResourceStoreRequest request) {
    if (request.getRequestPath().contains("?gems=")) {
      int index = request.getRequestPath().indexOf('?');
      return new String[]{
          request.getRequestPath().substring(0, index),
          request.getRequestPath().substring(index + 1)
      };
    }
    String query = "";
    // only request with ...?gems=... are used by the Layout
    if (request.getRequestUrl() != null && request.getRequestUrl().contains("?gems=")) {
      query = request.getRequestUrl().substring(request.getRequestUrl().indexOf('?') + 1);
    }
    return new String[]{request.getRequestPath(), query};
  }

  public RubygemsFile file(ResourceStoreRequest request) {
    String[] pathAndQuery = extractGemsQuery(request);
    log.debug("file :: {} :: query={}", request.getRequestPath(), pathAndQuery);
    return filesystem.file(pathAndQuery[0], pathAndQuery[1]);
  }

  public RubygemsFile file(String path) {
    return filesystem.file(path);
  }

  public RubygemsFile post(InputStream is, String path) {
    return filesystem.post(is, path);
  }

  public RubygemsFile post(InputStream is, RubygemsFile file) {
    filesystem.post(is, file);
    return file;
  }

  public RubygemsFile delete(String original) {
    return filesystem.delete(original);
  }

  @SuppressWarnings("deprecation")
  public StorageItem handleCommon(RubyRepository repository, RubygemsFile file)
      throws IllegalOperationException, StorageException
  {
    log.debug("handleCommon :: {} :: {}", repository.getId(), file);
    switch (file.state()) {
      case ERROR:
        Exception e = file.getException();
        log.debug("handleCommon :: ERROR", e);
        if (e instanceof IllegalOperationException) {
          throw (IllegalOperationException) e;
        }
        if (e instanceof RemoteAccessException) {
          throw (RemoteAccessException) e;
        }
        if (e instanceof StorageException) {
          throw (StorageException) e;
        }
        if (e instanceof IOException) {
          throw new StorageException(e);
        }
        throw new RuntimeException(e);
      case PAYLOAD:
        return (StorageItem) file.get();
      case FORBIDDEN:
          throw new IllegalRequestException(new ResourceStoreRequest(file.remotePath()),
              "Repository with ID='" + repository.getId()
              + "' does not allow deleting '" + file.remotePath() + "'.");
      case NOT_EXISTS:
      case TEMP_UNAVAILABLE:
      case NEW_INSTANCE:
      default:
        throw new RuntimeException("BUG: should not come here - " + file.state());
    }
  }

  @SuppressWarnings("deprecation")
  public StorageItem handleMutation(RubyRepository repository, RubygemsFile file)
      throws IllegalOperationException, StorageException, UnsupportedStorageOperationException
  {
    log.debug("handleMutation :: {} :: {}", repository.getId(), file);
    switch (file.state()) {
      case ERROR:
        Exception e = file.getException();
        if (e instanceof UnsupportedStorageOperationException) {
          throw (UnsupportedStorageOperationException) e;
        }
      default:
        return handleCommon(repository, file);
    }
  }

  static class DirectoryItemStorageItem
      extends AbstractStorageItem
  {
    public DirectoryItemStorageItem(Repository repository, String path) {
      super(repository, new ResourceStoreRequest(path), true, false);
    }

    @Override
    public boolean isVirtual() {
      return true;
    }
  }

  static class DirectoryStoreageItem
      extends DefaultStorageCollectionItem
  {
    private Directory dir;

    private RubyRepository repository;

    DirectoryStoreageItem(RubyRepository repository, ResourceStoreRequest req, Directory dir) {
      super(repository, req, true, false);
      this.dir = dir;
      this.repository = repository;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Collection<StorageItem> list()
        throws AccessDeniedException, NoSuchResourceStoreException, IllegalOperationException, StorageException
    {
      Collection<StorageItem> result;
      try {
        result = super.list();
      }
      catch (ItemNotFoundException e) {
        result = new LinkedList<>();
      }
      Set<String> items = new TreeSet<>(Arrays.asList(dir.getItems()));
      for (StorageItem i : result) {
        items.remove(i.getName());
        items.remove(i.getName() + "/");
      }
      for (String item : items) {
        if (!item.endsWith("/")) {
          result.add(new DirectoryItemStorageItem(repository, dir.storagePath() + "/" + item));
        }
      }
      return result;
    }
  }

  @SuppressWarnings("deprecation")
  public StorageItem handleRetrieve(RubyRepository repository, ResourceStoreRequest req, RubygemsFile file)
      throws IllegalOperationException, StorageException, ItemNotFoundException
  {
    log.debug("handleRetrieve :: {} :: {}", repository.getId(), file);
    switch (file.state()) {
      case NO_PAYLOAD:
        if (file.type() == FileType.DIRECTORY) {
          // handle directories
          req.setRequestPath(file.storagePath());
          return new DirectoryStoreageItem(repository, req, (Directory) file);
        }
        if (file.type() == FileType.NO_CONTENT) {
          return new DefaultStorageFileItem(repository, req, true, false,
              new ByteArrayContentLocator(new byte[0], file.type().mime()));
        }
      case NOT_EXISTS:
        throw new ItemNotFoundException(
            ItemNotFoundException.reasonFor(new ResourceStoreRequest(file.remotePath()), repository,
                "Can not serve path %s for repository %s", file.storagePath(),
                RepositoryStringUtils.getHumanizedNameString(repository)));
      case ERROR:
        Exception e = file.getException();
        if (e instanceof ItemNotFoundException) {
          throw (ItemNotFoundException) e;
        }
      default:
        return handleCommon(repository, file);
    }
  }
}