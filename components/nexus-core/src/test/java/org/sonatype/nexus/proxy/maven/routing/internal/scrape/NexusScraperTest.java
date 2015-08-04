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
package org.sonatype.nexus.proxy.maven.routing.internal.scrape;

import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.apachehttpclient.page.Page.UnexpectedPageResponse;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.sisu.goodies.common.FormatTemplate;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.base.Throwables;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

public class NexusScraperTest
    extends TestSupport
{
  final static String ROOT_BODY =
      "\n"
          + "<html>\n"
          + "  <head>\n"
          + "    <title>Index of /nexus/content/repositories/central/</title>\n"
          + "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n"
          + "\n"
          +
          "    <link rel=\"icon\" type=\"image/png\" href=\"http://localhost:${port}/nexus//favicon.png\"><!-- Major Browsers -->\n"
          +
          "    <!--[if IE]><link rel=\"SHORTCUT ICON\" href=\"http://localhost:${port}/nexus//favicon.ico\"/><![endif]--><!-- Internet Explorer-->\n"
          + "\n"
          +
          "    <link rel=\"stylesheet\" href=\"http://localhost:${port}/nexus//style/Sonatype-content.css?2.4-SNAPSHOT\" type=\"text/css\" media=\"screen\" title=\"no title\" charset=\"utf-8\">\n"
          + "  </head>\n"
          + "  <body>\n"
          + "    <h1>Index of /nexus/content/repositories/central/</h1>\n"
          + "    <table cellspacing=\"10\">\n"
          + "      <tr>\n"
          + "        <th align=\"left\">Name</th>\n"
          + "        <th>Last Modified</th>\n"
          + "        <th>Size</th>\n"
          + "        <th>Description</th>\n"
          + "      </tr>\n"
          + "      <tr>\n"
          + "        <td>\n"
          + "          <a href=\"../\">Parent Directory</a>\n"
          + "        </td>\n"
          + "      </tr>\n"
          + "                  <tr>\n"
          + "            <td>\n"
          +
          "                              <a href=\"http://localhost:${port}/nexus/content/repositories/central/archetype-catalog.xml\">archetype-catalog.xml</a>\n"
          + "                          </td>\n"
          + "            <td>\n"
          + "              Tue Feb 19 12:12:50 CET 2013\n"
          + "            </td>\n"
          + "            <td align=\"right\">\n"
          + "                              25\n"
          + "                          </td>\n"
          + "            <td>\n"
          + "              &nbsp;\n"
          + "            </td>\n"
          + "          </tr>\n"
          + "                  <tr>\n"
          + "            <td>\n"
          +
          "                              <a href=\"http://localhost:${port}/nexus/content/repositories/central/org/\">org/</a>\n"
          + "                          </td>\n" + "            <td>\n"
          + "              Tue Feb 19 12:14:38 CET 2013\n" + "            </td>\n"
          + "            <td align=\"right\">\n" + "                              &nbsp;\n"
          + "                          </td>\n" + "            <td>\n" + "              &nbsp;\n"
          + "            </td>\n" + "          </tr>\n" + "            </table>\n" + "  </body>\n" + "</html>\n" + "";

  final static String ORG_BODY =
      "\n"
          + "<html>\n"
          + "  <head>\n"
          + "    <title>Index of /nexus/content/repositories/central/org/</title>\n"
          + "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n"
          + "\n"
          +
          "    <link rel=\"icon\" type=\"image/png\" href=\"http://localhost:${port}/nexus//favicon.png\"><!-- Major Browsers -->\n"
          +
          "    <!--[if IE]><link rel=\"SHORTCUT ICON\" href=\"http://localhost:${port}/nexus//favicon.ico\"/><![endif]--><!-- Internet Explorer-->\n"
          + "\n"
          +
          "    <link rel=\"stylesheet\" href=\"http://localhost:${port}/nexus//style/Sonatype-content.css?2.4-SNAPSHOT\" type=\"text/css\" media=\"screen\" title=\"no title\" charset=\"utf-8\">\n"
          + "  </head>\n"
          + "  <body>\n"
          + "    <h1>Index of /nexus/content/repositories/central/org/</h1>\n"
          + "    <table cellspacing=\"10\">\n"
          + "      <tr>\n"
          + "        <th align=\"left\">Name</th>\n"
          + "        <th>Last Modified</th>\n"
          + "        <th>Size</th>\n"
          + "        <th>Description</th>\n"
          + "      </tr>\n"
          + "      <tr>\n"
          + "        <td>\n"
          + "          <a href=\"../\">Parent Directory</a>\n"
          + "        </td>\n"
          + "      </tr>\n"
          + "                  <tr>\n"
          + "            <td>\n"
          +
          "                              <a href=\"http://localhost:${port}/nexus/content/repositories/central/org/sonatype/\">sonatype/</a>\n"
          + "                          </td>\n" + "            <td>\n"
          + "              Tue Feb 19 12:14:38 CET 2013\n" + "            </td>\n"
          + "            <td align=\"right\">\n" + "                              &nbsp;\n"
          + "                          </td>\n" + "            <td>\n" + "              &nbsp;\n"
          + "            </td>\n" + "          </tr>\n" + "            </table>\n" + "  </body>\n" + "</html>\n" + "";

  final static String ORG_SONATYPE_BODY =
      "\n"
          + "<html>\n"
          + "  <head>\n"
          + "    <title>Index of /nexus/content/repositories/central/org/sonatype/</title>\n"
          + "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n"
          + "\n"
          +
          "    <link rel=\"icon\" type=\"image/png\" href=\"http://localhost:${port}/nexus//favicon.png\"><!-- Major Browsers -->\n"
          +
          "    <!--[if IE]><link rel=\"SHORTCUT ICON\" href=\"http://localhost:${port}/nexus//favicon.ico\"/><![endif]--><!-- Internet Explorer-->\n"
          + "\n"
          +
          "    <link rel=\"stylesheet\" href=\"http://localhost:${port}/nexus//style/Sonatype-content.css?2.4-SNAPSHOT\" type=\"text/css\" media=\"screen\" title=\"no title\" charset=\"utf-8\">\n"
          + "  </head>\n"
          + "  <body>\n"
          + "    <h1>Index of /nexus/content/repositories/central/org/sonatype/</h1>\n"
          + "    <table cellspacing=\"10\">\n"
          + "      <tr>\n"
          + "        <th align=\"left\">Name</th>\n"
          + "        <th>Last Modified</th>\n"
          + "        <th>Size</th>\n"
          + "        <th>Description</th>\n"
          + "      </tr>\n"
          + "      <tr>\n"
          + "        <td>\n"
          + "          <a href=\"../\">Parent Directory</a>\n"
          + "        </td>\n"
          + "      </tr>\n"
          + "                  <tr>\n"
          + "            <td>\n"
          +
          "                              <a href=\"http://localhost:${port}/nexus/content/repositories/central/org/sonatype/nexus/\">nexus/</a>\n"
          + "                          </td>\n" + "            <td>\n"
          + "              Tue Feb 19 12:14:38 CET 2013\n" + "            </td>\n"
          + "            <td align=\"right\">\n" + "                              &nbsp;\n"
          + "                          </td>\n" + "            <td>\n" + "              &nbsp;\n"
          + "            </td>\n" + "          </tr>\n" + "            </table>\n" + "  </body>\n" + "</html>\n" + "";

  final static String HOSTED_META_REPOSITORY_METADATA_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<repository-metadata>\n" + "  <version>1.0.0</version>\n"
      + "  <url>http://localhost:${port}/nexus/content/repositories/releases</url>\n" + "  <id>releases</id>\n"
      + "  <name>Releases</name>\n" + "  <layout>maven2</layout>\n" + "  <policy>release</policy>\n"
      + "</repository-metadata>";

  final static String PROXY_META_REPOSITORY_METADATA_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<repository-metadata>\n" + "  <version>1.0.0</version>\n" + "  <url>http://repo1.maven.org/maven2/</url>\n"
      + "  <id>central</id>\n" + "  <name>Central</name>\n" + "  <layout>maven2</layout>\n"
      + "  <policy>release</policy>\n"
      + "  <localUrl>http://localhost:${port}/nexus/content/repositories/central</localUrl>\n"
      + "</repository-metadata>";

  final static String GROUP_META_REPOSITORY_METADATA_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<repository-metadata>\n" + "  <version>1.0.0</version>\n"
      + "  <url>http://localhost:${port}/nexus/content/groups/public</url>\n" + "  <id>public</id>\n"
      + "  <name>Public Repositories</name>\n" + "  <layout>maven2</layout>\n" + "  <policy>mixed</policy>\n"
      + "  <memberRepositories>\n" + "    <memberRepository>\n" + "      <id>releases</id>\n"
      + "      <name>Releases</name>\n" + "      <policy>release</policy>\n"
      + "      <url>http://localhost:${port}/nexus/content/repositories/releases</url>\n"
      + "    </memberRepository>\n" + "    <memberRepository>\n" + "      <id>snapshots</id>\n"
      + "      <name>Snapshots</name>\n" + "      <policy>snapshot</policy>\n"
      + "      <url>http://localhost:${port}/nexus/content/repositories/snapshots</url>\n"
      + "    </memberRepository>\n" + "    <memberRepository>\n" + "      <id>thirdparty</id>\n"
      + "      <name>3rd party</name>\n" + "      <policy>release</policy>\n"
      + "      <url>http://localhost:${port}/nexus/content/repositories/thirdparty</url>\n"
      + "    </memberRepository>\n" + "    <memberRepository>\n" + "      <id>central</id>\n"
      + "      <name>Central</name>\n" + "      <policy>release</policy>\n"
      + "      <url>http://localhost:${port}/nexus/content/repositories/central</url>\n"
      + "    </memberRepository>\n" + "  </memberRepositories>\n" + "</repository-metadata>";

  final static String SHADOW_META_REPOSITORY_METADATA_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<repository-metadata>\n" + "  <version>1.0.0</version>\n"
      + "  <url>http://localhost:${port}/nexus/content/repositories/central-m1</url>\n" + "  <id>central-m1</id>\n"
      + "  <name>Central M1 shadow</name>\n" + "  <layout>maven1</layout>\n" + "  <policy>release</policy>\n"
      + "</repository-metadata>";

  enum RemoteType
  {
    HOSTED, PROXY, GROUP, SHADOW;
  }

  @Mock
  private MavenProxyRepository mavenProxyRepository;

  protected String getRepositoryMetadataXml(final RemoteType remoteType) {
    switch (remoteType) {
      case HOSTED:
        return HOSTED_META_REPOSITORY_METADATA_XML;
      case PROXY:
        return PROXY_META_REPOSITORY_METADATA_XML;
      case GROUP:
        return GROUP_META_REPOSITORY_METADATA_XML;
      case SHADOW:
        return SHADOW_META_REPOSITORY_METADATA_XML;
    }
    throw new IllegalArgumentException("Bad joke!");
  }

  private NexusScraper nexusScraper;

  @Before
  public void prepare()
      throws Exception
  {
    nexusScraper = new NexusScraper();
  }

  protected NexusScraper getScraper() {
    return nexusScraper;
  }

  protected static class ServerPortReader
  {
    private final Server server;

    public ServerPortReader(final Server server) {
      this.server = server;
    }

    public String toString() {
      return String.valueOf(server.getPort());
    }
  }

  public static FormatTemplate template(final String format, final Server server) {
    return new FormatTemplate(format, new Object[0])
    {
      @Override
      protected String render() {
        final Interpolator interpolator = new RegexBasedInterpolator();
        final HashMap<String, Object> values = new HashMap<String, Object>();
        values.put("port", new ServerPortReader(server));
        interpolator.addValueSource(new MapBasedValueSource(values));
        try {
          return interpolator.interpolate(format);
        }
        catch (InterpolationException e) {
          Throwables.propagate(e);
          return null;
        }
      }
    };
  }

  protected Server prepareServer(int code, final RemoteType remoteType)
      throws Exception
  {
    if (code == 200) {
      final Server result = Server.withPort(0);
      result.serve("/nexus/content/repositories/central/.meta/repository-metadata.xml").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(getRepositoryMetadataXml(remoteType),
              result)));
      result.serve("/nexus/content/repositories/central/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ROOT_BODY, result)));
      result.serve("/nexus/content/repositories/central/org/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ORG_BODY, result)));
      result.serve("/nexus/content/repositories/central/org/sonatype/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ORG_SONATYPE_BODY, result)));
      return result;
    }
    else if (code == 403) {
      final Server result = Server.withPort(0);
      result.serve("/*").withBehaviours(new DeliverBehaviour(403, "text/html", "<h1>Access denied</h1>"));
      return result;
    }
    else if (code == 404) {
      final Server result = Server.withPort(0);
      result.serve("/*").withBehaviours(new DeliverBehaviour(404, "text/html", "<h1>Not found</h1>"));
      return result;
    }
    else if (code == 500) {
      final Server result = Server.withPort(0);
      result.serve("/*").withBehaviours(new DeliverBehaviour(500, "text/html", "<h1>Ooops!</h1>"));
      return result;
    }
    else {
      throw new IllegalArgumentException("Code " + code + " not supported!");
    }
  }

  protected Server prepareServerWithCatch(int code, final RemoteType remoteType)
      throws Exception
  {
    if (code == 200) {
      final Server result = Server.withPort(0);
      result.serve("/nexus/content/repositories/central/.meta/repository-metadata.xml").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(getRepositoryMetadataXml(remoteType),
              result)));
      result.serve("/nexus/content/repositories/central/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ROOT_BODY, result)));
      result.serve("/nexus/content/repositories/central/org/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ORG_BODY, result)));
      result.serve("/nexus/content/repositories/central/org/sonatype/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ORG_SONATYPE_BODY, result)));
      return result;
    }
    else if (code == 403) {
      final Server result = Server.withPort(0);
      result.serve("/nexus/content/repositories/central/.meta/repository-metadata.xml").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(getRepositoryMetadataXml(remoteType),
              result)));
      result.serve("/nexus/content/repositories/central/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ROOT_BODY, result)));
      result.serve("/nexus/content/repositories/central/org/").withBehaviours(
          new DeliverBehaviour(403, "text/html", "<h1>Access denied</h1>"));
      return result;
    }
    else if (code == 404) {
      final Server result = Server.withPort(0);
      result.serve("/nexus/content/repositories/central/.meta/repository-metadata.xml").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(getRepositoryMetadataXml(remoteType),
              result)));
      result.serve("/nexus/content/repositories/central/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ROOT_BODY, result)));
      result.serve("/nexus/content/repositories/central/org/").withBehaviours(
          new DeliverBehaviour(404, "text/html", "<h1>Access denied</h1>"));
      return result;
    }
    else if (code == 500) {
      final Server result = Server.withPort(0);
      result.serve("/nexus/content/repositories/central/.meta/repository-metadata.xml").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(getRepositoryMetadataXml(remoteType),
              result)));
      result.serve("/nexus/content/repositories/central/").withBehaviours(
          new DeliverTemplateBehaviour(200, "text/html", template(ROOT_BODY, result)));
      result.serve("/nexus/content/repositories/central/org/").withBehaviours(
          new DeliverBehaviour(500, "text/html", "<h1>Access denied</h1>"));
      return result;
    }
    else {
      throw new IllegalArgumentException("Code " + code + " not supported!");
    }
  }

  // ==

  @Test
  public void simple200()
      throws Exception
  {
    final Server server = prepareServer(200, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(true));
      assertThat(context.getPrefixSource(), notNullValue());
      final List<String> entries = context.getPrefixSource().readEntries();
      assertThat(entries, notNullValue());
      assertThat(entries.size(), equalTo(2));
      assertThat(entries, contains("/archetype-catalog.xml", "/org/sonatype"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void simple200Proxy()
      throws Exception
  {
    final Server server = prepareServer(200, RemoteType.PROXY);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
      assertThat(context.getMessage(), containsString("is a proxy"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void simple200Group()
      throws Exception
  {
    final Server server = prepareServer(200, RemoteType.GROUP);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
      assertThat(context.getMessage(), containsString("is a group"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void simple200Shadow()
      throws Exception
  {
    final Server server = prepareServer(200, RemoteType.SHADOW);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
      assertThat(context.getMessage(), containsString("is not a Maven2"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void simple403()
      throws Exception
  {
    final Server server = prepareServer(403, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(false));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void simple404()
      throws Exception
  {
    final Server server = prepareServer(404, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(false));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test(expected = UnexpectedPageResponse.class)
  public void simple500()
      throws Exception
  {
    final Server server = prepareServer(500, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(false));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  // == In-scrape-failure:
  // Scenario when _during_ scrape some subsequent page returns unexpected result.
  // Here, context must be stopped as we did recognize it as SVN, but error prevented
  // us from scraping it.

  @Test
  public void inDive200()
      throws Exception
  {
    final Server server = prepareServerWithCatch(200, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(true));
      assertThat(context.getPrefixSource(), notNullValue());
      final List<String> entries = context.getPrefixSource().readEntries();
      assertThat(entries, notNullValue());
      assertThat(entries.size(), equalTo(2));
      assertThat(entries, contains("/archetype-catalog.xml", "/org/sonatype"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void inDive403()
      throws Exception
  {
    final Server server = prepareServerWithCatch(403, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void inDive404()
      throws Exception
  {
    final Server server = prepareServerWithCatch(404, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void inDive500()
      throws Exception
  {
    final Server server = prepareServerWithCatch(500, RemoteType.HOSTED);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/nexus/content/repositories/central/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      getScraper().scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

}
