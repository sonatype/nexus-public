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
package org.sonatype.nexus.index.mindexer;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.AttributeStorage;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;

import com.google.common.base.Strings;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.DefaultArtifactContextProducer;
import org.apache.maven.index.context.IndexingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * NX enhanced {@link DefaultArtifactContextProducer} that gets missing SHA1 hashes from NX.
 *
 * @since 2.11
 */
@Named("default")
@Singleton
public class NexusArtifactContextProducer
    extends DefaultArtifactContextProducer
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final RepositoryRegistry repositoryRegistry;

  @Inject
  public NexusArtifactContextProducer(final RepositoryRegistry repositoryRegistry) {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  @Override
  public ArtifactContext getArtifactContext(final IndexingContext context, final File file) {
    final ArtifactContext ac = super.getArtifactContext(context, file);
    if (ac != null && ac.getArtifactInfo().sha1 == null) {
      // special case for proxies
      String itemPath = file.getPath();
      try {
        // NOTE: MI supports Maven2 repositories only, so if this line does not throw,
        // we know we deal with a Maven2 proxy repository (no need to check is it Maven1)
        final MavenProxyRepository proxyRepository = repositoryRegistry
            .getRepositoryWithFacet(context.getRepositoryId(), MavenProxyRepository.class);
        final File baseDir = getRepositoryLocalStorageFile(proxyRepository, RepositoryItemUid.PATH_ROOT);
        if (baseDir != null) {
          itemPath = itemPath.substring(baseDir.getPath().length());
          if (!itemPath.startsWith(RepositoryItemUid.PATH_SEPARATOR)) {
            itemPath = RepositoryItemUid.PATH_SEPARATOR + itemPath;
          }
          final AttributeStorage attributeStorage = proxyRepository.getAttributesHandler().getAttributeStorage();
          final Attributes attributes = attributeStorage.getAttributes(proxyRepository.createUid(itemPath));
          if (attributes != null) {
            final String sha1 = attributes.get(StorageFileItem.DIGEST_SHA1_KEY);
            if (!Strings.isNullOrEmpty(sha1)) {
              log.debug("ArtifactContext of {} enhanced with sha1={}", itemPath, sha1);
              ac.getArtifactInfo().sha1 = sha1;
            }
          }
        }
      }
      catch (NoSuchRepositoryException e) {
        // this means repo being scanned is not a maven2 proxy, but is hosted instead, just ignore
        log.debug("Repository {} is not a maven proxy", context.getRepositoryId());
      }
      catch (IOException e) {
        // IOEx means storage problems ahead, do report it
        log.warn("IO problem during retrieve of {}:{}", context.getRepositoryId(), itemPath, e);
      }
    }
    return ac;
  }

  @Nullable
  private File getRepositoryLocalStorageFile(final Repository repository, final String path) {
    if (repository.getLocalUrl() != null && repository.getLocalStorage() instanceof DefaultFSLocalRepositoryStorage) {
      try {
        return
            ((DefaultFSLocalRepositoryStorage) repository.getLocalStorage())
                .getBaseDir(repository, new ResourceStoreRequest(path));
      }
      catch (LocalStorageException e) {
        log.warn("Cannot get {} file from {} repository's LS", path, repository.getId(), e);
      }
    }
    return null;
  }
}
