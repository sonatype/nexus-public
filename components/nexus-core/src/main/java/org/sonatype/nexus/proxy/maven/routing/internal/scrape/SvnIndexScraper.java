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
 * Scraper for remote SVN hosted repository.
 *
 * @author cstamas
 */
@Named(SvnIndexScraper.ID)
@Singleton
public class SvnIndexScraper
    extends AbstractGeneratedIndexPageScraper
{
  protected static final String ID = "svn-index";

  /**
   * Default constructor.
   */
  public SvnIndexScraper() {
    super(3000, ID); // 3rd by popularity
  }

  @Override
  protected String getTargetedServer() {
    return "Subversion Index Page";
  }

  @Override
  protected Element getParentDirectoryElement(final Page page) {
    final Document doc = Jsoup.parseBodyFragment("<a href=\"../\">..</a>", page.getUrl());
    return doc.getElementsByTag("a").first();
  }

  @Override
  protected RemoteDetectionResult detectRemoteRepository(final ScrapeContext context, final Page page) {
    final RemoteDetectionResult result = super.detectRemoteRepository(context, page);
    if (RemoteDetectionOutcome.RECOGNIZED_SHOULD_BE_SCRAPED == result.getRemoteDetectionOutcome()) {
      // ensure there is an "a" tag with href pointing to either "http://subversion.tigris.org/" or
      // "http://subversion.apache.org/"
      final Elements elements = page.getDocument().getElementsByTag("a");
      for (Element element : elements) {
        final String elementHref = element.absUrl("href");
        if ("http://subversion.tigris.org/".equals(elementHref)
            || "http://subversion.apache.org/".equals(elementHref)) {
          return new RemoteDetectionResult(RemoteDetectionOutcome.RECOGNIZED_SHOULD_BE_SCRAPED,
              getTargetedServer(), "Should be scraped.");
        }
      }
    }
    return new RemoteDetectionResult(RemoteDetectionOutcome.UNRECOGNIZED, getTargetedServer(),
        "Remote is not a generated index page of " + getTargetedServer());
  }
}
