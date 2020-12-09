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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;

import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
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

  @Override
  public int deleteComponents(final int[] componentIds) {
    Set<List<String>> gavs = collectGavs(componentIds);

    int deletedCount = super.deleteComponents(componentIds);

    if (!gavs.isEmpty()) {
      List<String[]> metadataLocations = gavs.stream()
          .flatMap(gav -> getMetadataLocations(gav.get(0), gav.get(1), gav.get(2)).stream())
          .collect(toList());

      metadataRebuilder.deleteMetadata(getRepository(), metadataLocations);
    }
    return deletedCount;
  }

  private Set<List<String>> collectGavs(final int[] componentIds) {
    ContentFacetSupport contentFacet = (ContentFacetSupport) contentFacet();
    ComponentStore<?> componentStore = contentFacet.stores().componentStore;
    return Arrays.stream(componentIds)
        .mapToObj(componentStore::readComponent)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::collectGav)
        .map(Arrays::asList)
        .collect(toSet());
  }

  public Set<String> deleteMetadata(final Component component) {
    String[] gav = collectGav(component);
    return metadataRebuilder.deleteMetadata(getRepository(), getMetadataLocations(gav[0], gav[1], gav[2]));
  }

  private String[] collectGav(Component component) {
    return new String[]{
        component.namespace(), component.name(),
        component.attributes(Maven2Format.NAME).get(P_BASE_VERSION, String.class)
    };
  }

  private List<String[]> getMetadataLocations(
      final String groupId,
      final String artifactId,
      final String baseVersion)
  {
    Repository repository = getRepository();
    ContentFacet contentFacet = repository.facet(ContentFacet.class);

    List<String[]> metadataCoordinates = new ArrayList<>();
    metadataCoordinates.add(new String[]{groupId, artifactId, baseVersion});

    boolean isGroupArtifactEmpty = contentFacet.components().versions(groupId, artifactId).isEmpty();
    if (isGroupArtifactEmpty) {
      metadataCoordinates.add(new String[]{groupId, artifactId, null});
    }
    else {
      contentFacet.assets()
          .path(prependIfMissing(metadataPath(groupId, artifactId, null).getPath(), "/"))
          .find()
          .ifPresent(asset -> asset.withAttribute(METADATA_REBUILD_KEY, true));
    }

    boolean isGroupEmpty = contentFacet.components().names(groupId).isEmpty();
    if (isGroupEmpty) {
      metadataCoordinates.add(new String[]{groupId, null, null});
    }
    else {
      contentFacet.assets()
          .path(prependIfMissing(metadataPath(groupId, null, null).getPath(), "/"))
          .find()
          .ifPresent(asset -> asset.withAttribute(METADATA_REBUILD_KEY, true));
    }
    return metadataCoordinates;
  }
}
