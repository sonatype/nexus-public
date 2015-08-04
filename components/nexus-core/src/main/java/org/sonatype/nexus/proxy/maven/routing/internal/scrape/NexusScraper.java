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

import javax.inject.Named;
import javax.inject.Singleton;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sonatype.nexus.apachehttpclient.page.Page;

/**
 * Scraper for remote Nexus instances that will scrape only if remote is for sure recognized as Nexus instance, and URL
 * points to a hosted repository.
 *
 * @author cstamas
 */
@Named(NexusScraper.ID)
@Singleton
public class NexusScraper
    extends AbstractGeneratedIndexPageScraper
{
  protected static final String ID = "nexus";

  /**
   * Default constructor.
   */
  public NexusScraper() {
    super(1000, ID); // 1st by popularity
  }

  @Override
  protected String getTargetedServer() {
    return "Sonatype Nexus";
  }

  @Override
  protected Element getParentDirectoryElement(final Page page) {
    final Document doc = Jsoup.parseBodyFragment("<a href=\"../\">Parent Directory</a>", page.getUrl());
    return doc.getElementsByTag("a").first();
  }

  @Override
  protected RemoteDetectionResult detectRemoteRepository(final ScrapeContext context, final Page page) {
    final RemoteDetectionResult result = super.detectRemoteRepository(context, page);
    if (RemoteDetectionOutcome.RECOGNIZED_SHOULD_BE_SCRAPED == result.getRemoteDetectionOutcome()) {
      try {
        // so index page looks like Nexus index page, let's see about repo metadata
        // this is not cheap, as we are doing extra HTTP requests to get it
        final Page repoMetadataPage =
            Page.getPageFor(context, context.getRemoteRepositoryRootUrl() + ".meta/repository-metadata.xml");
        if (page.getHttpResponse().getStatusLine().getStatusCode() == 200) {
          // sanity: all nexus repo MD has these elements (see below)
          final Elements url = repoMetadataPage.getDocument().getElementsByTag("url");
          final Elements layout = repoMetadataPage.getDocument().getElementsByTag("layout");
          // only proxies has this element
          final Elements localUrl = repoMetadataPage.getDocument().getElementsByTag("localUrl");
          // only groups has this element
          final Elements memberRepositories =
              repoMetadataPage.getDocument().getElementsByTag("memberRepositories");

          // sanity checks:
          // all of them must have "url" tag
          // all of the must have "layout" tag with value "maven2"
          if (!url.isEmpty() && !layout.isEmpty() && "maven2".equals(layout.get(0).text())) {
            if (localUrl.isEmpty() && memberRepositories.isEmpty()) {
              // we are sure it is a nexus hosted repo
              return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_BE_SCRAPED,
                  getTargetedServer(), "Remote is a hosted repository.");
            }
            else if (!localUrl.isEmpty() && memberRepositories.isEmpty()) {
              // is a proxy, do not scrape
              return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_NOT_BE_SCRAPED,
                  getTargetedServer(), "Remote is a proxy repository.");
            }
            else if (localUrl.isEmpty() && !memberRepositories.isEmpty()) {
              // is a group, do not scrape
              return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_NOT_BE_SCRAPED,
                  getTargetedServer(), "Remote is a group repository.");
            }
            else {
              // this is a failsafe branch, as should never happen (unless repo md is broken?, only one of
              // those two might exists)
              return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_NOT_BE_SCRAPED,
                  getTargetedServer(), "Remote is not a hosted repository.");
            }
          }
          else if (!url.isEmpty() && !layout.isEmpty() && !"maven2".equals(layout.get(0).text())) {
            // is not a maven2 repository/layout, do not scrape
            return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_NOT_BE_SCRAPED,
                getTargetedServer(), "Remote is not a Maven2 repository.");
          }
        }
      }
      catch (IOException e) {
        // hm, either not exists or whoknows, just ignore this as Nexus must have it and should return it
        log.debug("Problem during fetch of /.meta/repository-metadata.xml from remote of {}", context.getProxyRepository(), e);
      }
    }
    // um, we were not totally positive, this might be some web server with index page similar to Nexus one
    return new RemoteDetectionResult(RemoteDetectionOutcome.UNRECOGNIZED, getTargetedServer(),
        "Remote is not a generated index page of " + getTargetedServer());
  }
}
