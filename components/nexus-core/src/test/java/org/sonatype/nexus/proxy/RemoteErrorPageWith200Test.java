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

import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.proxy.RemoteRepositories.RemoteRepository;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.httpclient.HttpClientRemoteStorage;
import org.sonatype.tests.http.server.api.Behaviour;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MediaType;
import org.junit.Assert;
import org.junit.Test;

public class RemoteErrorPageWith200Test
    extends AbstractProxyTestEnvironment
{

  private RemoteRepositoryStorage remoteStorage;

  private ProxyRepository aProxyRepository;

  private String baseUrl;

  private Content content;

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();
    this.remoteStorage =
        this.lookup(RemoteRepositoryStorage.class, HttpClientRemoteStorage.PROVIDER_STRING);
    aProxyRepository =
        lookup(RepositoryRegistry.class).getRepositoryWithFacet("RemoteErrorPageWith200Test", ProxyRepository.class);
  }

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    this.content = new Content();
    RemoteRepositories remoteRepositories = RemoteRepositories.builder()
        .repo(RemoteRepository.repo("RemoteErrorPageWith200Test").behave(content).build())
        .build();
    this.baseUrl = remoteRepositories.getUrl("RemoteErrorPageWith200Test");
    return new M2TestsuiteEnvironmentBuilder(remoteRepositories);
  }

  @Test
  public void testRemoteReturnsErrorWith200StatusHeadersNotSet()
      throws Exception
  {
    String expectedContent = "my cool expected content";
    content.body = expectedContent;
    content.headers.clear();

    // remote request
    ResourceStoreRequest storeRequest = new ResourceStoreRequest("random/file.txt");
    DefaultStorageFileItem item =
        (DefaultStorageFileItem) remoteStorage.retrieveItem(aProxyRepository, storeRequest, this.baseUrl);

    // result should be HTML
    try (InputStream io = item.getInputStream()) {
      String content = IOUtils.toString(io);
      Assert.assertEquals(expectedContent, content);
    }
  }

  @Test
  public void testRemoteReturnsErrorWith200StatusHeadersSet()
      throws RemoteAccessException, StorageException, ItemNotFoundException
  {
    String expectedContent = "error page";
    content.body = expectedContent;
    content.headers.clear();
    content.headers.put(HttpClientRemoteStorage.NEXUS_MISSING_ARTIFACT_HEADER, "true");

    // remote request
    ResourceStoreRequest storeRequest = new ResourceStoreRequest("random/file.txt");
    try {
      DefaultStorageFileItem item =
          (DefaultStorageFileItem) remoteStorage.retrieveItem(aProxyRepository, storeRequest, this.baseUrl);
      Assert.fail("expected  RemoteStorageException");
    }
    // expect artifact not found
    catch (RemoteStorageException e) {
      // expected
    }
  }

  public static class Content
      implements Behaviour
  {
    public Multimap<String, String> headers = ArrayListMultimap.create();

    public String body = "<html>some content</html>";

    public String bodyContentType = MediaType.TEXT_HTML.toString();

    @Override
    public boolean execute(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Map<Object, Object> ctx)
        throws Exception
    {
      if (!headers.isEmpty()) {
        for (Map.Entry<String, String> entry : headers.entries()) {
          response.addHeader(entry.getKey(), entry.getValue());
        }
      }
      response.setContentType(bodyContentType);
      response.getWriter().write(body);
      return false;
    }
  }
}
