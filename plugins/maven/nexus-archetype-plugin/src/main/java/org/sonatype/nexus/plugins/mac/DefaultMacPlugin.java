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

import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.Query;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.index.AndMultiArtifactInfoFilter;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class DefaultMacPlugin
    implements MacPlugin
{
  private final NexusIndexer indexer;

  @Inject
  public DefaultMacPlugin(final NexusIndexer indexer) {
    this.indexer = checkNotNull(indexer);
  }

  /**
   * Lists available archatypes for given request.
   */
  protected IteratorSearchResponse listArchetypes(final MacRequest request, final IndexingContext ctx)
      throws IOException
  {
    // construct the query: we search for artifacts having packing "maven-archetype" exactly and nothing else
    final Query pq = indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("maven-archetype"));

    // NEXUS-5216: one and only one context must be given. If not given, we have to return "empty hands",
    // otherwise MI will initiate "untargeted" search. When running in Nexus, it will result in totally invalid
    // catalog, containing archetypes from all but this repository.
    if (ctx == null) {
      return IteratorSearchResponse.empty(pq);
    }

    // to have sorted results by version in descending order
    final IteratorSearchRequest sreq = new IteratorSearchRequest(pq, ctx);
    // filter that filters out classified artifacts
    final ClassifierArtifactInfoFilter classifierFilter = new ClassifierArtifactInfoFilter();

    // combine it with others if needed (unused in cli, but perm filtering in server!)
    if (request.getArtifactInfoFilter() != null) {
      final AndMultiArtifactInfoFilter andArtifactFilter =
          new AndMultiArtifactInfoFilter(Arrays.asList(new ArtifactInfoFilter[]{
              classifierFilter,
              request.getArtifactInfoFilter()
          }));
      sreq.setArtifactInfoFilter(andArtifactFilter);
    }
    else {
      sreq.setArtifactInfoFilter(classifierFilter);
    }

    return indexer.searchIterator(sreq);
  }

  public ArchetypeCatalog listArcherypesAsCatalog(final MacRequest request, final IndexingContext ctx)
      throws IOException
  {
    final IteratorSearchResponse infos = listArchetypes(request, ctx);

    try {
      final ArchetypeCatalog catalog = new ArchetypeCatalog();
      Archetype archetype = null;
      // fill it in
      for (ArtifactInfo info : infos) {
        archetype = new Archetype();
        archetype.setGroupId(info.groupId);
        archetype.setArtifactId(info.artifactId);
        archetype.setVersion(info.version);
        archetype.setDescription(info.description);

        if (StringUtils.isNotEmpty(request.getRepositoryUrl())) {
          archetype.setRepository(request.getRepositoryUrl());
        }
        catalog.addArchetype(archetype);
      }
      return catalog;
    }
    finally {
      if (infos != null) {
        infos.close();
      }
    }
  }

  // ==

  /**
   * Filters to strip-out possible sub-artifacts of artifacts having packaging "maven-archetype".
   *
   * @author cstamas
   */
  public static class ClassifierArtifactInfoFilter
      implements ArtifactInfoFilter
  {
    public boolean accepts(IndexingContext ctx, ArtifactInfo ai) {
      return StringUtils.isBlank(ai.classifier);
    }
  }
}
