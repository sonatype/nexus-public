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
package org.sonatype.nexus.repository.apt.internal.debian;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class ReleaseTest
{
  private Release parse(final String content) throws IOException {
    ControlFile cf = new ControlFileParser().parseControlFile(
        new ByteArrayInputStream(content.getBytes()));
    return new Release(cf);
  }

  // --- getManifestFiles ---

  @Test
  void getManifestFiles_sha256_returnsAllPaths() throws IOException {
    Release release = parse(
        "SHA256:\n" +
            " abc123 12345 main/binary-amd64/Packages.gz\n" +
            " def456 67890 main/i18n/Translation-en.gz\n" +
            " ghi789 11111 main/source/Sources.gz\n");

    List<String> paths = release.getManifestFiles();

    assertThat(paths, contains(
        "main/binary-amd64/Packages.gz",
        "main/i18n/Translation-en.gz",
        "main/source/Sources.gz"));
  }

  @Test
  void getManifestFiles_fallsBackToSha1WhenNoSha256() throws IOException {
    Release release = parse(
        "SHA1:\n" +
            " abc123 12345 main/binary-amd64/Packages.gz\n" +
            " def456 67890 main/i18n/Translation-en.gz\n");

    List<String> paths = release.getManifestFiles();

    assertThat(paths, contains(
        "main/binary-amd64/Packages.gz",
        "main/i18n/Translation-en.gz"));
  }

  @Test
  void getManifestFiles_fallsBackToMd5SumWhenNoSha256OrSha1() throws IOException {
    Release release = parse(
        "MD5Sum:\n" +
            " abc123 12345 main/binary-amd64/Packages.gz\n");

    assertThat(release.getManifestFiles(), contains("main/binary-amd64/Packages.gz"));
  }

  @Test
  void getManifestFiles_noHashSections_returnsEmptyList() throws IOException {
    Release release = parse("Origin: Ubuntu\nSuite: jammy\n");

    assertThat(release.getManifestFiles(), is(empty()));
  }

  @Test
  void getManifestFiles_malformedLines_skipsLinesWithFewerThanThreeParts() throws IOException {
    Release release = parse(
        "SHA256:\n" +
            " abc123 12345 main/binary-amd64/Packages.gz\n" +
            " onlytwoparts 99999\n" + // only 2 parts — skipped
            " singlepart\n" + // only 1 part — skipped
            " def456 67890 main/i18n/Translation-en.gz\n");

    assertThat(release.getManifestFiles(), contains(
        "main/binary-amd64/Packages.gz",
        "main/i18n/Translation-en.gz"));
  }

  @Test
  void getManifestFiles_sha256TakesPriorityOverSha1() throws IOException {
    // When both SHA256 and SHA1 are present, SHA256 paths are returned
    Release release = parse(
        "SHA256:\n" +
            " abc123 12345 main/binary-amd64/Packages.gz\n" +
            "SHA1:\n" +
            " def456 67890 main/i18n/Translation-en.gz\n");

    assertThat(release.getManifestFiles(), contains("main/binary-amd64/Packages.gz"));
  }

  @Test
  void getManifestFiles_emptyHashSection_returnsEmptyList() throws IOException {
    // SHA256 field present but empty value
    Release release = parse("SHA256:\n");

    assertThat(release.getManifestFiles(), is(empty()));
  }

  @Test
  void getManifestFiles_windowsLineEndings_parsesCorrectly() throws IOException {
    Release release = parse(
        "SHA256:\r\n" +
            " abc123 12345 main/binary-amd64/Packages.gz\r\n" +
            " def456 67890 main/i18n/Translation-en.gz\r\n");

    assertThat(release.getManifestFiles(), contains(
        "main/binary-amd64/Packages.gz",
        "main/i18n/Translation-en.gz"));
  }

  @Test
  void getManifestFiles_sha1TakesPriorityOverMd5Sum() throws IOException {
    Release release = parse(
        "SHA1:\n" +
            " abc123 12345 main/binary-amd64/Packages.gz\n" +
            "MD5Sum:\n" +
            " def456 67890 main/i18n/Translation-en.gz\n");

    assertThat(release.getManifestFiles(), contains("main/binary-amd64/Packages.gz"));
  }
}
