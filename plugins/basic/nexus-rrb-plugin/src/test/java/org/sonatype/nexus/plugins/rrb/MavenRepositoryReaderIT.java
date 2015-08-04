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
package org.sonatype.nexus.plugins.rrb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.jettytestsuite.BlockingServer;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.http.QueryStringBuilder;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import junit.framework.Assert;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * In this test we use example repo files that placed in the test resource catalogue To access these files locally via
 * MavenRepositoryReader that requires the http-protocol we start a Jetty server
 *
 * @author bjorne
 */
public class MavenRepositoryReaderIT
    extends TestSupport
{
  MavenRepositoryReader reader; // The "class under test"

  Server server; // An embedded Jetty server

  String localUrl = "http://local"; // This URL doesn't matter for the tests

  String nameOfConnector; // This is the host:portnumber of the Jetty connector

  @Mock
  private QueryStringBuilder queryStringBuilder;

  @Mock
  private RemoteProxySettings remoteProxySettings;

  @Before
  public void setUp()
      throws Exception
  {
    HttpClient httpClient = new DefaultHttpClient();
    reader = new MavenRepositoryReader(httpClient, queryStringBuilder);

    // Create a Jetty server with a handler that returns the content of the
    // given target (i.e. an emulated html, S3Repo, etc, file from the test
    // resources)
    Handler handler = new AbstractHandler()
    {

      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        String path = target;
        if (path.endsWith("/") && StringUtils.isNotEmpty(request.getParameter("prefix"))) {
          String prefix = request.getParameter("prefix");
          path = path + prefix.replaceAll("/", "-");
        }
        else if (target.endsWith("/")) {
          // might need welcome pages later.
          path += "root";
        }

        response.setStatus(HttpServletResponse.SC_OK);
        InputStream stream = this.getClass().getResourceAsStream(path);

        // added to make old tests work
        // we need to fall back to the file name that matches
        if (stream == null && path.endsWith("root")) {
          path = target;
          stream = this.getClass().getResourceAsStream(path);
        }

        if (stream == null) {
          System.out.println("Error handling: " + path);
        }

        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line = null;
        while ((line = reader.readLine()) != null) {
          result.append(line).append(System.getProperty("line.separator"));
        }
        response.getWriter().println(result.toString());
        ((Request) request).setHandled(true);
      }
    };

    server = new BlockingServer(0); // We choose an arbitrary server port
    server.setHandler(handler); // Assign the handler of incoming requests
    server.start();

    // After starting we must find out the host:port, so we know how to
    // connect to the server in the tests
    for (Connector connector : server.getConnectors()) {
      nameOfConnector = connector.getName();
      break; // We only need one connector name (and there should only be
      // one...)
    }
  }

  @After
  public void shutDown()
      throws Exception
  {
    server.stop();
  }

  /**
   * Auxiliary methods
   */
  private String getRemoteUrl() {
    return "http://" + nameOfConnector + "/";
  }

  private ProxyRepository getFakeProxyRepository(final String remoteUrl) {
    final ProxyRepository repository = Mockito.mock(ProxyRepository.class);
    Mockito.when(repository.getRemoteUrl()).thenReturn(remoteUrl);

    final DefaultRemoteStorageContext rsc = new DefaultRemoteStorageContext(null);
    rsc.setRemoteProxySettings(remoteProxySettings);
    rsc.setRemoteConnectionSettings(new DefaultRemoteConnectionSettings());
    Mockito.when(repository.getRemoteStorageContext()).thenReturn(rsc);

    return repository;
  }

  /**
   * First some tests of architypical test repos
   */
  @Test(timeout = 5000)
  public void testReadHtml() {
    List<RepositoryDirectory> result =
        reader.extract("htmlExample", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(7, result.size());
  }

  @Test(timeout = 5000)
  public void testReadS3() {
    List<RepositoryDirectory> result =
        reader.extract("s3Example", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(13, result.size());
  }

  @Test(timeout = 5000)
  public void testReadProtectedS3() {
    // Fetched from URI http://coova-dev.s3.amazonaws.com/mvn/
    // This S3 repo does _work_ (with maven and/or nexus proxying it), but it's setup (perms) does not allow
    // "public browsing".
    List<RepositoryDirectory> result =
        reader.extract("s3Example-foreign", localUrl,
            getFakeProxyRepository("http://coova-dev.s3.amazonaws.com/mvn/"), "test");
    assertEquals(0, result.size());
  }

  @Test(timeout = 5000)
  public void testReadArtifactory() {
    // In this test the format of the local URL is important
    localUrl =
        "http://localhost:8081/nexus/service/local/repositories/ArtyJavaNet/remotebrowser/http://repo.jfrog.org/artifactory/java.net";
    List<RepositoryDirectory> result =
        reader.extract("Artifactory.java.net.htm", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(30, result.size());
  }

  /**
   * Below follows a set of tests of some typical existing repos. The respectively repo's top level is stored as a
   * file in the ordinary test resource catalog. Each file has a name indicating the repo it is taken from and an
   * extension with the date it was downloaded in the format YYYYMMDD.
   */

  @Test(timeout = 5000)
  public void testAmazon_20100118() {
    // Fetched from URI http://s3.amazonaws.com/maven.springframework.org
    List<RepositoryDirectory> result =
        reader.extract("/", localUrl, getFakeProxyRepository(getRemoteUrl() + "Amazon_20100118"), "test");
    assertEquals(997, result.size());

    for (RepositoryDirectory repositoryDirectory : result) {
      assertFalse(repositoryDirectory.getRelativePath().contains("prefix"));
      assertFalse(repositoryDirectory.getResourceURI().contains("prefix"));
    }
  }

  @Test(timeout = 5000)
  public void testAmazon_20110112_slashCom() {
    // Fetched from URI http://repository.springsource.com/?prifix=maven/bundles/release&delimiter=/
    // and http://repository.springsource.com/maven/bundles/release/com
    List<RepositoryDirectory> result =
        reader.extract("/com/", localUrl, getFakeProxyRepository(getRemoteUrl()
            + "Amazon_20110112/maven/bundles/release"), "test");
    assertEquals("Result: " + result, 1, result.size());

    RepositoryDirectory repositoryDirectory1 = result.get(0);
    Assert.assertFalse(repositoryDirectory1.isLeaf());
    Assert.assertEquals(localUrl + "/com/springsource/", repositoryDirectory1.getResourceURI());
    Assert.assertEquals("/com/springsource/", repositoryDirectory1.getRelativePath());
  }

  @Test
  // ( timeout = 5000 )
  public void testAmazon_20110112_slashRoot()
  {
    // Fetched from URI http://repository.springsource.com/?prifix=maven/bundles/release&delimiter=/
    // and http://repository.springsource.com/maven/bundles/release/
    List<RepositoryDirectory> result =
        reader.extract("/", localUrl,
            getFakeProxyRepository(getRemoteUrl() + "Amazon_20110112/maven/bundles/release"), "test");
    assertEquals("Result: " + result, 2, result.size());

    RepositoryDirectory repositoryDirectory1 = result.get(0);
    Assert.assertFalse(repositoryDirectory1.isLeaf());
    Assert.assertEquals(localUrl + "/com/", repositoryDirectory1.getResourceURI());
    Assert.assertEquals("/com/", repositoryDirectory1.getRelativePath());

    RepositoryDirectory repositoryDirectory2 = result.get(1);
    Assert.assertFalse(repositoryDirectory2.isLeaf());
    Assert.assertEquals(localUrl + "/org/", repositoryDirectory2.getResourceURI());
    Assert.assertEquals("/org/", repositoryDirectory2.getRelativePath());
  }

  @Test(timeout = 5000)
  public void testApache_Snapshots() {
    // Fetched from URI http://repository.apache.org/snapshots
    List<RepositoryDirectory> result =
        reader.extract("Apache_Snapshots_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(9, result.size());
  }

  @Test(timeout = 5000)
  public void testCodehaus_Snapshots() {
    // Fetched from URI http://snapshots.repository.codehaus.org/
    List<RepositoryDirectory> result =
        reader.extract("Codehaus_Snapshots_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(3, result.size());
  }

  @Test(timeout = 5000)
  public void testGoogle_Caja() {
    // Fetched from URI http://google-caja.googlecode.com/svn/maven
    List<RepositoryDirectory> result =
        reader.extract("Google_Caja_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(3, result.size());
  }

  @Test(timeout = 5000)
  public void testGoogle_Oauth() {
    // Fetched from URI http://oauth.googlecode.com/svn/code/maven
    List<RepositoryDirectory> result =
        reader.extract("Google_Oauth_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(4, result.size());
  }

  @Test(timeout = 5000)
  public void testJBoss_Maven_Release_Repository() {
    // Fetched from URI http://repository.jboss.org/maven2/
    List<RepositoryDirectory> result =
        reader.extract("JBoss_Maven_Release_Repository_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()),
            "test");
    assertEquals(201, result.size());
  }

  @Test(timeout = 5000)
  public void testMaven_Central() {
    // Fetched from URI http://repo1.maven.org/maven2
    List<RepositoryDirectory> result =
        reader.extract("Maven_Central_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(647, result.size());
  }

  @Test(timeout = 5000)
  public void testNexus_Repository_Manager() {
    // Fetched from URI http://repository.sonatype.org/content/groups/forge
    List<RepositoryDirectory> result =
        reader.extract("Nexus_Repository_Manager_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(173, result.size());
  }

  @Test(timeout = 5000)
  public void testEviwares_Maven_repo() {
    // Fetched from URI http://www.eviware.com/repository/maven2/
    List<RepositoryDirectory> result =
        reader.extract("Eviwares_Maven_repo_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(67, result.size());
  }

  @Test(timeout = 5000)
  public void testjavaNet_repo() {
    // Fetched from URI http://download.java.net/maven/1/
    List<RepositoryDirectory> result =
        reader.extract("java.net_repo_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(94, result.size());
  }

  @Test(timeout = 5000)
  public void testCodehaus() {
    // Fetched from URI http://repository.codehaus.org/
    List<RepositoryDirectory> result =
        reader.extract("Codehaus_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(5, result.size());
  }

  @Test(timeout = 5000)
  public void testjavaNet2() {
    // Fetched from URI http://download.java.net/maven/2/
    List<RepositoryDirectory> result =
        reader.extract("java.net2_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(57, result.size());
  }

  @Test(timeout = 5000)
  public void testOpenIonaCom_Releases() {
    // Fetched from URI http://repo.open.iona.com/maven2/
    List<RepositoryDirectory> result =
        reader.extract("Open.iona.com_Releases_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(8, result.size());
  }

    /*
    * @Test(timeout = 5000) public void testterracotta() { // Fetched from URI http://download.terracotta.org/maven2/
    * List<RepositoryDirectory> result = reader .extract(getURLForTestRepoResource("terracotta_20100118"), localUrl,
    * null, "test"); assertEquals(-1, result.size()); }
    */

  @Test(timeout = 5000)
  public void testSpringsource() {
    // Fetched from URI http://repository.springsource.com/
    List<RepositoryDirectory> result =
        reader.extract("Springsource_20100118", localUrl, getFakeProxyRepository(getRemoteUrl()), "test");
    assertEquals(995, result.size());
  }
}
