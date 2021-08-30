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
package org.sonatype.nexus.repository.internal.blobstore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreLocator;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.RepositoryDoesNotExistException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link BlobStoreLocator} implementation.
 *
 * @since 3.34
 */
@Named
@Singleton
public class BlobStoreLocatorImpl implements BlobStoreLocator
{

  private final BlobStoreManager blobStoreManager;

  private final RepositoryManager repositoryManager;

  @Inject
  public BlobStoreLocatorImpl(final BlobStoreManager blobStoreManager, final RepositoryManager repositoryManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  public BlobStore getBlobStore(final String repositoryName) throws RepositoryDoesNotExistException {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new RepositoryDoesNotExistException();
    }
    Configuration configuration = repository.getConfiguration();
    String blobStoreName = configuration.attributes(STORAGE).require(BLOB_STORE_NAME, String.class);
    return blobStoreManager.get(blobStoreName);
  }
}
