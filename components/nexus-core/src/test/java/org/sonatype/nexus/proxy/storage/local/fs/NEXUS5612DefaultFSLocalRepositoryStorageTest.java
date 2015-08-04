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
package org.sonatype.nexus.proxy.storage.local.fs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;

import com.google.common.primitives.Ints;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

/**
 * Tests {@link DefaultFSLocalRepositoryStorage} in relation to issue https://issues.sonatype.org/browse/NEXUS-5612
 */
public class NEXUS5612DefaultFSLocalRepositoryStorageTest
    extends AbstractProxyTestEnvironment
{
  private static final String MINE_MESSAGE = "You stepped a mine!";

  private final String REPO_ID = "testrepo";

  private File localStorageDirectory;

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    // we need one hosted repo only, so build it
    return new EnvironmentBuilder()
    {
      @Override
      public void startService() {
      }

      @Override
      public void stopService() {
      }

      @Override
      public void buildEnvironment(AbstractProxyTestEnvironment env)
          throws ConfigurationException, IOException, ComponentLookupException
      {
        final PlexusContainer container = env.getPlexusContainer();
        localStorageDirectory =
            env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + REPO_ID);

        // ading one hosted only
        final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
        CRepository repoConf = new DefaultCRepository();
        repoConf.setProviderRole(Repository.class.getName());
        repoConf.setProviderHint("maven2");
        repoConf.setId(REPO_ID);
        repoConf.setName(REPO_ID);
        repoConf.setLocalStorage(new CLocalStorage());
        repoConf.getLocalStorage().setProvider("file");
        repoConf.getLocalStorage().setUrl(localStorageDirectory.toURI().toURL().toString());
        Xpp3Dom exRepo = new Xpp3Dom("externalConfiguration");
        repoConf.setExternalConfiguration(exRepo);
        M2RepositoryConfiguration exRepoConf = new M2RepositoryConfiguration(exRepo);
        exRepoConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
        repo.configure(repoConf);
        env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
        env.getRepositoryRegistry().addRepository(repo);
      }
    };
  }

  private static class TestingDefaultFSPeer
      extends DefaultFSPeer
  {
    private final List<String> minedPaths;

    public TestingDefaultFSPeer(List<String> minedPaths) {
      this.minedPaths = minedPaths;
    }

    @Override
    public Collection<File> listItems(final Repository repository, final File repositoryBaseDir,
                                      final ResourceStoreRequest request, final File target)
        throws ItemNotFoundException, LocalStorageException
    {
      if (minedPaths.contains(request.getRequestPath())) {
        throw new LocalStorageException(MINE_MESSAGE);
      }
      return super.listItems(repository, repositoryBaseDir, request, target);
    }
  }

  /**
   * Tests interaction when listing a directory and FSPeer throws LocalStorageException due to {@link File#list()}
   * returning null.
   */
  @Test
  public void testListFilesThrowsLocalStorageException()
      throws Exception
  {
    // prepare a repo to walk, copy some stuff under local storage
    FileUtils.copyDirectory(getTestFile("target/test-classes/repo1"), localStorageDirectory);
    // count the existing files
    final int filesInRepo = Ints.checkedCast(fileSizesInDirectory(localStorageDirectory));

    // list of parents that will crack
    final List<String> minedParents = Collections.singletonList("/activemq/activemq-core/1.2");
    // list of visited children during walk
    final List<String> visitedChildren = new ArrayList<String>();
    // list of parent collections having onCollectionExit invoked with
    final List<String> exitedCollections = new ArrayList<String>();

    // get the repo and tap in modified LS implementation
    final MavenHostedRepository testRepo =
        getRepositoryRegistry().getRepositoryWithFacet(REPO_ID, MavenHostedRepository.class);
    // tap in FSPeer that has mines planted
    // everything as usual, exception FSPeer implementation
    testRepo.setLocalStorage(new DefaultFSLocalRepositoryStorage(lookup(Wastebasket.class),
        lookup(LinkPersister.class), lookup(MimeSupport.class), new TestingDefaultFSPeer(minedParents)));

    // Create context and processors for the walk
    final DefaultWalkerContext context = new DefaultWalkerContext(testRepo, new ResourceStoreRequest("/"));
    context.getProcessors().add(new AbstractFileWalkerProcessor()
    {
      @Override
      protected void processFileItem(WalkerContext context, StorageFileItem fItem)
          throws Exception
      {
        visitedChildren.add(fItem.getPath());
      }

      @Override
      public void onCollectionExit(WalkerContext context, StorageCollectionItem coll)
          throws Exception
      {
        exitedCollections.add(coll.getPath());
      }

    });
    final Walker walker = lookup(Walker.class);

    // walk
    try {
      walker.walk(context);
      assertThat("Walk must fail as we planted a mine in there!", false);
    }
    catch (WalkerException e) {
      assertThat("Reason must state walk is \"aborted\"", e.getMessage().toLowerCase().contains("aborted"));
      assertThat("Reason must refer to our repo ID=" + REPO_ID,
          e.getMessage().toLowerCase().contains(REPO_ID.toLowerCase()));
      assertThat("Cause must be LocalStorageException", e.getCause() instanceof LocalStorageException);
      assertThat("Cause message must be the one we defined", e.getCause().getMessage(), equalTo(MINE_MESSAGE));
      assertThat("Context must be marked as stopped", context.isStopped());
      assertThat("Context stop-cause must be same as WalkerException's cause",
          context.getStopCause() == e.getCause());
      assertThat("Walk must stop before visiting all files", visitedChildren.size(), lessThan(filesInRepo));
      for (String minedParent : minedParents) {
        assertThat("WalkerProcessor#onCollectionExit must not be invoked with parent being mined",
            exitedCollections, not(contains(minedParent)));
      }
    }
  }

  /**
   * Recursively sum file sizes in a directory.
   *
   * @return count of files in directory.
   */
  private static long fileSizesInDirectory(final File directory)
      throws IllegalArgumentException
  {
    if (!directory.exists()) {
      final String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      final String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    return fileSizesInDirectorySilently(directory, null);
  }

  private static long fileSizesInDirectorySilently(final File directory, final FileFilter filter) {
    long size = 0;

    final File[] files = directory.listFiles(filter);

    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        final File file = files[i];

        if (file.isDirectory()) {
          size += fileSizesInDirectorySilently(file, filter);
        }
        else {
          size += file.length();
        }
      }
    }

    return size;
  }

}
