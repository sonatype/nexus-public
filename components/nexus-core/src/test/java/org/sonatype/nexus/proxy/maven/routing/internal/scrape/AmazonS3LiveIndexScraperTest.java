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

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests that would go remotely for real. Usable for some manual testing or debugging, left here only as reference but
 * these should actually never be run by CI or such, hence the whole class is ignored.
 *
 * @author cstamas
 */
@Ignore("This test really goes remotely and would try to scrape Springsource external repository.")
public class AmazonS3LiveIndexScraperTest
    extends TestSupport
{
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
   * This test will go remote for true and scrape Springsource External bundles repository, that should take about
   * 20+
   * "paged" request (every page is 1000 entries limited by S3), and will take about a minute to execute.
   */
  @Test
  public void realS3Repository1()
      throws Exception
  {
    final HttpClient httpClient = new DefaultHttpClient();
    final String repoRoot = "http://repository.springsource.com/maven/bundles/external/";
    when(mavenProxyRepository.getRemoteUrl()).thenReturn(repoRoot);
    final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
    final Page page = Page.getPageFor(context, repoRoot);
    s3scraper.scrape(context, page);
    assertThat(context.isStopped(), is(true));
    assertThat(context.isSuccessful(), is(true));
    assertThat(context.getPrefixSource(), notNullValue());
    final List<String> entries = context.getPrefixSource().readEntries();
    assertThat(entries, notNullValue());
    assertThat(entries.size(), equalTo(152));
  }

  /**
   * This test will go remote for true and scrape Springsource roo repository.
   */
  @Test
  public void realS3Repository2()
      throws IOException
  {
    final HttpClient httpClient = new DefaultHttpClient();
    final String remoteUrl = "http://spring-roo-repository.springsource.org/release/";
    when(mavenProxyRepository.getRemoteUrl()).thenReturn(remoteUrl);
    final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, 2);
    final Page page = Page.getPageFor(context, remoteUrl);
    s3scraper.scrape(context, page);

    if (context.isSuccessful()) {
      System.out.println(context.getPrefixSource().readEntries());
    }
    else {
      if (context.isStopped()) {
        System.out.println(context.getMessage());
      }
      else {
        System.out.println("Huh?");
      }
    }
  }

}
