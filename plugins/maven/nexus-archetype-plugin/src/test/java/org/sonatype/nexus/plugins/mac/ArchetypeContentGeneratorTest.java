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
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.index.DefaultIndexerManager;
import org.sonatype.nexus.index.IndexArtifactFilter;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.RepositoryURLBuilder;

import org.apache.maven.index.NexusIndexer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.mock;

/**
 * Test that verifies that "inconsistent state" when archetype-content XML generated it IS present in repo root, but
 * the
 * corresponding repository does NOT have indexing context for whatever reason, no NPE happens but the fact is caught
 * by
 * corresponding IOEx.
 *
 * @author cstamas
 * @since 2.6
 */
public class ArchetypeContentGeneratorTest
    extends AbstractMacPluginTest
{
  protected IndexerManager indexerManager;

  protected RepositoryRegistry repositoryRegistry;

  @Before
  public void prepare()
      throws Exception
  {
    indexerManager = lookup(IndexerManager.class);
    repositoryRegistry = lookup(RepositoryRegistry.class);
  }

  protected void prepareNexusIndexer(final NexusIndexer nexusIndexer, final Repository repository)
      throws Exception
  {
    indexerManager.addRepositoryIndexContext(repository.getId());
  }

  protected void unprepareNexusIndexer(final NexusIndexer nexusIndexer, final Repository repository)
      throws Exception
  {
    indexerManager.removeRepositoryIndexContext(repository.getId(), true);
  }

  protected M2Repository createRepository(final String id)
      throws Exception
  {
    // adding one proxy
    final M2Repository repo = (M2Repository) lookup(Repository.class, "maven2");
    CRepository repoConf = new DefaultCRepository();
    repoConf.setProviderRole(Repository.class.getName());
    repoConf.setProviderHint("maven2");
    repoConf.setId(id);
    repoConf.setName(id);
    repoConf.setNotFoundCacheActive(true);
    repoConf.setLocalStorage(new CLocalStorage());
    repoConf.getLocalStorage().setProvider("file");
    repoConf.getLocalStorage().setUrl(new File(getWorkHomeDir(), "proxy/store/" + id).toURI().toURL().toString());
    Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
    repoConf.setExternalConfiguration(ex);
    M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);
    exConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
    exConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
    repo.configure(repoConf);
    return repo;
  }

  @Test
  public void testWithoutPreparedContext()
      throws Exception
  {
    final M2Repository repository = createRepository("test");
    repository.setIndexable(false);
    repository.commitChanges();
    repositoryRegistry.addRepository(repository); // need to be in registry to hand it over to indexerManager
    prepareNexusIndexer(nexusIndexer, repository); // indexerManager creates context for it
    try {
      final ArchetypeContentGenerator archetypeContentGenerator =
          new ArchetypeContentGenerator(lookup(MacPlugin.class), (DefaultIndexerManager) indexerManager,
              mock(IndexArtifactFilter.class), mock(RepositoryURLBuilder.class));
      final StorageFileItem item = mock(StorageFileItem.class);
      final ArchetypeContentLocator archetypeContentLocator =
          (ArchetypeContentLocator) archetypeContentGenerator.generateContent(repository,
              "/archetype-catalog.xml", item);

      try {
        final InputStream content = archetypeContentLocator.getContent(); // here IOEx is thrown
        Assert.fail("The getContent() method should throw IOEx");
      }
      catch (IOException e) {
        assertThat(e.getMessage(), endsWith("no IndexingContext exists!"));
      }
    }
    finally {
      unprepareNexusIndexer(nexusIndexer, repository);
    }
  }
}
