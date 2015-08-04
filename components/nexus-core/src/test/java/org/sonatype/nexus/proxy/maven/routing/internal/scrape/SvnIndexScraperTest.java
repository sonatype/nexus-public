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

import java.util.List;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.apachehttpclient.page.Page.UnexpectedPageResponse;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.tests.http.server.fluent.Server;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

public class SvnIndexScraperTest
    extends TestSupport
{
  final static String ROOT_BODY =
      "<html><head><title>somerepo - Revision 1030: /trunk/somerepo</title></head>\n"
          + "<body>\n"
          + " <h2>somerepo - Revision 1030: /trunk/somerepo</h2>\n"
          + " <ul>\n"
          + "  <li><a href=\"../\">..</a></li>\n"
          + "  <li><a href=\"com/\">com/</a></li>\n"
          + " </ul>\n"
          +
          " <hr noshade><em><a href=\"http://code.google.com/\">Google Code</a> powered by <a href=\"http://subversion.apache.org/\">Subversion</a></em>\n"
          + "</body></html>";

  final static String COM_BODY =
      "<html><head><title>somerepo - Revision 1030: /trunk/somerepo/com</title></head>\n"
          + "<body>\n"
          + " <h2>somerepo - Revision 1030: /trunk/somerepo/com</h2>\n"
          + " <ul>\n"
          + "  <li><a href=\"../\">..</a></li>\n"
          + "  <li><a href=\"foo/\">foo/</a></li>\n"
          + "  <li><a href=\"bar/\">bar/</a></li>\n"
          + " </ul>\n"
          +
          " <hr noshade><em><a href=\"http://code.google.com/\">Google Code</a> powered by <a href=\"http://subversion.apache.org/\">Subversion</a></em>\n"
          + "</body></html>";

  final static String COM_FOO_BODY =
      "<html><head><title>somerepo - Revision 1030: /trunk/somerepo/com/foo</title></head>\n"
          + "<body>\n"
          + " <h2>somerepo - Revision 1030: /trunk/somerepo/com/foo</h2>\n"
          + " <ul>\n"
          + "  <li><a href=\"../\">..</a></li>\n"
          + "  <li><a href=\"foo1/\">foo1/</a></li>\n"
          + "  <li><a href=\"foo2/\">foo2/</a></li>\n"
          + " </ul>\n"
          +
          " <hr noshade><em><a href=\"http://code.google.com/\">Google Code</a> powered by <a href=\"http://subversion.apache.org/\">Subversion</a></em>\n"
          + "</body></html>";

  final static String COM_BAR_BODY =
      "<html><head><title>somerepo - Revision 1030: /trunk/somerepo/com/bar</title></head>\n"
          + "<body>\n"
          + " <h2>somerepo - Revision 1030: /trunk/somerepo//bar</h2>\n"
          + " <ul>\n"
          + "  <li><a href=\"../\">..</a></li>\n"
          + "  <li><a href=\"bar1/\">bar1/</a></li>\n"
          + "  <li><a href=\"bar2/\">bar2/</a></li>\n"
          + " </ul>\n"
          +
          " <hr noshade><em><a href=\"http://code.google.com/\">Google Code</a> powered by <a href=\"http://subversion.apache.org/\">Subversion</a></em>\n"
          + "</body></html>";

  @Mock
  private MavenProxyRepository mavenProxyRepository;

  private SvnIndexScraper svnScraper;

  @Before
  public void prepare()
      throws Exception
  {
    svnScraper = new SvnIndexScraper();
  }

  protected SvnIndexScraper getScraper() {
    return svnScraper;
  }

  protected Server prepareServer(int code)
      throws Exception
  {
    if (code == 200) {
      final Server result = Server.withPort(0);
      result.serve("/trunk/somerepo/").withBehaviours(
          new DeliverBehaviour(200, "text/html", ROOT_BODY));
      result.serve("/trunk/somerepo/com/").withBehaviours(
          new DeliverBehaviour(200, "text/html", COM_BODY));
      result.serve("/trunk/somerepo/com/foo/").withBehaviours(
          new DeliverBehaviour(200, "text/html", COM_FOO_BODY));
      result.serve("/trunk/somerepo/com/bar/").withBehaviours(
          new DeliverBehaviour(200, "text/html", COM_BAR_BODY));
      return result;
    }
    else if (code == 403) {
      final Server result = Server.withPort(0);
      result.serve("/*").withBehaviours(
          new DeliverBehaviour(403, "text/html", "<h1>Access denied</h1>"));
      return result;
    }
    else if (code == 404) {
      final Server result = Server.withPort(0);
      result.serve("/*").withBehaviours(
          new DeliverBehaviour(404, "text/html", "<h1>Not found</h1>"));
      return result;
    }
    else if (code == 500) {
      final Server result = Server.withPort(0);
      result.serve("/*").withBehaviours(
          new DeliverBehaviour(500, "text/html", "<h1>Ooops!</h1>"));
      return result;
    }
    else {
      throw new IllegalArgumentException("Code " + code + " not supported!");
    }
  }

  protected Server prepareServerWithCatch(int code)
      throws Exception
  {
    if (code == 200) {
      final Server result = Server.withPort(0);
      result.serve("/trunk/somerepo/").withBehaviours(
          new DeliverBehaviour(200, "text/html", ROOT_BODY));
      result.serve("/trunk/somerepo/com/").withBehaviours(
          new DeliverBehaviour(200, "text/html", COM_BODY));
      result.serve("/trunk/somerepo/com/foo/").withBehaviours(
          new DeliverBehaviour(200, "text/html", COM_FOO_BODY));
      result.serve("/trunk/somerepo/com/bar/").withBehaviours(
          new DeliverBehaviour(200, "text/html", COM_BAR_BODY));
      return result;
    }
    else if (code == 403) {
      final Server result = Server.withPort(0);
      result.serve("/trunk/somerepo/").withBehaviours(
          new DeliverBehaviour(200, "text/html", ROOT_BODY));
      result.serve("/trunk/somerepo/com/").withBehaviours(
          new DeliverBehaviour(403, "text/html", "<h1>Access denied</h1>"));
      return result;
    }
    else if (code == 404) {
      final Server result = Server.withPort(0);
      result.serve("/trunk/somerepo/").withBehaviours(
          new DeliverBehaviour(200, "text/html", ROOT_BODY));
      result.serve("/trunk/somerepo/com/").withBehaviours(
          new DeliverBehaviour(404, "text/html", "<h1>Not found</h1>"));
      return result;
    }
    else if (code == 500) {
      final Server result = Server.withPort(0);
      result.serve("/trunk/somerepo/").withBehaviours(
          new DeliverBehaviour(200, "text/html", ROOT_BODY));
      result.serve("/trunk/somerepo/com/").withBehaviours(
          new DeliverBehaviour(500, "text/html", "<h1>Ooops!</h1>"));
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
    final Server server = prepareServer(200);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
      assertThat(entries, contains("/com/foo", "/com/bar"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void simple403()
      throws Exception
  {
    final Server server = prepareServer(403);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
    final Server server = prepareServer(404);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
    final Server server = prepareServer(500);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
    final Server server = prepareServerWithCatch(200);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
      assertThat(entries, contains("/com/foo", "/com/bar"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void inDive403()
      throws Exception
  {
    final Server server = prepareServerWithCatch(403);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
    final Server server = prepareServerWithCatch(404);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
    final Server server = prepareServerWithCatch(500);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/trunk/somerepo/";
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
