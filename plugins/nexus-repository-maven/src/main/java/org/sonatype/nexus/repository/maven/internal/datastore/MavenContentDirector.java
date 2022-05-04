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
package org.sonatype.nexus.repository.maven.internal.datastore;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.director.ContentDirector;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import org.apache.commons.lang3.tuple.Pair;

import static org.sonatype.nexus.repository.maven.VersionPolicy.MIXED;
import static org.sonatype.nexus.repository.maven.VersionPolicy.RELEASE;
import static org.sonatype.nexus.repository.maven.VersionPolicy.SNAPSHOT;
import static org.sonatype.nexus.repository.maven.internal.datastore.MavenFacetUtils.isSnapshot;

/**
 * Maven content director for datastore implementation.
 *
 * @since 3.31
 */
@Named(Maven2Format.NAME)
@Singleton
public class MavenContentDirector
    implements ContentDirector
{
  @Override
  public boolean allowMoveTo(final Repository destination) {
    return true;
  }

  @Override
  public boolean allowMoveTo(final FluentComponent component, final Repository destination)
  {
    VersionPolicy versionPolicy = destination.facet(MavenFacet.class).getVersionPolicy();
    if (MIXED.equals(versionPolicy)) {
      return true;
    }
    if (isSnapshot(component)) {
      return SNAPSHOT.equals(versionPolicy);
    }
    else {
      return RELEASE.equals(versionPolicy);
    }
  }

  @Override
  public boolean allowMoveFrom(final Repository source) {
    return true;
  }

  @Override
  public void afterMove(final List<Map<String, String>> components, final Repository destination)
  {
    MavenMetadataRebuildFacet facet = destination.facet(MavenMetadataRebuildFacet.class);
    components.stream()
        .map(component -> Pair.of(component.get("group"), component.get("name")))
        .distinct()
        .forEach(pair -> facet.rebuildMetadata(pair.getLeft(), pair.getRight(), null, false));
  }

  @Override
  public FluentComponent copyComponent(
      final Component source, final Repository destination)
  {
    return destination.facet(MavenContentFacet.class).copy(source);
  }
}
