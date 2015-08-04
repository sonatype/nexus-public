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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.plugins.rrb.RepositoryDirectory;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class S3RemoteRepositoryParserTest
    extends RemoteRepositoryParserTestAbstract
{
  private S3RemoteRepositoryParser parser;

  String remoteUrl = "http://www.xxx.com"; // The exact names of the urls

  String localUrl = "http://local"; // doesn't matter in the tests

  String id = "test";

  @Before
  public void setUp() {
    parser = new S3RemoteRepositoryParser("/", localUrl, id, "milestone");
  }

  @Test
  public void testExtractLinks()
      throws Exception
  {
    // A S3 repo where the contents size is 15.
    // One of them is keyed as a robots.txt and should therefore in extractLinks method of the parser.
    StringBuilder indata = new StringBuilder(getExampleFileContent("/s3Example"));
    List<RepositoryDirectory> result = parser.extractLinks(indata);

    // Contents key:ed as robots.txt should not appear in the list of
    // RepositoryDirectory, which means that the result's size should be 13.
    assertEquals(13, result.size());
  }

  /**
   * A test that the right number of links with the right type (leaf) are extracted from ListBucketResult with one
   * item
   */
  @Test
  public void testRepoSize1()
      throws Exception
  {
    StringBuilder indata = new StringBuilder(s3OfSize1());
    List<RepositoryDirectory> result = parser.extractLinks(indata);
    assertEquals(1, result.size());
    RepositoryDirectory repositoryDirectory = result.get(0);
    assertTrue(repositoryDirectory.isLeaf());
  }

  /**
   * A test that checks if the parser extracts the uris and paths in the right way
   */
  @Test
  public void testExtractContent() {
    StringBuilder indata = new StringBuilder(contentString());
    parser.extractContent(indata, "milestone");
    List<RepositoryDirectory> result = parser.result;
    assertEquals(1, result.size());
    RepositoryDirectory repositoryDirectory = result.iterator().next();
    assertEquals("org.springframework.batch.archetype.simple.cli", repositoryDirectory.getText());
    assertEquals(localUrl + "/org/springframework/batch/org.springframework.batch.archetype.simple.cli/",
        repositoryDirectory.getResourceURI());
    assertEquals("/org/springframework/batch/org.springframework.batch.archetype.simple.cli/",
        repositoryDirectory.getRelativePath());
  }

  /**
   * Check that expected common prefixes are extracted into the repository directories
   */
  @Test
  public void testExtractCommonPrefix() {
    // no prefix
    parser = new S3RemoteRepositoryParser("/", localUrl, id, "");

    StringBuilder indata = new StringBuilder(repoWithCommonPrefixes());
    parser.extractCommonPrefix(indata, "");
    List<RepositoryDirectory> result = parser.result;
    assertEquals(5, result.size());
    String[] expectedResults = {"external", "milestone", "osgi", "release", "snapshot"};
    List<String> listOfExpectedResults = Arrays.asList(expectedResults);
    Iterator<String> expectedResultIterator = listOfExpectedResults.iterator();
    for (RepositoryDirectory repositoryDirectory : result) {
      assertFalse(repositoryDirectory.isLeaf());
      assertEquals(expectedResultIterator.next(), repositoryDirectory.getText());
    }

    assertFalse(result.get(0).getRelativePath().contains("prefix"));
    assertFalse(result.get(0).getResourceURI().contains("prefix"));
  }

  /**
   * A test links in the excluded list doesn't appear in the result
   */
  @Test
  public void testWithOneExcluded()
      throws Exception
  {
    StringBuilder indata = new StringBuilder(withOneInExcludedList());
    List<RepositoryDirectory> result = parser.extractLinks(indata);
    assertEquals(1, result.size());
  }

  private String contentString() {
    return "<Key>milestone/org/springframework/batch/org.springframework.batch.archetype.simple.cli/</Key>"
        + "<LastModified>2009-01-29T20:11:37.000Z</LastModified>"
        + "<ETag>&quot;d41d8cd98f00b204e9800998ecf8427e&quot;</ETag>" + "<Size>0</Size>"
        + "<StorageClass>STANDARD</StorageClass>";
  }

  private String s3OfSize1() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "	<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
        + "		<Name>maven.springframework.org</Name>"
        + "		<Prefix>milestone/org/springframework/batch/org.springframework.batch.archetype.simple.cli</Prefix>"
        + "		<Marker></Marker>" + "		<MaxKeys>1000</MaxKeys>" +
        "		<IsTruncated>false</IsTruncated>	"
        + "		<Contents>"
        + "			<Key>milestone/org/springframework/batch/org.springframework.batch.archetype.simple.cli/one</Key>"
        + "			<LastModified>2009-01-29T20:11:37.000Z</LastModified>"
        + "			<ETag>&quot;d41d8cd98f00b204e9800998ecf8427e&quot;</ETag>" + "			<Size>0</Size>"
        + "			<StorageClass>STANDARD</StorageClass>" + "		</Contents>" +
        "	</ListBucketResult>";
  }

  private String withOneInExcludedList() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "	<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2010-03-01/\">"
        + "		<Name>maven.springframework.org</Name>"
        + "		<Prefix>milestone/org/springframework/batch/org.springframework.batch.archetype.simple.cli</Prefix>"
        + "		<Marker></Marker>" + "		<MaxKeys>1000</MaxKeys>" +
        "		<IsTruncated>false</IsTruncated>	"
        + "		<Contents>"
        + "			<Key>milestone/org/springframework/batch/org.springframework.batch.archetype.simple.cli/one</Key>"
        + "			<LastModified>2009-01-29T20:11:37.000Z</LastModified>"
        + "			<ETag>&quot;d41d8cd98f00b204e9800998ecf8427e&quot;</ETag>" + "			<Size>0</Size>"
        + "			<StorageClass>STANDARD</StorageClass>" + "		</Contents>" +
        "		<Contents>"
        + "			<Key>external/robots.txt</Key>" +
        "			<LastModified>2009-01-29T20:11:37.000Z</LastModified>"
        + "			<ETag>&quot;d41d8cd98f00b204e9800998ecf8427e&quot;</ETag>" + "			<Size>0</Size>"
        + "			<StorageClass>STANDARD</StorageClass>" + "		</Contents>" +
        "	</ListBucketResult>";
  }

  private String repoWithCommonPrefixes() {
    // Fetched via http://s3.amazonaws.com/maven.springframework.org?delimiter=/
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
        + "	<Name>maven.springframework.org</Name>" + "	<Prefix></Prefix>" + "	<Marker></Marker>"
        + "	<MaxKeys>1000</MaxKeys>" + "	<Delimiter>/</Delimiter>" + "	<IsTruncated>false</IsTruncated>"
        + "	<Contents>" + "		<Key>.VolumeIcon.icns</Key>"
        + "		<LastModified>2010-01-26T21:06:40.000Z</LastModified>"
        + "		<ETag>&quot;837179d531bd8daa79625886bed898eb&quot;</ETag>"
        + "		<Size>58927</Size><StorageClass>STANDARD</StorageClass>" + "	</Contents>" +
        "	<Contents>"
        + "		<Key>._.</Key>" + "		<LastModified>2010-01-27T00:22:00.000Z</LastModified>"
        + "		<ETag>&quot;4381f675ace6679a83757b3860e66311&quot;</ETag>" + "		<Size>4096</Size>"
        + "		<StorageClass>STANDARD</StorageClass>" + "	</Contents>" + "	<Contents>"
        + "		<Key>._.VolumeIcon.icns</Key>" + "		<LastModified>2010-01-26T21:06:40.000Z</LastModified>"
        + "		<ETag>&quot;2091acbd4fe93901bf107e82c7b5e2ab&quot;</ETag>" + "		<Size>4096</Size>"
        + "		<StorageClass>STANDARD</StorageClass>" + "	</Contents>" + "	<Contents>" +
        "		<Key>0.dir</Key>"
        + "		<LastModified>2010-01-27T00:22:02.000Z</LastModified>"
        + "		<ETag>&quot;5f3c44666a9ca2461f27b87b4603b0d0&quot;</ETag>" + "		<Size>16</Size>"
        + "		<StorageClass>STANDARD</StorageClass>" + "	</Contents>" + "	<Contents>" +
        "		<Key>robots.txt</Key>"
        + "		<LastModified>2009-07-09T17:35:59.000Z</LastModified>"
        + "		<ETag>&quot;9152d7f1724ed8fbcd2e0c87029f193c&quot;</ETag>" + "		<Size>25</Size>"
        + "		<StorageClass>STANDARD</StorageClass>" + "	</Contents>" + "	<CommonPrefixes>"
        + "		<Prefix>external/</Prefix>" + "	</CommonPrefixes>" + "	<CommonPrefixes>"
        + "		<Prefix>milestone/</Prefix>" + "	</CommonPrefixes>" + "	<CommonPrefixes>" +
        "		<Prefix>osgi/</Prefix>"
        + "	</CommonPrefixes>" + "	<CommonPrefixes>" + "		<Prefix>release/</Prefix>" +
        "	</CommonPrefixes>"
        + "	<CommonPrefixes>" + "		<Prefix>snapshot/</Prefix>" + "	</CommonPrefixes>" +
        "</ListBucketResult>";
  }
}
