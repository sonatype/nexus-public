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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

/**
 * Utility methods for working with PyPI indexes.
 *
 * @since 3.1
 */
public final class PyPiIndexUtils
{
  /**
   * From PEP_053:
   * A repository MAY include a data-requires-python attribute on a file link.
   * This exposes the Requires-Python metadata field, specified in PEP 345, for the corresponding release.
   * Where this is present, installer tools SHOULD ignore the download when installing
   * to a Python version that doesn't satisfy the requirement.
   */
  private static final String PYPI_REQUIRES_PYTHON = "data-requires-python";

  public static final String ABSOLUTE_URL_PREFIX = "^https?://.*";

  public static final Pattern PATH_WITH_HOST_PREFIX = compile(
      "^/(?<host>[a-zA-Z0-9.-]+?)(?:/(?<port>\\d+))?/(?<scheme>https??)(?<path>/.*)");

  public static final String RELATIVE_PREFIX = "../../";

  /**
   * Returns a map (in original order of appearance) of the files and associated paths extracted from the index.
   */
  public static List<PyPiLink> extractLinksFromIndex(final InputStream in) throws IOException {
    checkNotNull(in);
    try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return extractLinksFromIndex(CharStreams.toString(reader));
    }
  }

  /**
   * Returns a map (in original order of appearance) of the files and associated paths extracted from the index.
   */
  public static List<PyPiLink> extractLinksFromIndex(final String html) {
    checkNotNull(html);
    List<PyPiLink> results = new ArrayList<>();
    Document document = Jsoup.parse(html);
    Elements links = document.select("a[href]");
    for (Element link : links) {
      String file = link.text().trim();
      String path = link.attr("href");
      String requiresPython = link.attr(PYPI_REQUIRES_PYTHON);
      results.add(new PyPiLink(file, path, requiresPython));
    }
    return results;
  }

  /**
   * Returns a string containing the HTML simple index page for the links, rendered in iteration order.
   */
  public static String buildIndexPage(final TemplateHelper helper, final String name, final Collection<PyPiLink> links) {
    checkNotNull(helper);
    checkNotNull(name);
    checkNotNull(links);
    URL template = PyPiIndexUtils.class.getResource("pypi-index.vm");
    TemplateParameters params = helper.parameters();
    params.set("name", name);
    params.set("assets", links.stream().map(PyPiIndexUtils::indexLinkToMap).collect(toList()));
    return helper.render(template, params);
  }

  /**
   * Returns a string containing the HTML simple root index page for the links, rendered in iteration order.
   */
  public static String buildRootIndexPage(final TemplateHelper helper, final Collection<PyPiLink> links) {
    checkNotNull(helper);
    checkNotNull(links);
    URL template = PyPiIndexUtils.class.getResource("pypi-root-index.vm");
    TemplateParameters params = helper.parameters();
    params.set("assets", links.stream().map(PyPiIndexUtils::rootIndexLinkToMap).collect(toList()));
    return helper.render(template, params);
  }

  /**
   * Convert a link to a map ready to be used for "pypi-index.vm".
   */
  static ImmutableMap<String, String> indexLinkToMap(final PyPiLink link) {
    return ImmutableMap.of(
        "link", link.getLink(),
        "file", link.getFile(),
        "data-requires-python", link.getDataRequiresPython()
    );
  }

  /**
   * Convert a link to a map ready to be used for "pypi-root-index.vm".
   */
  static ImmutableMap<String, String> rootIndexLinkToMap(final PyPiLink link) {
    return ImmutableMap.of(
        "link", link.getLink(),
        "name", link.getFile()
    );
  }

  /**
   * Rewrites all links so that the packages element is relative to the simple index, where possible. This is an
   * assumption based on email discussions with donald@stufft.io that there are no plans to have packages at any
   * location other than under the /packages directory.
   */
  private static List<PyPiLink> makeLinksRelative(
      final List<PyPiLink> oldLinks,
      final Function<PyPiLink, String> linkTranslator)
  {
    checkNotNull(oldLinks);
    List<PyPiLink> newLinks = new ArrayList<>();
    for (PyPiLink oldLink : oldLinks) {
      String newLink = linkTranslator.apply(oldLink);
      if (newLink != null) {
        newLinks.add(new PyPiLink(oldLink.getFile(), newLink, oldLink.getDataRequiresPython()));
      }
    }
    return newLinks;
  }

  public static boolean validateIndexLinks(final String packageName, final List<PyPiLink> links) {
    // PEP 503 - Normalized names
    Function<String, String> normalize = name -> name.replaceAll("[-_.]+", "-").toLowerCase(Locale.ENGLISH);

    String normalizedPackageName = normalize.apply(packageName);

    return links.stream().map(PyPiLink::getFile).filter(Objects::nonNull).map(normalize)
        .anyMatch(file -> file.startsWith(normalizedPackageName));
  }

  /**
   * Rewrites an index page so that the links reference NXRM paths.
   */
  public static List<PyPiLink> makeIndexLinksNexusPaths(final String name, final InputStream in)
      throws IOException
  {
    return makePackageLinksNexusPaths(name, extractLinksFromIndex(in));
  }

  /**
   * Rewrites an index page so that the links match NXRM proxy package paths.
   */
  static List<PyPiLink> makePackageLinksNexusPaths(
      final String name,
      final List<PyPiLink> oldLinks)
  {
    return makeLinksRelative(oldLinks, link -> {
      String filename = link.getFile();
      String version = PyPiFileUtils.extractVersionFromFilename(filename);

      String result = RELATIVE_PREFIX + PyPiPathUtils.packagesPath(name, version, filename);

      int hashIndex = link.getLink().indexOf('#');
      if (hashIndex != -1) {
        result += link.getLink().substring(hashIndex);
      }

      return result;
    });
  }

  /**
   * Rewrites a root index page from a stream so that the links are all relative where possible.
   */
  public static List<PyPiLink> makeRootIndexRelative(final InputStream in) throws IOException {
    return makeRootIndexLinksRelative(extractLinksFromIndex(in));
  }

  /**
   * Rewrites a root index page so that the links are all relative where possible.
   */
  static List<PyPiLink> makeRootIndexLinksRelative(final List<PyPiLink> oldLinks) {
    return makeLinksRelative(oldLinks, PyPiIndexUtils::maybeRewriteRootLink);
  }

  private static String maybeRewriteRootLink(final PyPiLink pypiLink) {
    String link = pypiLink.getLink();
    if (link.contains("/simple/")) {
      String[] split = link.split("/simple/");
      return split[1];
    }

    return link;
  }

  private PyPiIndexUtils() {
    // empty
  }
}
