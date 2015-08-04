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
package org.sonatype.nexus.obr.metadata;

import java.io.IOException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.LocalStorageException;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.osgi.service.obr.Resource;

/**
 * Default {@link ObrResourceReader} that can handle OBR referrals.
 */
public class DefaultObrResourceReader
    implements ObrResourceReader
{
  private final boolean relative;

  private final Map<String, ObrParser> visited;

  private final List<URL> breadcrumbs;

  private ObrParser parser;

  /**
   * Creates a new {@link ObrResourceReader} for the given OBR site.
   *
   * @param site     the OBR site
   * @param relative use relative URIs?
   */
  public DefaultObrResourceReader(final ObrSite site, final boolean relative)
      throws IOException
  {
    this.relative = relative;

    visited = new HashMap<String, ObrParser>();
    breadcrumbs = new ArrayList<URL>();

    visit(site, Integer.MAX_VALUE);
  }

  /**
   * Visit an OBR site and continue parsing.
   *
   * @param site  the OBR site
   * @param depth the maximum depth
   */
  private void visit(final ObrSite site, final int depth)
      throws IOException
  {
    final URL nextMetadataUrl = site.getMetadataUrl();

    if (isNewSite(nextMetadataUrl)) {
      try {
        pushReferral(nextMetadataUrl, new DefaultObrParser(site, depth, relative));
      }
      catch (final XmlPullParserException e) {
        throw new LocalStorageException("Error parsing OBR header", e);
      }
    }
  }

  /**
   * True if we have not yet visited this URL, otherwise false.
   *
   * @param url the OBR URL
   * @return true if we have not yet visited this URL, otherwise false
   */
  private boolean isNewSite(final URL url) {
    return !visited.containsKey(url.toExternalForm());
  }

  /**
   * Retrieves the parser that's currently parsing the given OBR.
   *
   * @param url the OBR URL
   * @return the parser for the given OBR
   */
  private ObrParser getParser(final URL url) {
    return visited.get(url.toExternalForm());
  }

  /**
   * Records that we are now parsing the referred OBR.
   *
   * @param url        the OBR URL
   * @param nextParser the parser for the given OBR
   */
  private void pushReferral(final URL url, final ObrParser nextParser)
      throws XmlPullParserException, IOException
  {
    visited.put(url.toExternalForm(), nextParser);

    // move onto the resource section of the OBR
    nextParser.require(XmlPullParser.START_DOCUMENT, null, null);
    nextParser.nextTag();
    nextParser.require(XmlPullParser.START_TAG, null, "repository");

    // safe to proceed
    if (parser != null) {
      breadcrumbs.add(parser.getMetadataUrl());
    }

    parser = nextParser;
  }

  /**
   * Stop parsing the referred OBR and return to the referring OBR site.
   *
   * @return true if there are more sites to parse, otherwise false
   */
  private boolean popReferral() {
    if (breadcrumbs.isEmpty()) {
      return false;
    }

    parser = getParser(breadcrumbs.remove(breadcrumbs.size() - 1));

    return true;
  }

  public Resource readResource()
      throws IOException
  {
    try {
      while (true) {
        parser.nextTag();

        if ("referral".equals(parser.getName())) {
          parseReferral(); // points to another (external) OBR site
        }
        else if ("repository".equals(parser.getName())) {
          // should be end tag as we parsed the start tag in "visit"
          parser.require(XmlPullParser.END_TAG, null, "repository");
          if (false == popReferral()) {
            return null;
          }
        }
        else {
          break; // assume the only other top-level tag is resource
        }
      }

      return parser.parseResource();
    }
    catch (final XmlPullParserException e) {
      throw new LocalStorageException("Error parsing OBR resource", e);
    }
  }

  /**
   * Parses a referred OBR site, obeying the maximum allowed depth of referrals.
   */
  private void parseReferral()
      throws IOException
  {
    try {
      parser.require(XmlPullParser.START_TAG, null, "referral");

      final String url = parser.getAttributeValue(null, "url");
      final String depth = parser.getAttributeValue(null, "depth");

      parser.nextTag();
      parser.require(XmlPullParser.END_TAG, null, "referral");

      if (breadcrumbs.size() < parser.getMaxDepth()) {
        visit(new ReferencedObrSite(new URL(parser.getMetadataUrl(), url)), calculateMaxDepth(depth));
      }
    }
    catch (final XmlPullParserException e) {
      throw new LocalStorageException("Error parsing OBR referral", e);
    }
  }

  /**
   * Chooses the smallest limit: either the current global maximum depth or the new local maximum.
   *
   * @param depth the requested depth
   * @return the new maximum depth
   */
  private int calculateMaxDepth(final String depth) {
    try {
      return Math.min(parser.getMaxDepth(), breadcrumbs.size() + Integer.parseInt(depth));
    }
    catch (final Exception e) {
      return Integer.MAX_VALUE;
    }
  }

  public int read(final CharBuffer cb)
      throws IOException
  {
    // just here to complete the Reader API, it's not actually used

    try {
      parser.nextToken();
    }
    catch (final XmlPullParserException e) {
      throw new LocalStorageException("Error parsing XML token", e);
    }

    final int n = cb.length();
    cb.append(parser.getText());
    return cb.length() - n;
  }

  public void close() {
    for (final ObrParser p : visited.values()) {
      IOUtils.closeQuietly(p);
    }
  }
}
