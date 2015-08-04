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
package org.sonatype.nexus.obr.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;

/**
 * An {@link ObrSite} that's managed by Nexus.
 */
public class ManagedObrSite
    extends AbstractObrSite
{
  private final StorageFileItem item;

  private final URL url;

  /**
   * Creates a managed OBR site based on the given metadata item inside Nexus.
   *
   * @param item the metadata item
   */
  public ManagedObrSite(final StorageFileItem item)
      throws StorageException
  {
    this.item = item;

    url = getAbsoluteUrlFromBase(item);
  }

  /**
   * Finds the absolute URL for the managed OBR site.
   *
   * @return the absolute URL
   */
  private static URL getAbsoluteUrlFromBase(final StorageFileItem item)
      throws StorageException
  {
    final RepositoryItemUid uid = item.getRepositoryItemUid();

    final Repository repository = uid.getRepository();
    final ResourceStoreRequest request = new ResourceStoreRequest(uid.getPath());

    if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      final ProxyRepository proxyRepository = repository.adaptToFacet(ProxyRepository.class);
      final RemoteRepositoryStorage storage = proxyRepository.getRemoteStorage();
      if (storage != null) {
        return storage.getAbsoluteUrlFromBase(proxyRepository, request);
      }
      // locally hosted proxy repository, so drop through...
    }

    return repository.getLocalStorage().getAbsoluteUrlFromBase(repository, request);
  }

  public URL getMetadataUrl() {
    return url;
  }

  public String getMetadataPath() {
    return item.getRepositoryItemUid().getPath();
  }

  @Override
  protected InputStream openRawStream()
      throws IOException
  {
    return item.getInputStream();
  }

  @Override
  protected String getContentType() {
    return item.getMimeType();
  }
}
