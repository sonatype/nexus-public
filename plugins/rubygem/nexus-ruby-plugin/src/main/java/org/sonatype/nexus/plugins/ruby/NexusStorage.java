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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.Directory;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.SpecsIndexFile;
import org.sonatype.nexus.ruby.SpecsIndexZippedFile;
import org.sonatype.nexus.ruby.layout.Storage;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * A {@link RubyRepository} backed {@link Storage} implementation.
 *
 * @since 2.11
 */
public class NexusStorage
    extends ComponentSupport
    implements Storage
{
  protected final RubyRepository repository;

  public NexusStorage(RubyRepository repository) {
    this.repository = repository;
  }

  @Override
  public void retrieve(RubygemsFile file) {
    log.debug("retrieve :: {}", file);
    try {
      file.set(repository.retrieveDirectItem(new ResourceStoreRequest(file.storagePath())));
    }
    catch (ItemNotFoundException e) {
      file.markAsNotExists();
    }
    catch (IOException | IllegalOperationException e) {
      file.setException(e);
    }
  }

  @Override
  public void retrieve(DependencyFile file) {
    retrieve((RubygemsFile) file);
  }

  @Override
  public void retrieve(SpecsIndexZippedFile file) {
    retrieve((RubygemsFile) file);
  }

  @Override
  public void retrieve(SpecsIndexFile specs) {
    log.debug("retrieve :: {}", specs);
    SpecsIndexZippedFile source = specs.zippedSpecsIndexFile();
    try {
      StorageFileItem item = (StorageFileItem)
          repository.retrieveDirectItem(new ResourceStoreRequest(source.storagePath()));

      DefaultStorageFileItem unzippedItem =
          new DefaultStorageFileItem(repository,
              new ResourceStoreRequest(specs.storagePath()),
              true, false,
              gunzipContentLocator(item));
      unzippedItem.setModified(item.getModified());
      specs.set(unzippedItem);
    }
    catch (ItemNotFoundException e) {
      specs.markAsNotExists();
    }
    catch (IOException | IllegalOperationException e) {
      specs.setException(e);
    }
  }

  private ContentLocator gunzipContentLocator(StorageFileItem item) throws IOException {
    try (InputStream in = item.getInputStream()) {
      ByteArrayInputStream gzipped = IOUtil.toGunzipped(in);
      return new PreparedContentLocator(gzipped,
          "application/x-marshal-ruby",
          gzipped.available());
    }
  }

  @Override
  public InputStream getInputStream(RubygemsFile file) throws IOException {
    if (file.get() == null) {
      retrieve(file);
    }
    return ((StorageFileItem) file.get()).getInputStream();
  }

  @Override
  public long getModified(RubygemsFile file) {
    return ((StorageItem) file.get()).getModified();
  }

  @Override
  public void create(InputStream is, RubygemsFile file) {
    update(is, file);
  }

  @Override
  public void update(InputStream is, RubygemsFile file) {
    log.debug("update :: {}", file);
    ResourceStoreRequest request = new ResourceStoreRequest(file.storagePath());
    ContentLocator contentLocator = new PreparedContentLocator(is, file.type().mime(), ContentLocator.UNKNOWN_LENGTH);
    DefaultStorageFileItem fileItem = new DefaultStorageFileItem(repository, request,
        true, true, contentLocator);

    try {
      // we need to bypass access control here !!!
      repository.storeItem(false, fileItem);
      file.set(fileItem);
    }
    catch (IOException | UnsupportedStorageOperationException | IllegalOperationException e) {
      file.setException(e);
    }
  }

  @SuppressWarnings("deprecation")
  public void delete(RubygemsFile file) {
    log.debug("delete :: {}", file);
    ResourceStoreRequest request = new ResourceStoreRequest(file.storagePath());

    try {
      repository.deleteItem(false, request);
    }
    catch (IOException | UnsupportedStorageOperationException | IllegalOperationException e) {
      file.setException(e);
    }
    catch (ItemNotFoundException e) {
      // already deleted
    }
  }

  @Override
  public void memory(ByteArrayInputStream data, RubygemsFile file) {
    memory(data, file, ContentLocator.UNKNOWN_LENGTH);
  }

  @Override
  public void memory(String data, RubygemsFile file) {
    memory(new ByteArrayInputStream(data.getBytes()), file, data.getBytes().length);
  }

  private void memory(InputStream data, RubygemsFile file, long length) {
    ContentLocator cl = new PreparedContentLocator(data, file.type().mime(), length);
    file.set(new DefaultStorageFileItem(repository, new ResourceStoreRequest(file.storagePath()), true, false, cl));
  }

  @Override
  public String[] listDirectory(Directory dir) {
    Set<String> result = new TreeSet<>(Arrays.asList(dir.getItems()));
    try {
      StorageItem list = repository.retrieveDirectItem(new ResourceStoreRequest(dir.storagePath()));
      if (list instanceof StorageCollectionItem) {
        for (StorageItem item : ((StorageCollectionItem) list).list()) {
          result.add(item.getName());
        }
      }
    }
    catch (ItemNotFoundException e) {
      // an empty array is good enough
    }
    catch (IOException | IllegalOperationException | AccessDeniedException | NoSuchResourceStoreException e) {
      dir.setException(e);
      // the return is still an empty array but the exception gets propagated
    }
    return result.toArray(new String[result.size()]);
  }
}
