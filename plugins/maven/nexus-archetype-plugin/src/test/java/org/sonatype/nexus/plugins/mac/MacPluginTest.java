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

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.NexusIndexer;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MacPluginTest
    extends AbstractMacPluginTest
{
  protected void prepareNexusIndexer(final NexusIndexer nexusIndexer)
      throws Exception
  {
    context =
        nexusIndexer.addIndexingContext("test-default", "test", repoDir, indexLuceneDir, null, null,
            DEFAULT_CREATORS);
    assertThat(context.getTimestamp(), nullValue()); // unknown upon creation
    nexusIndexer.scan(context);
    assertThat(context.getTimestamp(), notNullValue());
  }

  protected void unprepareNexusIndexer(final NexusIndexer nexusIndexer)
      throws Exception
  {
    nexusIndexer.removeIndexingContext(context, false);
  }

  @Test
  public void testCatalog()
      throws Exception
  {
    prepareNexusIndexer(nexusIndexer);
    try {
      final MacRequest request = new MacRequest(context.getRepositoryId());
      // get catalog
      ArchetypeCatalog catalog = macPlugin.listArcherypesAsCatalog(request, context);
      // repo has 3 artifacts indexed (plus 3 "internal" fields)
      assertThat("We have at least 3 Lucene documents in there for 3 artifacts!", context.getSize() >= 6);
      // repo has only 1 archetype
      assertThat("Catalog not exact!", catalog.getArchetypes(), hasSize(1));
      // add one archetype
      ArtifactInfo artifactInfo =
          new ArtifactInfo(context.getRepositoryId(), "org.sonatype.nexus.plugins", "nexus-archetype-plugin", "1.0",
              null);
      artifactInfo.packaging = "maven-archetype";
      ArtifactContext ac = new ArtifactContext(null, null, null, artifactInfo, artifactInfo.calculateGav());
      nexusIndexer.addArtifactToIndex(ac, context);
      // get catalog again
      catalog = macPlugin.listArcherypesAsCatalog(request, context);
      // repo has 4 artifacts indexed (plus 3 "internal" fields)
      assertThat("We have at least 4 Lucene documents in there for 3 artifacts!", context.getSize() >= 7);
      // repo has only 2 archetypes
      assertThat("Catalog not exact!", catalog.getArchetypes(), hasSize(2));
    }
    finally {
      unprepareNexusIndexer(nexusIndexer);
    }
  }
}
