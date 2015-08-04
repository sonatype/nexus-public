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

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Test that verifies that root generated file "/archetype-catalog.xml" is installed and uninstall as it should, when
 * repo is added, when it looses indexing context, etc.
 *
 * @author cstamas
 * @since 2.6
 */
public class MacPluginEventSubscriberTest
    extends NexusAppTestSupport
{
  protected IndexerManager indexerManager;

  protected RepositoryRegistry repositoryRegistry;

  protected M2Repository repository;

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Before
  public void prepare()
      throws Exception
  {
    indexerManager = lookup(IndexerManager.class);
    repositoryRegistry = lookup(RepositoryRegistry.class);
    startNx();
    repository = createRepository("test");
    repositoryRegistry.addRepository(repository); // need to be in registry to hand it over to indexerManager
    indexerManager.addRepositoryIndexContext(repository.getId());
  }

  @After
  public void unprepareNexusIndexer()
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
  public void testInstallUninstall()
      throws Exception
  {
    repository = createRepository("test");
    repositoryRegistry.addRepository(repository); // need to be in registry to hand it over to indexerManager
    indexerManager.addRepositoryIndexContext(repository.getId());

    // we just added a repo "as usual", catalog should be installed
    assertThat(
        repository.getLocalStorage().containsItem(repository, new ResourceStoreRequest("/archetype-catalog.xml")),
        is(true));

    // make repo non indexable
    repository.setIndexable(false);
    repository.commitChanges();

    // verify uninstall happened
    assertThat(
        repository.getLocalStorage().containsItem(repository, new ResourceStoreRequest("/archetype-catalog.xml")),
        is(false));

    // make repo indexable again
    repository.setIndexable(true);
    repository.commitChanges();

    // verify install happened
    assertThat(
        repository.getLocalStorage().containsItem(repository, new ResourceStoreRequest("/archetype-catalog.xml")),
        is(true));
  }
}
