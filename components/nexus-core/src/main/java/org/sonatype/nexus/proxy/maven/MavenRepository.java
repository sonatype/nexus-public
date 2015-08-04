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
package org.sonatype.nexus.proxy.maven;

import java.io.InputStream;
import java.util.Map;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

public interface MavenRepository
    extends Repository
{
  GavCalculator getGavCalculator();

  ArtifactPackagingMapper getArtifactPackagingMapper();

  ArtifactStoreHelper getArtifactStoreHelper();

  MetadataManager getMetadataManager();

  boolean recreateMavenMetadata(ResourceStoreRequest request);

  RepositoryPolicy getRepositoryPolicy();

  void setRepositoryPolicy(RepositoryPolicy repositoryPolicy);

  /**
   * Returns true if the item passed in conforms to the Maven Repository Layout of this repository. Meaning, it is
   * adressable, and hence, consumable by Maven (Maven1 or Maven2, depending on the layout of this repository!).
   */
  boolean isMavenArtifact(StorageItem item);

  boolean isMavenArtifactPath(String path);

  /**
   * Returns true if the item passed in conforms to the Maven Repository Layout of this repository, and is metadata
   * (Maven1 or Maven2, depending on the layout of this repository!).
   */
  boolean isMavenMetadata(StorageItem item);

  boolean isMavenMetadataPath(String path);

  // == "Public API" (JSec protected)

  void storeItemWithChecksums(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
             StorageException, AccessDeniedException;

  void deleteItemWithChecksums(ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
             StorageException, AccessDeniedException;

  // == "Insider API" (unprotected)

  void storeItemWithChecksums(boolean fromTask, AbstractStorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException;

  void deleteItemWithChecksums(boolean fromTask, ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;
}
