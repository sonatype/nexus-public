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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.NexusAppTestSupport;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;

public abstract class AbstractMacPluginTest
    extends NexusAppTestSupport
{
  public List<IndexCreator> DEFAULT_CREATORS;

  protected NexusIndexer nexusIndexer;

  protected Directory indexLuceneDir = new RAMDirectory();

  protected File repoDir = new File(getBasedir(), "src/test/repo");

  protected IndexingContext context;

  protected MacPlugin macPlugin;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    DEFAULT_CREATORS = new ArrayList<IndexCreator>();

    IndexCreator min = lookup(IndexCreator.class, MinimalArtifactInfoIndexCreator.ID);
    IndexCreator mavenPlugin = lookup(IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID);
    IndexCreator mavenArchetype = lookup(IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID);

    DEFAULT_CREATORS.add(min);
    DEFAULT_CREATORS.add(mavenPlugin);
    DEFAULT_CREATORS.add(mavenArchetype);

    // FileUtils.deleteDirectory( indexDir );
    nexusIndexer = lookup(NexusIndexer.class);

    macPlugin = lookup(MacPlugin.class);
  }
}
