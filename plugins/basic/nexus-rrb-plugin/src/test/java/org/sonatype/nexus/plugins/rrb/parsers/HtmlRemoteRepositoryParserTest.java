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
import static org.junit.Assert.assertTrue;

public class HtmlRemoteRepositoryParserTest
    extends RemoteRepositoryParserTestAbstract
{
  private HtmlRemoteRepositoryParser parser;

  ArrayList<RepositoryDirectory> result = new ArrayList<RepositoryDirectory>();

  @Before
  public void setUp()
      throws Exception
  {
    String remoteUrl = "http://www.xxx.com"; // The exact names of the urls
    String localUrl = "http://local"; // doesn't matter in the tests
    parser = new HtmlRemoteRepositoryParser("/", localUrl, "test", remoteUrl + "/");

    // htmlExample is an html repo with three sub directories
    StringBuilder indata = new StringBuilder(getExampleFileContent("/htmlExample"));
    result = parser.extractLinks(indata);
  }

  @Test
  public void testExtractLinks()
      throws Exception
  {
    assertEquals(7, result.size());
  }

  @Test
  public void testTextFirstPost()
      throws Exception
  {
    assertEquals("central", result.get(0).getText());
  }

  @Test
  public void testUriFirstPost()
      throws Exception
  {
    assertEquals("http://local/central/", result.get(0).getResourceURI());
  }

  @Test
  public void testRelativePathFirstPost()
      throws Exception
  {
    assertEquals("/central/", result.get(0).getRelativePath());
  }

  @Test
  public void testLeafFirstPost()
      throws Exception
  {
    assertFalse(result.get(0).isLeaf());
  }

  @Test
  public void testTextSecondPost()
      throws Exception
  {
    assertEquals("centralm1", result.get(1).getText());
  }

  @Test
  public void testUriSecondPost()
      throws Exception
  {
    assertEquals("http://local/centralm1/", result.get(1).getResourceURI());
  }

  @Test
  public void testRelativePathSecondPost()
      throws Exception
  {
    assertEquals("/centralm1/", result.get(1).getRelativePath());
  }

  @Test
  public void testLeafSecondPost()
      throws Exception
  {
    assertFalse(result.get(1).isLeaf());
  }

  @Test
  public void testTextThirdtPost()
      throws Exception
  {
    assertEquals("maven", result.get(2).getText());
  }

  @Test
  public void testUriThirdPost()
      throws Exception
  {
    assertEquals("http://local/maven/", result.get(2).getResourceURI());
  }

  @Test
  public void testRelativePathThirdPost()
      throws Exception
  {
    assertEquals("/maven/", result.get(2).getRelativePath());
  }

  @Test
  public void testLeafThirdPost()
      throws Exception
  {
    assertFalse(result.get(2).isLeaf());
  }

  @Test
  public void testTextFourthPost()
      throws Exception
  {
    assertEquals("file.jar", result.get(3).getText());
  }

  @Test
  public void testUriFourthPost()
      throws Exception
  {
    assertEquals("http://local/file.jar", result.get(3).getResourceURI());
  }

  @Test
  public void testRelativePathFourthPost()
      throws Exception
  {
    assertEquals("/file.jar", result.get(3).getRelativePath());
  }

  @Test
  public void testLeafFourthPost()
      throws Exception
  {
    assertTrue(result.get(3).isLeaf());
  }

  @Test
  public void testTextFivthPost()
      throws Exception
  {
    assertEquals("file.txt", result.get(4).getText());
  }

  @Test
  public void testUriFivthPost()
      throws Exception
  {
    assertEquals("http://local/file.txt", result.get(4).getResourceURI());
  }

  @Test
  public void testRelativePathFivthPost()
      throws Exception
  {
    assertEquals("/file.txt", result.get(4).getRelativePath());
  }

  @Test
  public void testLeafFivthPost()
      throws Exception
  {
    assertTrue(result.get(4).isLeaf());
  }

  @Test
  public void testTextSixthPost()
      throws Exception
  {
    assertEquals("file.txt", result.get(5).getText());
  }

  @Test
  public void testUriSixthPost()
      throws Exception
  {
    assertEquals("http://local/file.txt", result.get(5).getResourceURI());
  }

  @Test
  public void testRelativePathSixthPost()
      throws Exception
  {
    assertEquals("/file.txt", result.get(5).getRelativePath());
  }

  @Test
  public void testLeafSixthPost()
      throws Exception
  {
    assertTrue(result.get(5).isLeaf());
  }

  @Test
  public void testTextSeventhPost()
      throws Exception
  {
    assertEquals("maven", result.get(6).getText());
  }

  @Test
  public void testUriSeventhPost()
      throws Exception
  {
    assertEquals("http://local/maven/", result.get(6).getResourceURI());
  }

  @Test
  public void testRelativePathSeventhPost()
      throws Exception
  {
    assertEquals("/maven/", result.get(6).getRelativePath());
  }

  @Test
  public void testLeafSevenththPost()
      throws Exception
  {
    assertFalse(result.get(6).isLeaf());
  }

  @Test
  public void testCleanup1() {
    assertEquals("text/", parser.cleanup("text/"));
  }

  @Test
  public void testCleanup2() {
    assertEquals("text/", parser.cleanup("text/  "));
  }

  @Test
  public void testCleanup3() {
    assertEquals("text/", parser.cleanup("  text/"));
  }

  @Test
  public void testCleanup4() {
    assertEquals("text/", parser.cleanup("<img src=\"abc\" alt=\"abc\"/>text/"));
  }

  @Test
  public void testCleanup5() {
    assertEquals("text/", parser.cleanup("  <img src=\"abc\" alt=\"abc\"/>text/"));
  }

  @Test
  public void testCleanup6() {
    assertEquals("text/", parser.cleanup("text/<img src=\"abc\" alt=\"abc\"/>"));
  }

  @Test
  public void testCleanup7() {
    assertEquals("text/", parser.cleanup("text/<img src=\"abc\" alt=\"abc\"/>   "));
  }

  @Test
  public void testCleanup8() {
    assertEquals("text/", parser.cleanup("  text/<img src=\"abc\" alt=\"abc\"/>  "));
  }

  @Test
  public void testCleanup9() {
    assertEquals("text/", parser.cleanup("text/  <img src=\"abc\" alt=\"abc\"/>"));
  }

  @Test
  public void testGetLinkName1() {
    assertEquals("text/", parser.getLinkName(new StringBuilder("<a href=\"abc\">text/</a>")));
  }

  @Test
  public void testGetLinkName2() {
    assertEquals("text/", parser.getLinkName(new StringBuilder("<a href=\"abc\">text/  </a>")));
  }

  @Test
  public void testGetLinkName3() {
    assertEquals("text/", parser.getLinkName(new StringBuilder("<a href=\"abc\">  text/</a>")));
  }

  @Test
  public void testGetLinkName4() {
    assertEquals(
        "text/",
        parser.getLinkName(new StringBuilder("<a href=\"abc\"><img src=\"abc\" alt=\"abc\"/>text/</a>")));
  }

  @Test
  public void testGetLinkName5() {
    assertEquals(
        "text/",
        parser.getLinkName(new StringBuilder(
            "<a href=\"abc\">  <img src=\"abc\" alt=\"abc\"/>text/</a>")));
  }

  @Test
  public void testGetLinkName6() {
    assertEquals(
        "text/",
        parser.getLinkName(new StringBuilder("<a href=\"abc\">text/<img src=\"abc\" alt=\"abc\"/></a>")));
  }

  @Test
  public void testGetLinkName7() {
    assertEquals(
        "text/",
        parser.getLinkName(new StringBuilder(
            "<a href=\"abc\">text/<img src=\"abc\" alt=\"abc\"/>   </a>")));
  }

  @Test
  public void testGetLinkName8() {
    assertEquals(
        "text/",
        parser.getLinkName(new StringBuilder(
            "<a href=\"abc\">  text/<img src=\"abc\" alt=\"abc\"/>  </a>")));
  }

  @Test
  public void testGetLinkName9() {
    assertEquals(
        "text/",
        parser.getLinkName(new StringBuilder(
            "<a href=\"abc\">text/  <img src=\"abc\" alt=\"abc\"/></a>")));
  }

}
