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
package org.sonatype.nexus.plugins.ruby.group;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sonatype.nexus.plugins.ruby.NexusStorage;
import org.sonatype.nexus.plugins.ruby.RubyGroupRepository;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.GroupItemNotFoundException;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.ruby.BundlerApiFile;
import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.DependencyHelper;
import org.sonatype.nexus.ruby.MergeSpecsHelper;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.SpecsIndexType;
import org.sonatype.nexus.ruby.SpecsIndexZippedFile;
import org.sonatype.nexus.ruby.layout.ProxyStorage;

import org.codehaus.plexus.util.IOUtil;

/**
 * Rubygems group storage.
 *
 * @since 2.11
 */
public class GroupNexusStorage
    extends NexusStorage
    implements ProxyStorage
{
  private final RubygemsGateway gateway;

  private final RubyGroupRepository repository;

  public GroupNexusStorage(RubyGroupRepository repository, RubygemsGateway gateway) {
    super(repository);
    this.repository = repository;
    this.gateway = gateway;
  }

  @Override
  public void retrieve(DependencyFile file) {
    doRetrieve(file);
  }

  @Override
  public void retrieve(SpecsIndexZippedFile file) {
    doRetrieve(file);
  }

  private void doRetrieve(RubygemsFile file) {
    try {
      log.debug("doRetrieve :: {}", file);
      file.set(setup(file));
    }
    catch (ItemNotFoundException e) {
      log.debug("doRetrieve-NotFound :: {} :: {}", file, e.toString());
      file.markAsNotExists();
    }
    catch (Exception e) {
      log.debug("doRetrieve-Exception :: {} :: {}", file, e.toString());
      file.setException(e);
    }
  }

  private StorageItem setup(RubygemsFile file) throws Exception {
    ResourceStoreRequest req = new ResourceStoreRequest(file.storagePath());
    // TODO is synchronized really needed
    synchronized (repository) {
      List<StorageItem> items = repository.doRetrieveItems(req);
      if (items.size() == 1) {
        return items.get(0);
      }
      return store(file, items);
    }
  }

  private StorageItem store(RubygemsFile file, List<StorageItem> items) throws Exception {
    StorageFileItem localItem = null;
    try {
      localItem = (StorageFileItem) repository.getLocalStorage().retrieveItem(repository,
          new ResourceStoreRequest(file.storagePath()));
    }
    catch (ItemNotFoundException e) {
      // Ignored. there are situations like a freshly created repo
    }

    boolean outdated = true; // outdated is true if there are no local-specs
    if (localItem != null) {
      long modified = localItem.getModified();
      outdated = false;
      for (StorageItem item : items) {
        outdated = outdated || (item.getModified() > modified);
      }
    }

    if (outdated) {
      switch (file.type()) {
        case DEPENDENCY:
          return mergeDependency((DependencyFile) file, items);
        case SPECS_INDEX_ZIPPED:
          return mergeSpecsIndex((SpecsIndexZippedFile) file, items);
        default:
          throw new RuntimeException("BUG: should never reach here: " + file);
      }
    }
    else {
      return localItem;
    }
  }

  private StorageItem mergeSpecsIndex(SpecsIndexZippedFile file, List<StorageItem> items) throws Exception {
    log.debug("mergeSpecsIndex :: {} :: {}", file, items);
    MergeSpecsHelper specs = gateway.newMergeSpecsHelper();
    for (StorageItem item : items) {
      try (InputStream is = ((StorageFileItem) item).getInputStream()) {
        specs.add(new GZIPInputStream(is));
      }
    }
    try (InputStream is = specs.getInputStream(file.specsType() == SpecsIndexType.LATEST)) {
      return storeSpecsIndex(file, is);
    }
  }

  private StorageItem storeSpecsIndex(SpecsIndexZippedFile file, InputStream content) throws Exception {
    ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
    try (OutputStream out = new GZIPOutputStream(gzipped)) {
      IOUtil.copy(content, out);
    }
    ContentLocator cl = new PreparedContentLocator(new ByteArrayInputStream(gzipped.toByteArray()),
        "application/x-gzip", gzipped.size());
    DefaultStorageFileItem item =
        new DefaultStorageFileItem(repository,
            new ResourceStoreRequest(file.storagePath()),
            true, true, cl);
    repository.storeItem(false, item);
    return item;
  }

  private StorageItem mergeDependency(DependencyFile file, List<StorageItem> dependencies) throws Exception {
    log.debug("mergeDependency :: {} :: {}", file, dependencies);
    DependencyHelper deps = gateway.newDependencyHelper();
    for (StorageItem item : dependencies) {
      try (InputStream is = ((StorageFileItem) item).getInputStream()) {
        deps.add(is);
      }
    }
    ContentLocator cl = new PreparedContentLocator(deps.getInputStream(true),
        file.type().mime(),
        PreparedContentLocator.UNKNOWN_LENGTH);

    DefaultStorageFileItem item =
        new DefaultStorageFileItem(repository,
            new ResourceStoreRequest(file.storagePath()),
            true, true, cl);
    repository.storeItem(false, item);
    return item;
  }

  @Override
  public void retrieve(BundlerApiFile file) {
    try {
      // mimic request as coming directly to ProxyRepository
      repository.doRetrieveItems(new ResourceStoreRequest(file.storagePath()));
      file.set(null);
    }
    catch (GroupItemNotFoundException | IOException e) {
      file.setException(e);
    }
  }

  @Override
  public boolean isExpired(DependencyFile file) {
    return true;
  }
}
