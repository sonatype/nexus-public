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
package org.sonatype.nexus.plugins.lvo.strategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.lvo.DiscoveryRequest;
import org.sonatype.nexus.plugins.lvo.DiscoveryResponse;
import org.sonatype.nexus.plugins.lvo.DiscoveryStrategy;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;

/**
 * This is a "local" strategy, uses Nexus content for information fetch.
 *
 * @author cstamas
 */
@Singleton
@Named("content-get")
@Typed(DiscoveryStrategy.class)
public class ContentGetDiscoveryStrategy
    extends AbstractDiscoveryStrategy
{
  private final RepositoryRegistry repositoryRegistry;

  @Inject
  public ContentGetDiscoveryStrategy(RepositoryRegistry repositoryRegistry) {
    this.repositoryRegistry = Preconditions.checkNotNull(repositoryRegistry);
  }

  public DiscoveryResponse discoverLatestVersion(DiscoveryRequest request)
      throws NoSuchRepositoryException, IOException
  {
    final DiscoveryResponse dr = new DiscoveryResponse(request);
    final StorageFileItem response = handleRequest(request);
    if (response != null) {
      try (InputStream is = response.getInputStream()) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(is, bos);
        dr.setVersion(bos.toString());
        dr.setSuccessful(true);
      }
    }

    return dr;
  }

  protected StorageFileItem handleRequest(DiscoveryRequest request) {
    try {
      // NoSuchRepository if the repoId is not known
      final Repository repository = repositoryRegistry.getRepository(request.getLvoKey().getRepositoryId());

      // ItemNotFound if the path does not exists
      final StorageItem item =
          repository.retrieveItem(false, new ResourceStoreRequest(request.getLvoKey().getLocalPath()));

      // return only if item is a file, nuke it otherwise
      if (item instanceof StorageFileItem) {
        return (StorageFileItem) item;
      }
      else {
        return null;
      }
    }
    catch (Exception e) {
      // we are very rude about exceptions here ;)
      log.warn("Could not retrieve content!", e);
      return null;
    }
  }
}
