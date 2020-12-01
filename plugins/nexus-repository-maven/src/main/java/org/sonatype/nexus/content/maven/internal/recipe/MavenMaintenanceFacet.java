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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;

import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.content.maven.MavenMetadataRebuildContentFacet.METADATA_REBUILD_KEY;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;

/**
 * @since 3.next
 */
@Named
public class MavenMaintenanceFacet
    extends LastAssetMaintenanceFacet
{
  private MetadataRebuilder metadataRebuilder;

  @Inject
  public MavenMaintenanceFacet(final MetadataRebuilder metadataRebuilder) {
    this.metadataRebuilder = checkNotNull(metadataRebuilder);
  }

  @Override
  public Set<String> deleteComponent(final Component component) {
    return Sets.union(super.deleteComponent(component), deleteMetadata(component));
  }

  public Set<String> deleteMetadata(final Component component) {
    return deleteMetadata(component.namespace(), component.name(),
        component.attributes(Maven2Format.NAME).get(P_BASE_VERSION, String.class));
  }

  private Set<String> deleteMetadata(final String groupId, final String artifactId, final String baseVersion) {
    Repository repository = getRepository();
    ContentFacet contentFacet = repository.facet(ContentFacet.class);

    Set<String> deletedPaths = Sets.newHashSet();
    List<String[]> gavCoordinate = singletonList(new String[]{groupId, artifactId, baseVersion});
    deletedPaths.addAll(metadataRebuilder.deleteMetadata(repository, gavCoordinate));

    boolean isGroupArtifactEmpty = contentFacet.components().versions(groupId, artifactId).isEmpty();
    if (isGroupArtifactEmpty) {
      List<String[]> gaCoordinate = singletonList(new String[]{groupId, artifactId, null});
      deletedPaths.addAll(metadataRebuilder.deleteMetadata(repository, gaCoordinate));
    }
    else {
      contentFacet.assets()
          .path(prependIfMissing(metadataPath(groupId, artifactId, null).getPath(), "/"))
          .find()
          .ifPresent(asset -> asset.withAttribute(METADATA_REBUILD_KEY, true));
    }

    boolean isGroupEmpty = contentFacet.components().names(groupId).isEmpty();
    if (isGroupEmpty) {
      List<String[]> gCoordinate = singletonList(new String[]{groupId, null, null});
      deletedPaths.addAll(metadataRebuilder.deleteMetadata(repository, gCoordinate));
    }
    else {
      contentFacet.assets()
          .path(prependIfMissing(metadataPath(groupId, null, null).getPath(), "/"))
          .find()
          .ifPresent(asset -> asset.withAttribute(METADATA_REBUILD_KEY, true));
    }

    return deletedPaths;
  }
}
