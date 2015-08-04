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

import javax.inject.Named;
import javax.inject.Singleton;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sonatype.nexus.apachehttpclient.page.Page;

/**
 * Scraper for remote Apache HTTPD hosted repositories.
 *
 * @author cstamas
 */
@Named(HttpdIndexScraper.ID)
@Singleton
public class HttpdIndexScraper
    extends AbstractGeneratedIndexPageScraper
{
  protected static final String ID = "httpd-index";

  /**
   * Default constructor.
   */
  public HttpdIndexScraper() {
    super(2000, ID); // 2nd by popularity
  }

  @Override
  protected String getTargetedServer() {
    return "Apache HTTPD Index Page";
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
      // Server response header is mandatory
      if (!page.hasHeaderAndStartsWith("Server", "Apache/")) {
        return new RemoteDetectionResult(RemoteDetectionOutcome.UNRECOGNIZED, getTargetedServer(),
            "Remote is not a generated index page of " + getTargetedServer());
      }
      // NEXUS-5589: Check address element only if present. If present, it MUST contain
      // required values, if not present, nothing.
      final Elements addressElements = page.getDocument().getElementsByTag("address");
      if (!addressElements.isEmpty() && !addressElements.get(0).text().startsWith("Apache")) {
        return new RemoteDetectionResult(RemoteDetectionOutcome.UNRECOGNIZED, getTargetedServer(),
            "Remote is not a generated index page of " + getTargetedServer());
      }
      // if we are here, we say YES for scraping
      return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_BE_SCRAPED, getTargetedServer(),
          "Should be scraped.");
    }
    else {
      return result;
    }
  }
}
