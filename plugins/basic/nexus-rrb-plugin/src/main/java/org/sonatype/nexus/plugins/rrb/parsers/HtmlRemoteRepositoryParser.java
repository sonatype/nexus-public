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
package org.sonatype.nexus.plugins.rrb.parsers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.sonatype.nexus.plugins.rrb.RepositoryDirectory;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlRemoteRepositoryParser
    implements RemoteRepositoryParser
{

  private static final String[] EXCLUDES = {
      ">Skip to content<", ">Log in<", ">Products<", "Parent Directory", "?",
      ">../", ">..<", ">._.<", "-logo.png", ">Community<", ">Support<", ">Resources<", ">About us<", ">Downloads<",
      ">Documentation<", ">Resources<", ">About This Site<", ">Contact Us<", ">Legal Terms and Privacy Policy<",
      ">Log out<", ">IONA Technologies<", ">Site Index<", ">Skip to content<", ">Log In<"
  };

  private final Logger logger = LoggerFactory.getLogger(HtmlRemoteRepositoryParser.class);

  protected String localUrl;

  protected String remotePath;

  protected String linkStart = "<a ";

  protected String linkEnd = "/a>";

  protected String href = "href=\"";

  protected String id;

  protected String baseUrl;
  
  protected String basePath;

  public HtmlRemoteRepositoryParser(String remotePath, String localUrl, String id, String baseUrl) {
    this.remotePath = remotePath;
    this.localUrl = localUrl;
    this.id = id;
    this.baseUrl = baseUrl;
    try {
      this.basePath = new URI(baseUrl).getPath();
    } catch (URISyntaxException e) {
      // TODO: in this case was should not even be called?
      this.basePath = null;
    }
  }

  /**
   * Extracts the links and sets the data in the RepositoryDirectory object.
   *
   * @return a list of RepositoryDirectory objects
   */
  public ArrayList<RepositoryDirectory> extractLinks(StringBuilder indata) {
    ArrayList<RepositoryDirectory> result = new ArrayList<RepositoryDirectory>();

    if (indata.indexOf(linkStart.toUpperCase()) != -1) {
      linkStart = linkStart.toUpperCase();
      linkEnd = linkEnd.toUpperCase();
      href = href.toUpperCase();
    }
    int start = 0;
    int end = 0;

    if (!remotePath.endsWith("/")) {
      remotePath += "/";
    }
    if (remotePath.equals("/")) {
      remotePath = "";
    }
    if (!localUrl.endsWith("/")) {
      localUrl += "/";
    }

    do {
      RepositoryDirectory rp = new RepositoryDirectory();
      StringBuilder temp = new StringBuilder();
      start = indata.indexOf(linkStart, start);
      if (start < 0) {
        break;
      }

      end = indata.indexOf(linkEnd, start) + linkEnd.length();
      temp.append(indata.subSequence(start, end));
      if (!exclude(temp)) {
        if (!getLinkName(temp).trim().endsWith("/")) {
          rp.setLeaf(true);
        }
        rp.setText(getLinkName(temp).replace("/", "").trim());
        String linkUrl = getLinkUrl(temp);
        if (!StringUtils.isBlank(basePath) && linkUrl.startsWith(basePath)) {
          linkUrl = linkUrl.substring(basePath.length(), linkUrl.length());
          if (!StringUtils.isBlank(remotePath) && linkUrl.startsWith(remotePath)) {
            linkUrl = linkUrl.substring(remotePath.length(), linkUrl.length());
          }
        }
        String uri = linkUrl.replace(baseUrl, localUrl);
        try {
          new URL(uri);
          // if URL is valid it means that either the replacement of base url was successful either the url is
          // absolute but it is not starting with base URL (e.g. in case of remote uses a forced base URL that is
          // different that the one configured on repository (NEXUS-5811)
          uri = uri.startsWith(localUrl) ? uri : localUrl + uri;
        }
        catch (MalformedURLException e) {
          // the URL should be relative so let's prepend
          uri = localUrl + remotePath + uri;
        }
        rp.setResourceURI(uri);
        rp.setRelativePath(uri.replace(localUrl, ""));
        if (!rp.getRelativePath().startsWith("/")) {
          rp.setRelativePath("/" + rp.getRelativePath());
        }

        if (StringUtils.isNotEmpty(rp.getText())) {
          result.add(rp);
        }
        logger.debug("adding {} to result", rp.toString());
      }
      start = end + 1;
    }
    while (start > 0);

    return result;
  }

  /**
   * Extracts the link name.
   */
  protected String getLinkName(StringBuilder temp) {
    int start = temp.indexOf(">") + 1;
    int end = temp.indexOf("</");
    return cleanup(temp.substring(start, end));
  }

  protected String cleanup(String value) {
    int start = value.indexOf('<');
    int end = value.indexOf('>');
    if (start != -1 && start < end) {
      CharSequence seq = value.substring(start, end + 1);
      value = value.replace(seq, "");
      cleanup(value);
    }
    return value.trim();
  }

  /**
   * Extracts the link url.
   */
  protected String getLinkUrl(StringBuilder temp) {
    int start = temp.indexOf(href) + href.length();
    int end = temp.indexOf("\"", start + 1);
    return temp.substring(start, end);
  }

  /**
   * Excludes links that are not relevant for the listing.
   */
  boolean exclude(StringBuilder value) {
    for (String s : EXCLUDES) {
      if (value.indexOf(s) > 0) {
        logger.debug("{} is in EXCLUDES array", value);
        return true;
      }
    }
    return false;
  }

}
