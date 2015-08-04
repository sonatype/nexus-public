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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ArtifactoryRemoteRepositoryParserTest
    extends
    RemoteRepositoryParserTestAbstract
{
  ArtifactoryRemoteRepositoryParser parser;

  StringBuilder indata;

  String localPrefix = "http://localhost:8081/nexus/service/local/repositories/ArtyJavaNet/remotebrowser/";

  @Before
  public void setUp()
      throws Exception
  {
    String remoteUrl = "http://repo.jfrog.org/artifactory/java.net-cache"; // The exact name doesn't matter
    //However the format of the localUrl is important for the outcome
    String localUrl = localPrefix;

    parser = new ArtifactoryRemoteRepositoryParser("/", localUrl, "test", remoteUrl);

    // Artifactory.java.net.htm is a file extracted from an Artifactory repo
    indata = new StringBuilder(getExampleFileContent("/Artifactory.java.net.htm"));

  }

  @Test
  public void testExtractArtifactoryLinks() {
    ArrayList<String> result = new ArrayList<String>();
    result = parser.extractArtifactoryLinks(indata);
    assertEquals(30, result.size());
  }

  @Test
  public void testExtractLinks()
      throws Exception
  {
    ArrayList<RepositoryDirectory> result = new ArrayList<RepositoryDirectory>();
    result = parser.extractLinks(indata);
    assertEquals(30, result.size());
    for (RepositoryDirectory repo : result) {
      //One repo is a leaf, "archetype-catalog.xml", the rest are not leafs
      assertEquals(repo.getText().equals("archetype-catalog.xml"), repo.isLeaf());
      assertEquals((localPrefix + repo.getText() + (repo.isLeaf() ? "" : "/")), repo.getResourceURI());
      assertFalse(repo.getResourceURI().contains("repo.jfrog.org"));


    }
  }

  @Test
  public void testExcludeDottedLinks() {
    //A test of excluding dotted links
    ArrayList<RepositoryDirectory> result = new ArrayList<RepositoryDirectory>();
    result = parser.extractLinks(new StringBuilder(dottedLinkExample()));
    assertEquals(1, result.size());
  }

  /**
   * An example with one real and one dotted link
   */
  private String dottedLinkExample() {
    return
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:wicket=\"http://wicket.sourceforge.net/\">" +
            "<head>" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
            "<link rel=\"icon\" type=\"image/x-icon\" href=\"../../../../favicon.ico\"/>" +
            "<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"../../../../favicon.ico\"/>" +
            "<title>Artifactory@repo.jfrog.org :: Repository Browser</title>" +
            "<meta content=\"10040\" name=\"revision\"/>" +
            "</head>" +
            "<body class=\"tundra\">" +
            "<table cellpadding=\"0\" cellspacing=\"0\" class=\"page\">" +
            "<tr>" +
            "<a title=\"Artifactory\" class=\" artifactory-logo\" href=\"../../../../webapp/home.html\">" +
            "<span class=\"none\">Artifactory</span>" +
            "<div class=\"local-repos-list\">" +
            "<div>" +
            "<a class=\"icon-link folder\" href=\"http://repo.jfrog.org/artifactory/java.net-cache/commons-httpclient/commons-httpclient/\">..</a>" +
            "<a class=\"icon-link jar\" href=\"http://repo.jfrog.org/artifactory/java.net-cache/commons-httpclient/commons-httpclient/3.1-rc1/commons-httpclient-3.1-rc1-sources.jar\">commons-httpclient-3.1-rc1-sources.jar</a>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>";
  }

}
