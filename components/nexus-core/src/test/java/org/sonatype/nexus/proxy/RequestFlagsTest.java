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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.ConfigurableRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Proxy;
import org.sonatype.tests.http.server.fluent.Server;
import org.sonatype.tests.http.server.jetty.behaviour.Record;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.sonatype.tests.http.server.fluent.Behaviours.content;

/**
 * Unit test testing Nexus behaviour (with regard to remote fetches) with default behaviour, and then with the presence
 * of the {@link ResourceStoreRequest} flags like "localOnly", "remoteOnly" and the new "asExpired" and their
 * combination.
 *
 * @author cstamas
 * @since 2.4
 */
public class RequestFlagsTest
    extends NexusAppTestSupport
{
  public static final String PATH = "/test.txt";

  public static final String CONTENT = "foobar123";

  private Server server;

  private Record recordedRequestsBehaviour;

  private LastModifiedSender lastModifiedSender;

  private ProxyRepository proxyRepository;

  @Before
  public void prepare()
      throws Exception
  {
    recordedRequestsBehaviour = new Record();
    // somewhere in near past
    lastModifiedSender =
        new LastModifiedSender(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)));

    server =
        Proxy.withPort(0).serve(PATH).withBehaviours(recordedRequestsBehaviour, lastModifiedSender,
            content(CONTENT)).start();
    startNx();
    proxyRepository = createProxyRepository();
  }

  @After
  public void cleanup()
      throws Exception
  {
    server.stop();
  }

  protected ProxyRepository createProxyRepository()
      throws Exception
  {
    final RepositoryTemplate template =
        (RepositoryTemplate) getRepositoryTemplates().getTemplates(Maven2ContentClass.class,
            RepositoryPolicy.RELEASE, MavenProxyRepository.class).pick();
    final ConfigurableRepository templateConf = template.getConfigurableRepository();
    templateConf.setId("test");
    templateConf.setName("Test");
    final CRemoteStorage remoteStorageConf = new CRemoteStorage();
    remoteStorageConf.setUrl("http://localhost:" + server.getPort());
    template.getCoreConfiguration().getConfiguration(true).setRemoteStorage(remoteStorageConf);
    final MavenProxyRepository mavenProxyRepository = (MavenProxyRepository) template.create();

    return mavenProxyRepository;
  }

  protected List<String> getRecordedRequests() {
    // for me it was a surprise that Record behaviour reverses list, so this
    // here just "undos" that, to make assertions in proper order (as requests happened)
    // instead of doing it in reverse order (from last to 1st)
    // Why does Record reverse requests anyway?
    final List<String> list = new ArrayList<String>(recordedRequestsBehaviour.getRequests());
    Collections.reverse(list);
    return list;
  }

  // == Normal behaviour: this is how Nexus performs in various cases when NO request flag is set at all
  // == Cases like empty cache, primed cache, expired cache and remote is same, and expired cache and remote is newer

  @Test
  public void noFlagEmptyCacheIsServed()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(1));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));

  }

  @Test
  public void noFlagPrimedCacheIsServed()
      throws Exception
  {
    // prime the cache
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(1));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void noFlagExpiredCacheIsServed()
      throws Exception
  {
    // prime the cache and make it expired
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
      proxyRepository.expireCaches(new ResourceStoreRequest("/"));
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(2));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("HEAD"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void noFlagExpiredCacheRemoteIsNewerIsServed()
      throws Exception
  {
    // prime the cache and make it expired
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
      proxyRepository.expireCaches(new ResourceStoreRequest("/"));
    }

    lastModifiedSender.setLastModified(new Date());

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(3));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("HEAD"));
    MatcherAssert.assertThat(recordedRequests.get(2), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  // == LocalOnly flag: this flag PREVENTS nexus to go remote at all (even if it would normally)
  // == All the recorded requests (if any) are made by "priming", not be the flagged request

  @Test
  public void localOnlyFlagWithEmptyCacheIs404()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestLocalOnly(true);
    try {
      proxyRepository.retrieveItem(request);
      Assert.fail("We should get INFEx!");
    }
    catch (ItemNotFoundException e) {
      // good
    }

    MatcherAssert.assertThat(getRecordedRequests(), Matchers.empty());
  }

  @Test
  public void localOnlyFlagWithPrimedCacheIsServed()
      throws Exception
  {
    // prime the cache
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestLocalOnly(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(1));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void localOnlyFlagWithExpiredCacheIsServed()
      throws Exception
  {
    // prime the cache and make it expired
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
      proxyRepository.expireCaches(new ResourceStoreRequest("/"));
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestLocalOnly(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(1));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  // == RemoteOnly: this flag FORCES Nexus to go remotely and always fetch from remote
  // == (even if it would not) and REPLACE the cached content (if any)
  // == Note: as this test shows, with this flag Nexus will delete cache content if remote is 404!

  @Test
  public void remoteOnlyFlagWithEmptyCacheGoesRemoteAndIsServed()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestRemoteOnly(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(1));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void remoteOnlyFlagWithPrimedCacheGoesRemoteAndIsServed()
      throws Exception
  {
    // prime the cache
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestRemoteOnly(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    // BOTH requests will go to remote server!
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(2));
    // BOTH requests were GETs
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void remoteOnlyFlagWithPrimedCacheGoesRemoteAndIsDeletedIfNotFound()
      throws Exception
  {
    // igor: another discrepancy: with remoteOnly Nexus _deletes_
    // local cache content if it was deleted remotely, unlike in
    // "normal" case (this was true since beginning)

    // prime the cache
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
    }

    // recreate server to report 404 for path
    final int port = server.getPort();
    server.stop();
    server =
        Proxy.withPort(port).serve(PATH).withBehaviours(recordedRequestsBehaviour,
            Behaviours.error(404, "Not found")).start();

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestRemoteOnly(true);
    try {
      final StorageItem item = proxyRepository.retrieveItem(request);
      Assert.fail("We should get INFEx!");
    }
    catch (ItemNotFoundException e) {
      // good
    }

    final List<String> recordedRequests = getRecordedRequests();
    // BOTH requests will go to remote server!
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(2));
    // BOTH requests were GETs
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("GET"));

    // cache got removed coz of 404!
    MatcherAssert.assertThat(
        proxyRepository.getLocalStorage().containsItem(proxyRepository, new ResourceStoreRequest(PATH)),
        Matchers.is(false));
  }

  @Test
  public void remotelOnlyFlagWithExpiredCacheGoesRemoteAndIsServed()
      throws Exception
  {
    // prime the cache and make it expired
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
      proxyRepository.expireCaches(new ResourceStoreRequest("/"));
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestRemoteOnly(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    // BOTH requests will go to remote server!
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(2));
    // BOTH requests were GETs
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  // == AsExpired: this flag controls the "expiration", making the item requested behave as "expired"
  // == This means it's better than remoteOnly (that blindly GETs), as this one will do same as Nexus
  // == does with expired items (even if it is actually not expired, nor aged). Usable when payload
  // == is bigger one (to save redundant download if you already have it cached, you just want to be sure it's
  // == same as the remote), or explicitly keeping some local file in sync with remote, without doing
  // == costly "expire proxy caches"

  @Test
  public void asExpireFlagWithEmptyCacheGoesRemoteAndIsServed()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestAsExpired(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(1));
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void asExpiredFlagWithPrimedCacheGoesRemoteAndIsServed()
      throws Exception
  {
    // prime the cache
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestAsExpired(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    // BOTH requests will go to remote server
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(2));
    // But, requests are GET and HEAD (1st is for "prime", 2nd is checking for remote)
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("HEAD"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void asExpiredFlagWithPrimedCacheGoesRemoteAndIsServedWithRemoteNewer()
      throws Exception
  {
    // prime the cache
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
    }

    lastModifiedSender.setLastModified(new Date());

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestAsExpired(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    // BOTH requests will go to remote server (but 2nd will do HEAD only request)!
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(3));
    // But, requests are GET, HEAD and GET (1st is for "prime", 2nd is checking for remote, and 3rd one actually
    // GETs it
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("HEAD"));
    MatcherAssert.assertThat(recordedRequests.get(2), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void asExpiredFlagWithExpiredCacheGoesRemoteAndIsServed()
      throws Exception
  {
    // prime the cache and make it expired
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
      proxyRepository.expireCaches(new ResourceStoreRequest("/"));
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestAsExpired(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    // BOTH requests will go to remote server
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(2));
    // But, requests are GET and HEAD (1st is for "prime", 2nd is checking for remote)
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("HEAD"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  @Test
  public void asExpiredFlagWithExpiredCacheGoesRemoteAndIsServedWithRemoteNewer()
      throws Exception
  {
    // prime the cache and make it expired
    {
      final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
      proxyRepository.retrieveItem(request);
      proxyRepository.expireCaches(new ResourceStoreRequest("/"));
    }

    lastModifiedSender.setLastModified(new Date());

    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestAsExpired(true);
    final StorageItem item = proxyRepository.retrieveItem(request);

    final List<String> recordedRequests = getRecordedRequests();
    // BOTH requests will go to remote server (but 2nd will do TWO HTTP requests)!
    MatcherAssert.assertThat(recordedRequests.size(), Matchers.equalTo(3));
    // But, requests are GET, HEAD and GET (1st is for "prime", 2nd is checking for remote, and 3rd one actually
    // GETs it
    MatcherAssert.assertThat(recordedRequests.get(0), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(recordedRequests.get(1), Matchers.startsWith("HEAD"));
    MatcherAssert.assertThat(recordedRequests.get(2), Matchers.startsWith("GET"));
    MatcherAssert.assertThat(item, Matchers.instanceOf(StorageFileItem.class));

    final String content = IOUtils.toString(((StorageFileItem) item).getContentLocator().getContent());
    MatcherAssert.assertThat(content, Matchers.equalTo(CONTENT));
  }

  // == localOnly + remoteOnly : Not found always, as we forbid use
  // == of local and remote storage, so nowhere to serve from

  @Test(expected = IllegalArgumentException.class)
  public void localAndRemoteOnlyFlagsAreIllegalTogether()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestLocalOnly(true);
    request.setRequestRemoteOnly(true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void localAndExpiredFlagsAreIllegalTogether()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestAsExpired(true);
    request.setRequestLocalOnly(true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void remoteAndExpiredFlagsAreIllegalTogether()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    request.setRequestRemoteOnly(true);
    request.setRequestAsExpired(true);
  }

  // ==

  public class LastModifiedSender
      implements Behaviour
  {
    private Date lastModified;

    public LastModifiedSender(final Date date) {
      setLastModified(date);
    }

    public void setLastModified(final Date when) {
      lastModified = when;
    }

    @Override
    public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
        throws Exception
    {
      response.setDateHeader("last-modified", lastModified.getTime());
      return true;
    }
  }
}
