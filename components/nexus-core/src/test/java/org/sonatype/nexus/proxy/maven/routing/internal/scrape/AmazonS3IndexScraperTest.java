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
import java.util.Map;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.apachehttpclient.page.Page.PageContext;
import org.sonatype.nexus.apachehttpclient.page.Page.UnexpectedPageResponse;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.collect.Maps;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

public class AmazonS3IndexScraperTest
    extends TestSupport
{
  final static String NO_SUCH_KEY_RESPONSE = //
      "<Error>"//
          + "<Code>NoSuchKey</Code>"//
          + "<Message>The specified key does not exist.</Message>"//
          + "<Key>release/</Key>"//
          + "<RequestId>74090E77260B51CF</RequestId>"//
          + "<HostId>s4OjyIur2nB1qBhzVao2j8HzeBtoPrHYcagfxQePqS6+T89adq89IutpSLW3kGiH</HostId>"//
          + "</Error>";

  final static String NO_SUCH_KEY_RESPONSE_ROOT = //
      "<Error>"//
          + "<Code>NoSuchKey</Code>"//
          + "<Message>The specified key does not exist.</Message>"//
          + "<Key>/</Key>"//
          + "<RequestId>74090E77260B51CF</RequestId>"//
          + "<HostId>s4OjyIur2nB1qBhzVao2j8HzeBtoPrHYcagfxQePqS6+T89adq89IutpSLW3kGiH</HostId>"//
          + "</Error>";

  final static String ACCESS_DENIED_RESPONSE = //
      "<Error>"//
          + "<Code>AccessDenied</Code>"//
          + "<Message>Access Denied.</Message>"//
          + "<RequestId>74090E77260B51CF</RequestId>"//
          + "<HostId>s4OjyIur2nB1qBhzVao2j8HzeBtoPrHYcagfxQePqS6+T89adq89IutpSLW3kGiH</HostId>"//
          + "</Error>";

  final static String INTERNAL_ERROR_RESPONSE = //
      "<Error>"//
          + "<Code>InternalError</Code>"//
          + "<Message>Internal error.</Message>"//
          + "<RequestId>74090E77260B51CF</RequestId>"//
          + "<HostId>s4OjyIur2nB1qBhzVao2j8HzeBtoPrHYcagfxQePqS6+T89adq89IutpSLW3kGiH</HostId>"//
          + "</Error>";

  final static String ONE_PAGE_RESPONSE = //
      "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"//
          + "<Name>foo-bar</Name>"//
          + "<Prefix />"//
          + "<Marker />"//
          + "<MaxKeys>1000</MaxKeys>"//
          + "<IsTruncated>false</IsTruncated>"//
          + "<Contents>"//
          + "<Key>release/foo/bar/1/bar-1.jar</Key>"//
          + "<LastModified>2011-08-25T01:38:05.000Z</LastModified>"//
          + "<ETag>\"21c28f400f3c48799fdae89226066a8d\"</ETag>"//
          + "<Size>9298877</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "<Contents>"//
          + "<Key>release/.meta/repository-metadata.xml</Key>"//
          + "<LastModified>2011-08-25T01:38:05.000Z</LastModified>"//
          + "<ETag>\"8bf32bb10b5a9c818739cc0031c67ac9\"</ETag>"//
          + "<Size>267</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "<Contents>"//
          + "<Key>release/foo/baz/1/baz-2.jar</Key>"//
          + "<LastModified>2013-02-02T10:25:26.000Z</LastModified>"//
          + "<ETag>\"9f25f7c8efd60f815626306442d5e71c\"</ETag>"//
          + "<Size>436</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "</ListBucketResult>";

  final static String MULTI_PAGE_RESPONSE_PAGE_1 = //
      "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"//
          + "<Name>foo-bar</Name>"//
          + "<Prefix />"//
          + "<Marker />"//
          + "<MaxKeys>1000</MaxKeys>"//
          + "<IsTruncated>true</IsTruncated>"//
          + "<Contents>"//
          + "<Key>release/foo1/bar/1/bar-1.jar</Key>"//
          + "<LastModified>2011-08-25T01:38:05.000Z</LastModified>"//
          + "<ETag>\"21c28f400f3c48799fdae89226066a8d\"</ETag>"//
          + "<Size>9298877</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "<Contents>"//
          + "<Key>release/.meta/repository-metadata.xml</Key>"//
          + "<LastModified>2011-08-25T01:38:05.000Z</LastModified>"//
          + "<ETag>\"8bf32bb10b5a9c818739cc0031c67ac9\"</ETag>"//
          + "<Size>267</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "<Contents>"//
          + "<Key>release/foo1/baz/1/baz-2.jar</Key>"//
          + "<LastModified>2013-02-02T10:25:26.000Z</LastModified>"//
          + "<ETag>\"9f25f7c8efd60f815626306442d5e71c\"</ETag>"//
          + "<Size>436</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "</ListBucketResult>";

  final static String MULTI_PAGE_RESPONSE_PAGE_2 = //
      "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"//
          + "<Name>foo-bar</Name>"//
          + "<Prefix />"//
          + "<Marker />"//
          + "<MaxKeys>1000</MaxKeys>"//
          + "<IsTruncated>true</IsTruncated>"//
          + "<Contents>"//
          + "<Key>release/foo2/bar/1/bar-1.jar</Key>"//
          + "<LastModified>2011-08-25T01:38:05.000Z</LastModified>"//
          + "<ETag>\"21c28f400f3c48799fdae89226066a8d\"</ETag>"//
          + "<Size>9298877</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "<Contents>"//
          + "<Key>release/foo2/baz/1/baz-2.jar</Key>"//
          + "<LastModified>2013-02-02T10:25:26.000Z</LastModified>"//
          + "<ETag>\"9f25f7c8efd60f815626306442d5e71c\"</ETag>"//
          + "<Size>436</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "</ListBucketResult>";

  final static String MULTI_PAGE_RESPONSE_PAGE_3 = //
      "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"//
          + "<Name>foo-bar</Name>"//
          + "<Prefix />"//
          + "<Marker />"//
          + "<MaxKeys>1000</MaxKeys>"//
          + "<IsTruncated>false</IsTruncated>"//
          + "<Contents>"//
          + "<Key>release/foo3/bar/1/bar-1.jar</Key>"//
          + "<LastModified>2011-08-25T01:38:05.000Z</LastModified>"//
          + "<ETag>\"21c28f400f3c48799fdae89226066a8d\"</ETag>"//
          + "<Size>9298877</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "<Contents>"//
          + "<Key>release/foo3/baz/1/baz-2.jar</Key>"//
          + "<LastModified>2013-02-02T10:25:26.000Z</LastModified>"//
          + "<ETag>\"9f25f7c8efd60f815626306442d5e71c\"</ETag>"//
          + "<Size>436</Size>"//
          + "<StorageClass>STANDARD</StorageClass>"//
          + "</Contents>"//
          + "</ListBucketResult>";

  @Mock
  private MavenProxyRepository mavenProxyRepository;

  private AmazonS3IndexScraper s3scraper;

  @Before
  public void prepare()
      throws Exception
  {
    s3scraper = new AmazonS3IndexScraper();
  }

  /**
   * Server that delivers trivial repository as single page.
   */
  protected Server prepareServer()
      throws Exception
  {
    final Server result = Server.withPort(0);
    result.serve("/release/").withBehaviours(new S3ResponseHeadersBehaviour(),
        new DeliverBehaviour(404, "application/xml", NO_SUCH_KEY_RESPONSE));
    result.serve("/").withBehaviours(new S3ResponseHeadersBehaviour(),
        new DeliverBehaviour(200, "application/xml", ONE_PAGE_RESPONSE));
    return result;
  }

  /**
   * Server that delivers trivial repository as single page, but will hit an error during deliver of repo root.
   */
  protected Server prepareErrorServer(final int code)
      throws Exception
  {
    if (code == 403) {
      final Server result = Server.withPort(0);
      result.serve("/release/").withBehaviours(new S3ResponseHeadersBehaviour(),
          new DeliverBehaviour(404, "application/xml", NO_SUCH_KEY_RESPONSE));
      result.serve("/*").withBehaviours(new S3ResponseHeadersBehaviour(),
          new DeliverBehaviour(403, "application/xml", ACCESS_DENIED_RESPONSE));
      return result;
    }
    else if (code == 404) {
      final Server result = Server.withPort(0);
      result.serve("/release/").withBehaviours(new S3ResponseHeadersBehaviour(),
          new DeliverBehaviour(404, "application/xml", NO_SUCH_KEY_RESPONSE));
      result.serve("/*").withBehaviours(new S3ResponseHeadersBehaviour(),
          new DeliverBehaviour(404, "application/xml", NO_SUCH_KEY_RESPONSE_ROOT));
      return result;
    }
    else if (code == 500) {
      final Server result = Server.withPort(0);
      result.serve("/release/").withBehaviours(new S3ResponseHeadersBehaviour(),
          new DeliverBehaviour(404, "application/xml", NO_SUCH_KEY_RESPONSE));
      result.serve("/*").withBehaviours(new S3ResponseHeadersBehaviour(),
          new DeliverBehaviour(500, "application/xml", INTERNAL_ERROR_RESPONSE));
      return result;
    }
    else {
      throw new IllegalArgumentException("Code " + code + " not supported!");
    }
  }

  /**
   * Server that delivers "big" repository as multiple pages, and all transactions are fine.
   */
  protected Server prepareMultiPageServer()
      throws Exception
  {
    // maps with "subsequent pages" that client needs to ask using ?marker=XXX
    // where marker value is actually last key it got from previous page
    // Hence, key for 2nd page in last <Key> from 1st page response
    // key for 3rd page is last <Key> from 2nd page response
    final Map<String, DeliverBehaviour> pages = Maps.newHashMapWithExpectedSize(2);
    pages.put("release/foo1/baz/1/baz-2.jar", new DeliverBehaviour(200, "application/xml",
        MULTI_PAGE_RESPONSE_PAGE_2));
    pages.put("release/foo2/baz/1/baz-2.jar", new DeliverBehaviour(200, "application/xml",
        MULTI_PAGE_RESPONSE_PAGE_3));

    final Server result = Server.withPort(0);
    result.serve("/release/").withBehaviours(new S3ResponseHeadersBehaviour(),
        new DeliverBehaviour(404, "application/xml", NO_SUCH_KEY_RESPONSE));
    result.serve("/").withBehaviours(
        new S3ResponseHeadersBehaviour(),
        new S3PagedDeliverBehaviour(new DeliverBehaviour(200, "application/xml", MULTI_PAGE_RESPONSE_PAGE_1),
            pages));
    return result;
  }

  /**
   * Erroneous server that delivers "big" repository as multiple pages, and last page transactions fails.
   */
  protected Server prepareErrorMultiPageServer(final int code)
      throws Exception
  {
    // maps with "subsequent pages" that client needs to ask using ?marker=XXX
    // where marker value is actually last key it got from previous page
    // Hence, key for 2nd page in last <Key> from 1st page response
    // key for 3rd page is last <Key> from 2nd page response
    final Map<String, DeliverBehaviour> pages = Maps.newHashMapWithExpectedSize(2);
    pages.put("release/foo1/baz/1/baz-2.jar", new DeliverBehaviour(200, "application/xml",
        MULTI_PAGE_RESPONSE_PAGE_2));

    if (code == 403) {
      pages.put("release/foo2/baz/1/baz-2.jar", new DeliverBehaviour(403, "application/xml",
          ACCESS_DENIED_RESPONSE));
    }
    else if (code == 404) {
      pages.put("release/foo2/baz/1/baz-2.jar", new DeliverBehaviour(404, "application/xml",
          NO_SUCH_KEY_RESPONSE_ROOT));
    }
    else if (code == 500) {
      pages.put("release/foo2/baz/1/baz-2.jar", new DeliverBehaviour(500, "application/xml",
          INTERNAL_ERROR_RESPONSE));
    }
    else {
      throw new IllegalArgumentException("Code " + code + " not supported!");
    }

    final Server result = Server.withPort(0);
    result.serve("/release/").withBehaviours(new S3ResponseHeadersBehaviour(),
        new DeliverBehaviour(404, "application/xml", NO_SUCH_KEY_RESPONSE));
    result.serve("/").withBehaviours(
        new S3ResponseHeadersBehaviour(),
        new S3PagedDeliverBehaviour(new DeliverBehaviour(200, "application/xml", MULTI_PAGE_RESPONSE_PAGE_1),
            pages));
    return result;
  }

  // == One page:
  // These scenarios covers when repo URL actually points to a bucket root, and no URL fix happens.
  // Here we test scenarios when remote S3 is recognized, but various responses are received.

  @Test
  public void onePageHttp200()
      throws Exception
  {
    final Server server = prepareServer();
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(true));
      assertThat(context.getPrefixSource(), notNullValue());
      final List<String> entries = context.getPrefixSource().readEntries();
      assertThat(entries, notNullValue());
      assertThat(entries.size(), equalTo(1));
      assertThat(entries, contains("/release/foo"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void onePageHttp403()
      throws Exception
  {
    // server recognized as S3 but AccessDenied:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorServer(403);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  /**
   * Unsure is this a valid test case at all. Unsure when bucket root would respond with 404 at all? Empty bucket?
   */
  @Test
  public void onePageHttp404()
      throws Exception
  {
    // server recognized as S3 but 404:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorServer(404);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  /**
   * This test makes no sense, as HTTP 500 response triggers mechanism in
   * {@link Page#getPageFor(PageContext, String)}, so S3 scraper is not tested at all actually.
   */
  @Test(expected = UnexpectedPageResponse.class)
  public void onePageHttp500()
      throws Exception
  {
    // server recognized as S3 but 500:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorServer(500);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  // == Two pages:
  // Scenarios generally covers case when the repo root (that is nested in bucket, not at it's root)
  // responds as expected (NoSuchKey), and then tests simulates various responses for "fixed" URL
  // Point here, is that we did recognize remote as S3, and in any case, we should STOP scraping
  // instead throwing generic IOEx that would make other scrapers pick up and continue trying to scrape
  // the URL.

  @Test
  public void twoPagesHttp200()
      throws Exception
  {
    final Server server = prepareServer();
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(true));
      assertThat(context.getPrefixSource(), notNullValue());
      final List<String> entries = context.getPrefixSource().readEntries();
      assertThat(entries, notNullValue());
      assertThat(entries.size(), equalTo(2));
      assertThat(entries, contains("/foo/bar", "/foo/baz"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void twoPagesHttp403()
      throws Exception
  {
    // server recognized as S3 but AccessDenied:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorServer(403);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void twoPagesHttp404()
      throws Exception
  {
    // server recognized as S3 but 404:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorServer(404);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void twoPagesHttp500()
      throws Exception
  {
    // server recognized as S3 but 500:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorServer(500);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  // == Multi-pages (big repo)
  // Scenarios generally covers case when the repo root (that is nested in bucket, not at it's root)
  // responds as expected (NoSuchKey), and then tests simulates various responses for "fixed" URL
  // Point here, is that we did recognize remote as S3, and in any case, we should STOP scraping
  // instead throwing generic IOEx that would make other scrapers pick up and continue trying to scrape
  // the URL.

  @Test
  public void multiPagesHttp200()
      throws Exception
  {
    final Server server = prepareMultiPageServer();
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(true));
      assertThat(context.getPrefixSource(), notNullValue());
      final List<String> entries = context.getPrefixSource().readEntries();
      assertThat(entries, notNullValue());
      assertThat(entries.size(), equalTo(6));
      assertThat(entries,
          containsInAnyOrder("/foo1/bar", "/foo1/baz", "/foo2/bar", "/foo2/baz", "/foo3/bar", "/foo3/baz"));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void multiPagesHttp403()
      throws Exception
  {
    // server recognized as S3 but AccessDenied:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorMultiPageServer(403);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void multiPagesHttp404()
      throws Exception
  {
    // server recognized as S3 but 404:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorMultiPageServer(404);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void multiPagesHttp500()
      throws Exception
  {
    // server recognized as S3 but 500:
    // context should be stopped and unsuccessful
    final Server server = prepareErrorMultiPageServer(500);
    server.start();
    try {
      final HttpClient httpClient = new DefaultHttpClient();
      final String repoRoot = server.getUrl().toString() + "/release/";
      when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
      final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
      final Page page = Page.getPageFor(context, repoRoot);
      s3scraper.scrape(context, page);
      assertThat(context.isStopped(), is(true));
      assertThat(context.isSuccessful(), is(false));
    }
    finally {
      server.stop();
    }
  }
}
