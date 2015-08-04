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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.apachehttpclient.page.Page;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.Config;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteStrategy;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyFailedException;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyResult;
import org.sonatype.nexus.proxy.maven.routing.internal.scrape.ScrapeContext;
import org.sonatype.nexus.proxy.maven.routing.internal.scrape.Scraper;
import org.sonatype.nexus.proxy.storage.remote.httpclient.HttpClientManager;

import org.apache.http.client.HttpClient;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Remote scrape strategy.
 * 
 * @author cstamas
 */
@Named(RemoteScrapeStrategy.ID)
@Singleton
public class RemoteScrapeStrategy
    extends AbstractHttpRemoteStrategy
    implements RemoteStrategy
{
  protected static final String ID = "scrape";

  private final Config config;

  private final List<Scraper> scrapers;

  /**
   * Constructor.
   */
  @Inject
  public RemoteScrapeStrategy(final Config config, final HttpClientManager httpClientManager,
      final List<Scraper> scrapers)
  {
    // "last resort"
    super(Integer.MAX_VALUE, ID, httpClientManager);
    this.config = checkNotNull(config);
    this.scrapers = checkNotNull(scrapers);
  }

  @Override
  public StrategyResult doDiscover(final MavenProxyRepository mavenProxyRepository) throws StrategyFailedException,
      IOException
  {
    log.debug("Remote scrape on {} tried", mavenProxyRepository);
    // check does a proxy have a valid URL at all
    final String remoteRepositoryRootUrl;
    try {
      remoteRepositoryRootUrl = getRemoteUrlOf(mavenProxyRepository);
    }
    catch (MalformedURLException e) {
      throw new StrategyFailedException("Proxy repository " + mavenProxyRepository + " remote URL malformed:"
          + e.getMessage(), e);
    }

    // get client configured in same way as proxy is using it
    final HttpClient httpClient = createHttpClientFor(mavenProxyRepository);
    final ScrapeContext context = new ScrapeContext(mavenProxyRepository, httpClient, config.getRemoteScrapeDepth());
    final Page rootPage = Page.getPageFor(context, remoteRepositoryRootUrl);
    final ArrayList<Scraper> appliedScrapers = new ArrayList<Scraper>(scrapers);
    Collections.sort(appliedScrapers, new PriorityOrderingComparator<Scraper>());
    for (Scraper scraper : appliedScrapers) {
      log.debug("Remote scraping {} with Scraper {}", mavenProxyRepository, scraper.getId());
      scraper.scrape(context, rootPage);
      if (context.isStopped()) {
        if (context.isSuccessful()) {
          log.debug("Remote scraping {} with Scraper {} succeeded.", mavenProxyRepository, scraper.getId());
          return new StrategyResult(context.getMessage(), context.getPrefixSource(), true);
        }
        else {
          log.debug("Remote scraping {} with Scraper {} stopped execution.", mavenProxyRepository,
              scraper.getId());
          throw new StrategyFailedException(context.getMessage());
        }
      }
      log.debug("Remote scraping {} with Scraper {} skipped.", mavenProxyRepository, scraper.getId());
    }

    log.info("Not possible remote scrape of {}, no scraper succeeded.", mavenProxyRepository);
    throw new StrategyFailedException("No scraper was able to scrape remote (or remote prevents scraping).");
  }
}
