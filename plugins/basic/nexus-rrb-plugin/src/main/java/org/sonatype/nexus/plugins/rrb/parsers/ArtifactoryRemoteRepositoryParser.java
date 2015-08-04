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

import java.util.ArrayList;

import org.sonatype.nexus.plugins.rrb.RepositoryDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactoryRemoteRepositoryParser
    extends
    HtmlRemoteRepositoryParser
{

  /**
   * Links to sub-repos contain the pattern assigned to artifactoryLinkPattern
   * e.g. as in
   * <a class="icon-link folder" href="http://repo.jfrog.org/artifactory/java.net-cache/args4j/">args4j</a>
   * or
   * <a class="icon-link xml" href="http://repo.jfrog.org/artifactory/java.net-cache/archetype-catalog.xml">archetype-catalog.xml</a>
   */
  static String artifactoryLinkPattern = "class=\"icon-link";

  static String startOfArtifactoryLink = "<a " + artifactoryLinkPattern;

  static String validRefStart = "href=\"http";

  static String folderLink = artifactoryLinkPattern + " folder";

  static String folderLinkMatchPattern = ".*" + folderLink + ".*";

  //static String xmlLink = artifactoryLinkPattern + " xml";
  //static String pomLink = artifactoryLinkPattern + " pom";
  static String uriPrefixEnd = "/http";

  public ArtifactoryRemoteRepositoryParser(String remotePath, String localUrl,
                                           String id, String baseUrl)
  {
    super(remotePath, localUrl, id, baseUrl);
  }

  @Override
  public ArrayList<RepositoryDirectory> extractLinks(StringBuilder indata) {
    ArrayList<RepositoryDirectory> result = new ArrayList<RepositoryDirectory>();
    ArrayList<String> artifactoryLinks = extractArtifactoryLinks(indata);

    int uriPrefixEndPosition = localUrl.indexOf(uriPrefixEnd);
    String uriPrefix = "";
    if (uriPrefixEndPosition > 0) {
      uriPrefix = localUrl.substring(0, uriPrefixEndPosition) + "/";
    }
    for (String artifactoryLink : artifactoryLinks) {
      RepositoryDirectory repositoryDirectory = new RepositoryDirectory();
      String text = getLinkName(artifactoryLink).replace("/", "").trim();
      //If the link not contains the folderLink string it is a leaf
      repositoryDirectory.setLeaf(!artifactoryLink.matches(folderLinkMatchPattern));
      repositoryDirectory.setText(text);
      repositoryDirectory.setResourceURI(getLinkUrl(artifactoryLink));
      repositoryDirectory.setRelativePath(getRelativePath(artifactoryLink));
      result.add(repositoryDirectory);
    }
    return result;
  }

  /**
   * Go through the indata (i.e. the html content) and extract the links (i.e. <a.../a>) that are of Artifactory type
   *
   * @param indata-the html content
   * @return an ArrayList<String> where each element is a html anchor-link
   */
  public ArrayList<String> extractArtifactoryLinks(StringBuilder indata) {
    ArrayList<String> result = new ArrayList<String>();
    int currentStartPosition = -1;
    int endPosition = currentStartPosition;
    while ((currentStartPosition = getNextArtifactoryAnchorPosition(indata, endPosition)) > 0) {
      endPosition = indata.indexOf(linkEnd, currentStartPosition) + linkEnd.length();
      String string = indata.substring(currentStartPosition, endPosition);
      if (containsValidArtifactoryReference(string)) {
        result.add(string);
      }
    }
    return result;
  }

  /**
   * Check if string contains a reference defined by validRefStart
   * but exclude the reference if the anchor text contains ">..<"
   *
   * @param string - the anchor text
   */
  private boolean containsValidArtifactoryReference(String string) {
    return (string.indexOf(validRefStart) > 0) && (string.indexOf(">..<") < 0);
  }

  /**
   * Get the next anchor of Artifactory type starting in the last end position
   *
   * @param indata          - the indata text to search
   * @param lastEndPosition - the position of the end of last search
   */
  private int getNextArtifactoryAnchorPosition(StringBuilder indata,
                                               int lastEndPosition)
  {
    return indata.indexOf(startOfArtifactoryLink, lastEndPosition);
  }

  protected String getLinkName(String anchorString) {
    return getLinkName(new StringBuilder(anchorString));
  }

  private String getRelativePath(String anchorString) {
    String artifactoryUrl = getLinkUrl(new StringBuilder(anchorString));

    return artifactoryUrl.substring(this.baseUrl.length());
  }

  protected String getLinkUrl(String anchorString) {
    String relativePath = this.getRelativePath(anchorString);

    // strip starting /
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }

    String url;
    if (!this.localUrl.endsWith("/")) {
      url = this.localUrl + "/" + relativePath;
    }
    else {
      url = this.localUrl + relativePath;
    }

    return url;

  }

}
