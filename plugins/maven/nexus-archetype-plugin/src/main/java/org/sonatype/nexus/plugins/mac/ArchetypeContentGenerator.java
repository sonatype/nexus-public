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
package org.sonatype.nexus.plugins.mac;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.index.DefaultIndexerManager;
import org.sonatype.nexus.index.IndexArtifactFilter;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.item.ContentGenerator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.RepositoryURLBuilder;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.context.IndexingContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Archetype catalog content generator.
 *
 * @author cstamas
 */
@Named(ArchetypeContentGenerator.ID)
@Singleton
public class ArchetypeContentGenerator
    implements ContentGenerator
{
  public static final String ID = "ArchetypeContentGenerator";

  private final MacPlugin macPlugin;

  private final DefaultIndexerManager indexerManager;

  private final IndexArtifactFilter indexArtifactFilter;

  private final RepositoryURLBuilder repositoryURLBuilder;

  @Inject
  public ArchetypeContentGenerator(final MacPlugin macPlugin, final DefaultIndexerManager indexerManager,
                                   final IndexArtifactFilter indexArtifactFilter,
                                   final RepositoryURLBuilder repositoryURLBuilder)
  {
    this.macPlugin = checkNotNull(macPlugin);
    this.indexerManager = checkNotNull(indexerManager);
    this.indexArtifactFilter = checkNotNull(indexArtifactFilter);
    this.repositoryURLBuilder = checkNotNull(repositoryURLBuilder);
  }

  @Override
  public String getGeneratorId() {
    return ID;
  }

  @Override
  public ContentLocator generateContent(Repository repository, String path, StorageFileItem item)
      throws IllegalOperationException, ItemNotFoundException, LocalStorageException
  {
    ArtifactInfoFilter artifactInfoFilter = new ArtifactInfoFilter()
    {
      public boolean accepts(IndexingContext ctx, ArtifactInfo ai) {
        return indexArtifactFilter.filterArtifactInfo(ai);
      }
    };
    final String exposedRepositoryContentUrl = repositoryURLBuilder.getExposedRepositoryContentUrl(repository);
    return new ArchetypeContentLocator(repository, exposedRepositoryContentUrl, indexerManager, macPlugin,
        artifactInfoFilter);
  }
}
