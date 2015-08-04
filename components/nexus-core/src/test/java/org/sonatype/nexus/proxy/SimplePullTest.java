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
package org.sonatype.nexus.proxy;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCacheCreate;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCacheUpdate;
import org.sonatype.nexus.proxy.events.RepositoryItemEventRetrieve;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.AbstractMavenRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.AbstractRequestStrategy;
import org.sonatype.nexus.proxy.repository.GroupItemNotFoundException;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.util.WrappingInputStream;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SimplePullTest
    extends AbstractProxyTestEnvironment
{

  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(ss);
    return jettyTestsuiteEnvironmentBuilder;
  }

  @Before
  public void startNexus() throws Exception {
    startNx();
  }

  @Test
  public void testSimplePull()
      throws Exception
  {
    StorageItem item = null;

    try {
      item =
          getRootRouter().retrieveItem(
              new ResourceStoreRequest(
                  "/repositories/repo1/activemq/activemq-core/1.2/broken/activemq-core-1.2", false));

      Assert.fail("We should not be able to pull this path!");
    }
    catch (ItemNotFoundException e) {
      // good, the layout says this is not a file!
    }

    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheCreate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2/xstream/xstream/1.2.2/xstream-1.2.2.pom", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheCreate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/groups/test/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(2, getTestEventListener().getEvents().size());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/groups/test/xstream/xstream/1.2.2/xstream-1.2.2.pom", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(2, getTestEventListener().getEvents().size());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(new ResourceStoreRequest("/groups/test/rome/rome/0.9/rome-0.9.pom", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheCreate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

    item = getRootRouter().retrieveItem(new ResourceStoreRequest("/groups/test/repo3.txt", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheCreate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

    item = getRootRouter().retrieveItem(new ResourceStoreRequest("/groups/test/", false));
    Collection<StorageItem> dir = ((StorageCollectionItem) item).list();
    // we should have listed in root only those things/dirs we pulled, se above!
    // ".nexus" is here too!
    // Expected results:
    // test:/.meta (coll)
    // test:/.nexus (coll)
    // repo1:/activemq (coll)
    // repo1:/rome (coll)
    // repo2:/xstream (coll)
    // repo3:/repo3.txt (file)
    assertEquals(6, dir.size());

    // SO FAR, IT's OLD Unit test, except CacheCreate events were changed (it was Cache event).
    // Now below, we add some more, to cover NXCM-3525 too:

    // NXCM-3525
    // Now we expire local cache, and touch the "remote" files to make it newer and hence, to
    // make nexus refetch them and do all the pulls again:

    // expire caches
    getRepositoryRegistry().getRepository("repo1").expireCaches(new ResourceStoreRequest("/"));
    getRepositoryRegistry().getRepository("repo2").expireCaches(new ResourceStoreRequest("/"));
    getRepositoryRegistry().getRepository("repo3").expireCaches(new ResourceStoreRequest("/"));

    // touch remote files
    final long now = System.currentTimeMillis();
    getRemoteFile(getRepositoryRegistry().getRepository("repo1"),
        "/activemq/activemq-core/1.2/activemq-core-1.2.jar").setLastModified(now);
    getRemoteFile(getRepositoryRegistry().getRepository("repo1"), "/rome/rome/0.9/rome-0.9.pom").setLastModified(
        now);
    getRemoteFile(getRepositoryRegistry().getRepository("repo2"), "/xstream/xstream/1.2.2/xstream-1.2.2.pom")
        .setLastModified(
            now);
    getRemoteFile(getRepositoryRegistry().getRepository("repo3"), "/repo3.txt").setLastModified(now);

    // and here we go again
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheUpdate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo2/xstream/xstream/1.2.2/xstream-1.2.2.pom", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheUpdate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/groups/test/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(2, getTestEventListener().getEvents().size());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/groups/test/xstream/xstream/1.2.2/xstream-1.2.2.pom", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(2, getTestEventListener().getEvents().size());
    getTestEventListener().reset();

    item =
        getRootRouter().retrieveItem(new ResourceStoreRequest("/groups/test/rome/rome/0.9/rome-0.9.pom", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheUpdate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

    item = getRootRouter().retrieveItem(new ResourceStoreRequest("/groups/test/repo3.txt", false));
    checkForFileAndMatchContents(item);
    assertEquals(RepositoryItemEventCacheUpdate.class, getTestEventListener().getFirstEvent().getClass());
    assertEquals(RepositoryItemEventRetrieve.class, getTestEventListener().getLastEvent().getClass());
    getTestEventListener().reset();

  }

  @Test
  public void testSimplePullWithRegardingToPathEnding()
      throws Exception
  {

    // pull the stuff from remote, to play with it below
    StorageItem item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));
    checkForFileAndMatchContents(item);

    item =
        getRootRouter().retrieveItem(
            new ResourceStoreRequest("/groups/test/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));
    checkForFileAndMatchContents(item);

    // new test regarding item properties and path endings.
    // All resource storage implementations should behave the same way.
    item = getRootRouter().retrieveItem(new ResourceStoreRequest("/groups/test/activemq", false));
    assertEquals("/groups/test/activemq", item.getPath());
    assertEquals("/groups/test", item.getParentPath());
    assertEquals("activemq", item.getName());

    item = getRootRouter().retrieveItem(new ResourceStoreRequest("/groups/test/activemq/", false));
    assertEquals("/groups/test/activemq", item.getPath());
    assertEquals("/groups/test", item.getParentPath());
    assertEquals("activemq", item.getName());

    // against reposes
    item =
        getRepositoryRegistry().getRepository("repo1").retrieveItem(
            new ResourceStoreRequest("/activemq", false));
    assertEquals("/activemq", item.getPath());
    assertEquals("/", item.getParentPath());
    assertEquals("activemq", item.getName());

    item =
        getRepositoryRegistry().getRepository("repo1").retrieveItem(
            new ResourceStoreRequest("/activemq", false));
    assertEquals("/activemq", item.getPath());
    assertEquals("/", item.getParentPath());
    assertEquals("activemq", item.getName());

    item =
        getRepositoryRegistry().getRepository("repo1").retrieveItem(
            new ResourceStoreRequest("/activemq/activemq-core/1.2", false));
    assertEquals("/activemq/activemq-core/1.2", item.getPath());
    assertEquals("/activemq/activemq-core", item.getParentPath());
    assertEquals("1.2", item.getName());
    assertTrue(StorageCollectionItem.class.isAssignableFrom(item.getClass()));

    StorageCollectionItem coll = (StorageCollectionItem) item;
    Collection<StorageItem> items = coll.list();
    assertEquals(1, items.size());
    StorageItem collItem = items.iterator().next();
    assertEquals("/activemq/activemq-core/1.2/activemq-core-1.2.jar", collItem.getPath());
    assertEquals("activemq-core-1.2.jar", collItem.getName());
    assertEquals("/activemq/activemq-core/1.2", collItem.getParentPath());
  }

  @Test
  public void testSimplePush()
      throws Exception
  {

    ResourceStoreRequest request =
        new ResourceStoreRequest("/repositories/inhouse/activemq/activemq-core/1.2/activemq-core-1.2.jar", true);
    StorageFileItem item =
        (StorageFileItem) getRootRouter().retrieveItem(
            new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));

    getRootRouter().storeItem(request, item.getInputStream(), null);

    assertTrue(FileUtils.contentEquals(
        getFile(getRepositoryRegistry().getRepository("repo1"),
            "/activemq/activemq-core/1.2/activemq-core-1.2.jar"),
        getFile(getRepositoryRegistry().getRepository("inhouse"),
            "/activemq/activemq-core/1.2/activemq-core-1.2.jar")));
  }

  @Test
  public void testSimplePullOfNonexistent()
      throws Exception
  {
    try {
      getRootRouter().retrieveItem(
          new ResourceStoreRequest(
              "/groups/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar-there-is-no-such", false));
      fail();
    }
    catch (ItemNotFoundException e) {
      // good, this is what we need
    }

    try {
      getRootRouter().retrieveItem(
          new ResourceStoreRequest("/groups/test/rome/rome/0.9/rome-0.9.pom-there-is-no-such", false));
      fail();
    }
    catch (ItemNotFoundException e) {
      // good, this is what we need
    }
  }

  @Test
  public void testSimplePullOfSlashEndedFilePaths()
      throws Exception
  {
    try {
      getRootRouter().retrieveItem(
          new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));
    }
    catch (ItemNotFoundException e) {
      fail("Should get the file!");
    }

    try {
      getRootRouter().retrieveItem(
          new ResourceStoreRequest("/repositories/repo1/activemq/activemq-core/1.2/activemq-core-1.2.jar/",
              false));

      fail("The path ends with slash '/'!");
    }
    catch (ItemNotFoundException e) {
      // good
    }
  }

  @Test
  public void testSimpleWithRequestProcessorsNexus3990()
      throws Exception
  {
    // create a simple "counter" request processor
    CounterRequestStrategy crp = new CounterRequestStrategy();

    for (Repository repo : getRepositoryRegistry().getRepositories()) {
      repo.registerRequestStrategy(CounterRequestStrategy.class.getName(), crp);
    }

    // get something from a group
    try {
      getRootRouter().retrieveItem(
          new ResourceStoreRequest(
              "/groups/test/classworlds/classworlds/1.1-alpha-2/classworlds-1.1-alpha-2-nonexistent.pom", false));
      fail("We should not find this!");
    }
    catch (ItemNotFoundException e) {
      // good, we want this, to "process" all reposes
    }

    // counter has to be: 1 (group) + 5 (5 members) == 6
    Assert.assertEquals("RequestProcessors should be invoked for groups and member reposes!", 6,
        crp.getReferredCount());
  }

  @Test
  public void testNexus4985GroupsShouldNotSwallowMemberExceptions()
      throws Exception
  {
    // add another group to make things a bit hairier
    {
      M2GroupRepository group = (M2GroupRepository) lookup(GroupRepository.class, "maven2");

      CRepository repoGroupConf = new DefaultCRepository();

      repoGroupConf.setProviderRole(GroupRepository.class.getName());
      repoGroupConf.setProviderHint("maven2");
      repoGroupConf.setId("another-test");

      repoGroupConf.setLocalStorage(new CLocalStorage());
      repoGroupConf.getLocalStorage().setProvider("file");
      repoGroupConf.getLocalStorage().setUrl(
          getApplicationConfiguration().getWorkingDirectory("proxy/store/another-test").toURI().toURL().toString());

      Xpp3Dom exGroupRepo = new Xpp3Dom("externalConfiguration");
      repoGroupConf.setExternalConfiguration(exGroupRepo);
      M2GroupRepositoryConfiguration exGroupRepoConf = new M2GroupRepositoryConfiguration(exGroupRepo);
      // members are "test" (an existing group, to have group of group) and repo1 that is already member via
      // "test"
      exGroupRepoConf.setMemberRepositoryIds(Arrays.asList("test", "repo1"));
      exGroupRepoConf.setMergeMetadata(true);
      group.configure(repoGroupConf);
      getApplicationConfiguration().getConfigurationModel().addRepository(repoGroupConf);
      getRepositoryRegistry().addRepository(group);
    }

    // now put a hosted repository "inhouse-snapshot" out of service to make output nicer
    final Repository inhouseSnapshot = getRepositoryRegistry().getRepository("inhouse-snapshot");
    inhouseSnapshot.setLocalStatus(LocalStatus.OUT_OF_SERVICE);
    ((AbstractMavenRepository) inhouseSnapshot).commitChanges();

    // so far, what we did: we had few reposes and a group called "test" (that had all the reposes as members).
    // now, we added test and repo1 reposes ta a newly created group, to have groups of groups.
    // we also put a member repo "inhouse-snapshot" out of service.

    // now we ask for something that IS KNOWN TO NOT EXISTS, hence, request will arrive to all members
    // and members of members (recursively), and the response will form a nice tree

    final GroupRepository group =
        getRepositoryRegistry().getRepositoryWithFacet("another-test", GroupRepository.class);

    try {
      group.retrieveItem(new ResourceStoreRequest("/some/path/that/we/know/is/not/existing/123456/12.foo"));
      // anything else should fail
      Assert.fail("We expected an exception here!");
    }
    catch (GroupItemNotFoundException e) {
      final String dumpStr = dumpNotFoundReasoning(e, 0);

      // just for eyes
      System.out.println(dumpStr);

      // Asserts
      // one repo is out of service, this class simple name must exists, one of them
      assertThat(dumpStr, containsString(RepositoryNotAvailableException.class.getSimpleName()));
      assertThat(countOccurence(dumpStr, RepositoryNotAvailableException.class.getSimpleName()), equalTo(1));
      // groups are throwing this one, 2 of them
      assertThat(dumpStr, containsString(GroupItemNotFoundException.class.getSimpleName()));
      assertThat(countOccurence(dumpStr, GroupItemNotFoundException.class.getSimpleName()), equalTo(2));
      // non-groups are throwing this one, 4 of them (counting with space to not include partial matches against
      // GroupItemNotFoundException)
      assertThat(dumpStr, containsString(ItemNotFoundException.class.getSimpleName()));
      assertThat(countOccurence(dumpStr, " " + ItemNotFoundException.class.getSimpleName()), equalTo(4));
    }
  }

  /**
   * NXCM-4582: When Local storage is about to store something, but during "store" operation source stream EOFs, the
   * new LocalStorage exception should be thrown, to differentiate from other "fatal" (like disk full or what not)
   * error.
   */
  @Test
  public void testNXCM4852()
      throws Exception
  {
    final Repository repository = getRepositoryRegistry().getRepository("inhouse");
    final ResourceStoreRequest request =
        new ResourceStoreRequest("/activemq/activemq-core/1.2/activemq-core-1.2.jar", true);

    try {
      repository.storeItem(request, new WrappingInputStream(new ByteArrayInputStream(
          "123456789012345678901234567890".getBytes()))
      {
        @Override
        public int read()
            throws IOException
        {
          int result = super.read();
          if (result == -1) {
            throw new EOFException("Foo");
          }
          else {
            return result;
          }
        }

        @Override
        public int read(final byte[] b, final int off, final int len)
            throws IOException
        {
          int result = super.read(b, off, len);
          if (result == -1) {
            throw new EOFException("Foo");
          }
          return result;
        }
      }, null);

      Assert.fail("We expected a LocalStorageEofException to be thrown");
    }
    catch (LocalStorageEOFException e) {
      // good, we expected this
    }
    finally {
      // now we have to ensure no remnant files exists
      assertThat(repository.getLocalStorage().containsItem(repository, request), is(false));
      // no tmp files should exists either
      assertThat(
          repository.getLocalStorage().listItems(repository, new ResourceStoreRequest("/.nexus/tmp")),
          is(empty()));
    }
  }

  /**
   * NXCM-4582: When remote storage is fetching something, but during "cache" operation source stream EOFs, the new
   * LocalStorage exception should be thrown, to differentiate from other "fatal" (like disk full or what not) error.
   */
  @Test
  public void testNXCM4852EofFromRemote()
      throws Exception
  {
    final int port = jettyTestsuiteEnvironmentBuilder.getServletServer().getPort();
    jettyTestsuiteEnvironmentBuilder.stopService();

    final Server server = Server.withPort(port);
    server.serve("/*").withBehaviours(new DropConnection()).start();
    try {
      final Repository repository = getRepositoryRegistry().getRepository("repo1");
      final ResourceStoreRequest request =
          new ResourceStoreRequest("/activemq/activemq-core/1.2/activemq-core-1.2.jar");

      try {
        final StorageItem item = repository.retrieveItem(request);
        Assert.fail("We expected a LocalStorageEofException to be thrown");
      }
      catch (LocalStorageEOFException e) {
        // good, we expected this
      }
      finally {
        // now we have to ensure no remnant files exists
        assertThat(repository.getLocalStorage().containsItem(repository, request), is(false));
        // no tmp files should exists either
        assertThat(
            repository.getLocalStorage().listItems(repository, new ResourceStoreRequest("/.nexus/tmp")),
            is(empty()));
      }
    }
    finally {
      server.stop();
    }
  }

  public static class DropConnection
      implements Behaviour
  {

    @Override
    public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
        throws Exception
    {
      response.setStatus(200);
      response.setContentType("application/octet-stream");
      response.setContentLength(500);
      response.getOutputStream().write("partialcontent".getBytes());
      response.flushBuffer();
      response.getOutputStream().close();
      return false;
    }
  }

  //

  protected int countOccurence(final String string, final String snippet) {
    int occurrences = 0;
    int index = 0;
    while (index < string.length() && (index = string.indexOf(snippet, index)) >= 0) {
      occurrences++;
      index = index + snippet.length();
    }
    return occurrences;
  }

  protected String dumpNotFoundReasoning(final Throwable t, int depth) {
    final StringBuilder sb = new StringBuilder();

    // newline
    sb.append("\n");

    // indent
    sb.append(Strings.padEnd("", depth * 2, ' '));
    sb.append(t.getClass().getSimpleName()).append("( ").append(t.getMessage()).append(" )");

    if (t instanceof GroupItemNotFoundException) {
      final GroupItemNotFoundException ginf = (GroupItemNotFoundException) t;
      sb.append(" repo=").append(ginf.getReason().getRepository().getId());

      for (Throwable r : ginf.getMemberReasons().values()) {
        sb.append(dumpNotFoundReasoning(r, depth + 1));
      }
    }

    return sb.toString();
  }

  public static class CounterRequestStrategy
      extends AbstractRequestStrategy
  {
    private int referredCount = 0;

    public int getReferredCount() {
      return referredCount;
    }

    @Override
    public void onHandle(Repository repository, ResourceStoreRequest request, Action action)
        throws ItemNotFoundException, IllegalOperationException
    {
      referredCount++;
      super.onHandle(repository, request, action);
    }
  }
}
