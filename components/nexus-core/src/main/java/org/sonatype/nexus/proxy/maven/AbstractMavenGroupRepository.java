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
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractMavenGroupRepository
    extends AbstractGroupRepository
    implements MavenGroupRepository
{
  /**
   * Metadata manager.
   */
  private MetadataManager metadataManager;

  /**
   * The artifact packaging mapper.
   */
  private ArtifactPackagingMapper artifactPackagingMapper;

  private ArtifactStoreHelper artifactStoreHelper;

  private RepositoryKind repositoryKind;

  @Inject
  public void populateAbstractMavenGroupRepository(final MetadataManager metadataManager,
      final ArtifactPackagingMapper artifactPackagingMapper)
  {
    this.metadataManager = checkNotNull(metadataManager);
    this.artifactPackagingMapper = checkNotNull(artifactPackagingMapper);
    this.artifactStoreHelper = new ArtifactStoreHelper(this);
    this.repositoryKind = new DefaultRepositoryKind(GroupRepository.class,
        Arrays.asList(new Class<?>[] { MavenGroupRepository.class }));
  }

  @Override
  protected AbstractMavenGroupRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (AbstractMavenGroupRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }
  
  @Override
  public RepositoryKind getRepositoryKind() {
    return repositoryKind;
  }

  @Override
  public boolean isMergeMetadata() {
    return getExternalConfiguration(false).isMergeMetadata();
  }

  @Override
  public void setMergeMetadata(boolean mergeMetadata) {
    getExternalConfiguration(true).setMergeMetadata(mergeMetadata);
  }

  @Override
  public ArtifactPackagingMapper getArtifactPackagingMapper() {
    return artifactPackagingMapper;
  }

  @Override
  public ArtifactStoreHelper getArtifactStoreHelper() {
    return artifactStoreHelper;
  }

  @Override
  public MetadataManager getMetadataManager() {
    return metadataManager;
  }

  @Override
  public boolean recreateMavenMetadata(final ResourceStoreRequest request) {
    if (!shouldServiceOperation(request, "recreateMavenMetadata")) {
      return false;
    }
    boolean result = false;
    for (Repository repository : getMemberRepositories()) {
      if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
        result |= ((MavenRepository) repository).recreateMavenMetadata(request);
      }
    }
    return result;
  }

  @Override
  public RepositoryPolicy getRepositoryPolicy() {
    return RepositoryPolicy.MIXED;
  }

  @Override
  public void setRepositoryPolicy(RepositoryPolicy repositoryPolicy) {
    throw new UnsupportedOperationException(
        "Setting repository policy on a Maven group repository is not possible!");
  }

  @Override
  public boolean isMavenArtifact(StorageItem item) {
    return isMavenArtifactPath(item.getPath());
  }

  @Override
  public boolean isMavenMetadata(StorageItem item) {
    return isMavenMetadataPath(item.getPath());
  }

  @Override
  public boolean isMavenArtifactPath(String path) {
    return getGavCalculator().pathToGav(path) != null;
  }

  @Override
  public abstract boolean isMavenMetadataPath(String path);

  @Override
  public void storeItemWithChecksums(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
             StorageException, AccessDeniedException
  {
    getArtifactStoreHelper().storeItemWithChecksums(request, is, userAttributes);
  }

  @Override
  public void storeItemWithChecksums(boolean fromTask, AbstractStorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    getArtifactStoreHelper().storeItemWithChecksums(fromTask, item);
  }

  @Override
  public void deleteItemWithChecksums(ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, ItemNotFoundException, IllegalOperationException,
             StorageException, AccessDeniedException
  {
    getArtifactStoreHelper().deleteItemWithChecksums(request);
  }

  @Override
  public void deleteItemWithChecksums(boolean fromTask, ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    getArtifactStoreHelper().deleteItemWithChecksums(fromTask, request);
  }

}
