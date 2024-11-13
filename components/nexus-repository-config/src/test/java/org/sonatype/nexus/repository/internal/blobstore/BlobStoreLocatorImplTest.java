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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.RepositoryDoesNotExistException;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.hamcrest.MatcherAssert.assertThat;

public class BlobStoreLocatorImplTest extends TestSupport
{
  private BlobStoreLocatorImpl underTest;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private Repository sourceRepository;

  @Mock
  private BlobStore sourceBlobStore;

  @Mock
  private BlobStoreConfiguration sourceBlobStoreConfiguration;

  @Mock
  private Configuration sourceRepositoryConfiguration;

  @Mock
  NestedAttributesMap sourceRepoStorageAttributes;


  @Before
  public void setup() throws RepositoryDoesNotExistException {
    when(blobStoreManager.get("blobStoreName")).thenReturn(sourceBlobStore);
    when(sourceRepository.getConfiguration()).thenReturn(sourceRepositoryConfiguration);
    when(repositoryManager.get("repoName")).thenReturn(sourceRepository);
    when(sourceRepoStorageAttributes.require(BLOB_STORE_NAME, String.class)).thenReturn("blobStoreName");
    when(sourceRepoStorageAttributes.get(BLOB_STORE_NAME, String.class)).thenReturn("blobStoreName");
    when(sourceRepositoryConfiguration.attributes(STORAGE)).thenReturn(sourceRepoStorageAttributes);
    underTest = new BlobStoreLocatorImpl(blobStoreManager, repositoryManager);
  }

  @Test
  public void getBlobStore() throws RepositoryDoesNotExistException {
    BlobStore blobStore = underTest.getBlobStore("repoName");
    assertThat(blobStore, is(sourceBlobStore));
  }

  @Test (expected = RepositoryDoesNotExistException.class)
  public void getBlobStoreRepositoryNotPresent() throws RepositoryDoesNotExistException {
    when(repositoryManager.get("repoName")).thenReturn(null);
    underTest.getBlobStore("repoName");
  }
}
