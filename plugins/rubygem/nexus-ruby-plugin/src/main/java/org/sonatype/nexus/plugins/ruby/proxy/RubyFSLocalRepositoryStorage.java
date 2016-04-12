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
package org.sonatype.nexus.plugins.ruby.proxy;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.plugins.ruby.NexusRubygemsFacade;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.fs.FSPeer;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;

/**
 * Rubygems {@link LocalRepositoryStorage}.
 *
 * @since 2.11
 */
@Singleton
@Named("rubyfile")
public class RubyFSLocalRepositoryStorage
    extends DefaultFSLocalRepositoryStorage
{
  private final NexusRubygemsFacade fileSystem = new NexusRubygemsFacade(new DefaultRubygemsFileSystem());

  @Inject
  public RubyFSLocalRepositoryStorage(Wastebasket wastebasket,
                                      LinkPersister linkPersister,
                                      MimeSupport mimeSupport,
                                      FSPeer fsPeer)
  {
    super(wastebasket, linkPersister, mimeSupport, fsPeer);
  }

  @Override
  public void storeItem(Repository repository, StorageItem item)
      throws UnsupportedStorageOperationException, LocalStorageException
  {
    if (!item.getPath().startsWith("/.nexus")) {
      RubygemsFile file = fileSystem.file(item.getResourceStoreRequest());

      if (file.type() != FileType.NOT_FOUND) {
        item.getResourceStoreRequest().setRequestPath(file.storagePath());
        ((AbstractStorageItem) item).setPath(file.storagePath());
      }
    }
    super.storeItem(repository, item);
  }

  @Override
  public AbstractStorageItem retrieveItem(final Repository repository, final ResourceStoreRequest request)
      throws ItemNotFoundException, LocalStorageException
  {
    if (!request.getRequestPath().startsWith("/.nexus")) {
      RubygemsFile file = fileSystem.file(request);

      if (file.type() == FileType.BUNDLER_API || file.type() == FileType.DEPENDENCY) {
        request.pushRequestPath(file.storagePath());
        try {
          return super.retrieveItem(repository, request);
        }
        finally {
          request.popRequestPath();
        }
      }
    }
    return super.retrieveItem(repository, request);
  }

  @Override
  public boolean containsItem(final Repository repository, final ResourceStoreRequest request)
      throws LocalStorageException
  {
    if (!request.getRequestPath().startsWith("/.nexus")) {
      RubygemsFile file = fileSystem.file(request);

      if (file.type() == FileType.BUNDLER_API || file.type() == FileType.DEPENDENCY) {
        request.pushRequestPath(file.storagePath());
        try {
          return super.containsItem(repository, request);
        }
        finally {
          request.popRequestPath();
        }
      }
    }
    return super.containsItem(repository, request);
  }
}
