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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;

import com.google.common.io.CharStreams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for working with PyPI indexes.
 *
 * @since 3.1
 */
public final class PyPiIndexUtils
{
  private static final Logger log = LoggerFactory.getLogger(PyPiIndexUtils.class);

  /**
   * Returns a map (in original order of appearance) of the files and associated paths extracted from the index.
   */
  static Map<String, String> extractLinksFromIndex(final InputStream in) throws IOException {
    checkNotNull(in);
    Map<String, String> results = new LinkedHashMap<>();
    try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String html = CharStreams.toString(reader);
      Document document = Jsoup.parse(html);
      Elements links = document.select("a[href]");
      for (Element link : links) {
        String file = link.text().trim();
        String path = link.attr("href");
        if (!results.containsKey(file)) {
          results.put(file, path);
        }
      }
    }
    return results;
  }

  /**
   * Returns a string containing the HTML simple index page for the links, rendered in iteration order.
   */
  static String buildIndexPage(final TemplateHelper helper, final String name, final Map<String, String> links) {
    checkNotNull(helper);
    checkNotNull(name);
    checkNotNull(links);
    URL template = PyPiIndexUtils.class.getResource("pypi-index.vm");
    TemplateParameters params = helper.parameters();
    params.set("name", name);
    params.set("assets", links.entrySet().stream().map((link) -> {
      Map<String, String> data = new HashMap<>();
      data.put("link", link.getValue());
      data.put("file", link.getKey());
      return data;
    }).collect(Collectors.toList()));
    return helper.render(template, params);
  }

  /**
   * Rewrites all links so that the packages element is relative to the simple index, where possible. This is an
   * assumption based on email discussions with donald@stufft.io that there are no plans to have packages at any
   * location other than under the /packages directory.
   */
  static Map<String, String> makeLinksRelative(final Map<String, String> oldLinks) {
    checkNotNull(oldLinks);
    Map<String, String> newLinks = new LinkedHashMap<>();
    for (Entry<String, String> oldLink : oldLinks.entrySet()) {
      String newLink = maybeRewriteLink(oldLink.getValue());
      if (newLink != null) {
        newLinks.put(oldLink.getKey(), newLink);
      }
    }
    return newLinks;
  }

  /**
   * Rewrites an index page so that the links are all relative where possible.
   */
  static Map<String, String> makeIndexRelative(final InputStream in) throws IOException {
    return PyPiIndexUtils.makeLinksRelative(PyPiIndexUtils.extractLinksFromIndex(in));
  }

  /**
   * Rewrites a link so that the link is relative to the simple index, if possible. Returns null if the link is
   * skipped.
   */
  @Nullable
  private static String maybeRewriteLink(final String link) {
    if (link.startsWith("../../packages")) {
      log.trace("Found index page relative link, not rewriting: {}", link);
      return link;
    }

    // This allows the PyPI warehouse implementation to work as an undocumented side-effect, a request to the resulting
    // URL will produce a 301 that ends up being followed to the actual file location on their file hosting site
    int startIndex = link.indexOf("/packages");
    if (startIndex != -1) {
      log.trace("Rewriting link as relative (is this an absolute url for warehouse?): " + link);
      return "../.." + link.substring(startIndex);
    }

    // Since we're being more permissive/trying to do some ad-hoc cleanup on links as above, when we encounter other
    // weirdness, just log it and continue
    log.warn("Found unexpected PyPI link format, skipping: " + link);
    return null;
  }

  private PyPiIndexUtils() {
    // empty
  }
}
