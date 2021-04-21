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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.base.Strings;
import org.apache.maven.artifact.repository.metadata.Metadata;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;
import static org.sonatype.nexus.repository.maven.internal.Constants.METADATA_FILENAME;

/**
 * @since 3.26
 */
@Named
public class MavenMetadataRebuildFacetImpl
    extends FacetSupport
    implements MavenMetadataRebuildFacet
{
  private MavenContentFacet mavenContentFacet;

  private MetadataRebuilder metadataRebuilder;

  private static final ThreadLocal<Boolean> rebuilding = new ThreadLocal<>();

  @Inject
  public MavenMetadataRebuildFacetImpl(final MetadataRebuilder metadataRebuilder)
  {
    this.metadataRebuilder = checkNotNull(metadataRebuilder);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    mavenContentFacet = facet(MavenContentFacet.class);
  }

  @Override
  public void maybeRebuildMavenMetadata(final String path, final boolean update, final boolean rebuildChecksums)
      throws IOException
  {
    Optional<FluentAsset> maybeAsset = mavenContentFacet.assets().path(path).find();
    Optional<Payload> maybePayload = maybeAsset.map(FluentAsset::download);

    if (maybeAsset.isPresent() && maybePayload.isPresent()) {
      FluentAsset asset = maybeAsset.get();
      Payload payload = maybePayload.get();
      if (needsRebuild(mavenContentFacet.getMavenPathParser().parsePath(path), asset)) {
        asset.withoutAttribute(METADATA_REBUILD);
        rebuildMetadata(payload, update, rebuildChecksums);
      }
    }
  }

  private boolean needsRebuild(final MavenPath path, final FluentAsset asset) {
    return !TRUE.equals(rebuilding.get())
        && path.getFileName().equals(METADATA_FILENAME)
        && !(getRepository().getType() instanceof ProxyType)
        && TRUE.equals(asset.attributes(METADATA_REBUILD).get(METADATA_FORCE_REBUILD, false));
  }

  private void rebuildMetadata(final Payload metadataPayload, final boolean update, final boolean rebuildChecksums)
      throws IOException
  {
    Metadata metadata = MavenModels.readMetadata(metadataPayload.openInputStream());
    String groupId = Optional.ofNullable(metadata).map(Metadata::getGroupId).orElse(null);
    String artifactId = Optional.ofNullable(metadata).map(Metadata::getArtifactId).orElse(null);
    String baseVersion = Optional.ofNullable(metadata).map(Metadata::getVersion).orElse(null);
    rebuildMetadata(groupId, artifactId, baseVersion, rebuildChecksums, update);
  }

  @Override
  public void rebuildMetadata(
      final String groupId,
      final String artifactId,
      final String baseVersion,
      final boolean rebuildChecksums)
  {
    final boolean update = !Strings.isNullOrEmpty(groupId)
        || !Strings.isNullOrEmpty(artifactId)
        || !Strings.isNullOrEmpty(baseVersion);
    rebuildMetadata(groupId, artifactId, baseVersion, rebuildChecksums, update);
  }

  @Override
  public void rebuildMetadata(
      final String groupId,
      final String artifactId,
      final String baseVersion,
      final boolean rebuildChecksums,
      final boolean update)
  {
    // avoid triggering nested rebuilds as the rebuilder will already do that if necessary
    rebuilding.set(TRUE);
    try {
      metadataRebuilder
          .rebuildInTransaction(getRepository(), update, rebuildChecksums, groupId, artifactId, baseVersion);
    }
    finally {
      rebuilding.remove();
    }
  }
}
