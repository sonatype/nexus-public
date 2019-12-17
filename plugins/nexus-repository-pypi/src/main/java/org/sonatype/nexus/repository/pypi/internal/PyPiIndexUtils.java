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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.valueOf;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static org.apache.http.client.utils.URLEncodedUtils.parse;

/**
 * Utility methods for working with PyPI indexes.
 *
 * @since 3.1
 */
public final class PyPiIndexUtils
{

  private static final Logger log = LoggerFactory.getLogger(PyPiIndexUtils.class);

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
  static List<PyPiLink> extractLinksFromIndex(final InputStream in) throws IOException {
    checkNotNull(in);
    List<PyPiLink> results = new ArrayList<>();
    try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String html = CharStreams.toString(reader);
      Document document = Jsoup.parse(html);
      Elements links = document.select("a[href]");
      for (Element link : links) {
        String file = link.text().trim();
        String path = link.attr("href");
        String requiresPython = link.attr(PYPI_REQUIRES_PYTHON);
        results.add(new PyPiLink(file, path, requiresPython));
      }
    }
    return results;
  }

  /**
   * Returns a string containing the HTML simple index page for the links, rendered in iteration order.
   */
  static String buildIndexPage(final TemplateHelper helper, final String name, final Collection<PyPiLink> links) {
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
  static String buildRootIndexPage(final TemplateHelper helper, final Collection<PyPiLink> links) {
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
  private static List<PyPiLink> makeLinksRelative(final List<PyPiLink> oldLinks,
                                                       final Function<String, String> linkTranslator) {
    checkNotNull(oldLinks);
    List<PyPiLink> newLinks = new ArrayList<>();
    for (PyPiLink oldLink : oldLinks) {
      String newLink = linkTranslator.apply(oldLink.getLink());
      if (newLink != null) {
        newLinks.add(new PyPiLink(oldLink.getFile(), newLink, oldLink.getDataRequiresPython()));
      }
    }
    return newLinks;
  }

  /**
   * Rewrites an index page so that the links are all relative where possible.
   */
  static List<PyPiLink> makeIndexRelative(final URI remoteUrl, final String name, final InputStream in)
      throws IOException
  {
    return makePackageLinksRelative(remoteUrl, name, extractLinksFromIndex(in));
  }

  /**
   * Rewrites an index page so that the links are all relative where possible.
   */
  static List<PyPiLink> makePackageLinksRelative(final URI remoteUrl,
                                                      final String name,
                                                      final List<PyPiLink> oldLinks)
  {
    return makeLinksRelative(oldLinks, link -> rewriteLink(remoteUrl, name, link));
  }

  /**
   * Rewrites a root index page from a stream so that the links are all relative where possible.
   */
  static List<PyPiLink> makeRootIndexRelative(final InputStream in) throws IOException {
    return makeRootIndexLinksRelative(extractLinksFromIndex(in));
  }

  /**
   * Rewrites a root index page so that the links are all relative where possible.
   */
  static List<PyPiLink> makeRootIndexLinksRelative(final List<PyPiLink> oldLinks) {
    return makeLinksRelative(oldLinks, PyPiIndexUtils::maybeRewriteRootLink);
  }

  /**
   * Rewrites a link so that the link is relative to the simple index, if possible. Returns null if the link is
   * skipped.
   */
  @Nullable
  private static String rewriteLink(final URI remoteUrl, final String name, final String link) {
    if (link.matches(ABSOLUTE_URL_PREFIX)) {
      return rewriteAbsoluteUri(link);
    }
    return rewriteRelativeUri(remoteUrl, name, link);
  }

  /**
   * Embeds the scheme, host and port in the URL path so that it can be parsed out and proxied, even when the host
   * does not match the remote
   */
  private static String rewriteAbsoluteUri(final String link) {
    try {
      URI linkUri = new URI(link);
      URI relativeUri = hostPathUri(linkUri).resolve(linkUri.getPath().substring(1));
      return RELATIVE_PREFIX + new URIBuilder(relativeUri)
          .setFragment(linkUri.getFragment())
          .setCustomQuery(linkUri.getQuery())
          .build()
          .toASCIIString();
    }
    catch (URISyntaxException e) {
      log.error("Error building relative path for absolute url {}. Package link is being skipped.", link, e);
      return null;
    }
  }

  /**
   * Creates a URI in the format of {host}/{port}/{scheme}/ where {port} is only used when explicitly declared
   */
  private static URI hostPathUri(final URI linkUri) throws URISyntaxException {
    URI relativeUri = new URIBuilder(toPath(linkUri.getHost())).build();
    if (linkUri.getPort() != -1) {
      relativeUri = relativeUri.resolve(toPath(valueOf(linkUri.getPort())));
    }
    relativeUri = relativeUri.resolve(toPath(linkUri.getScheme()));
    return relativeUri;
  }

  private static String toPath(final String host) {
    return host + "/";
  }

  /**
   * Rewrites a link relative to the remoteUrl
   */
  private static String rewriteRelativeUri(final URI remoteUrl, final String name, final String link) {
    URI linkUri = remoteUrl.resolve("/").resolve(name).resolve(link);
    try {
      String relativePath = linkUri.getPath().substring(1);
      if (PATH_WITH_HOST_PREFIX.matcher(linkUri.getPath()).matches()) {
        relativePath = hostPathUri(linkUri).resolve(relativePath).getPath();
      }
      return RELATIVE_PREFIX + new URIBuilder(relativePath)
          .addParameters(parse(linkUri, Charset.defaultCharset()))
          .setFragment(linkUri.getFragment()).build()
          .toASCIIString();
    }
    catch (URISyntaxException e) {
      log.error("Error building relative path for relative path {}. Package link is being skipped.", link, e);
      return null;
    }
  }

  private static String maybeRewriteRootLink(final String link) {
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
