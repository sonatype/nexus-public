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
package org.sonatype.nexus.mime.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.mime.MimeRule;
import org.sonatype.nexus.mime.MimeRulesSource;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultMimeSupport}.
 */
public class DefaultMimeSupportTest
    extends TestSupport
{
  private DefaultMimeSupport underTest = new DefaultMimeSupport();

  @Mock
  private NexusMimeTypes mimeTypes;

  @Mock
  private MimeRule mimeType;

  /**
   * Tests the simple "guessing" against some known paths.
   */
  @Test
  public void testGuessMimeTypeFromPath() {
    assertThat(underTest.guessMimeTypeFromPath("/some/path/artifact.pom"), equalTo("application/xml"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/artifact.jar"), equalTo("application/java-archive"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/artifact-sources.jar"), equalTo("application/java-archive"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/maven-metadata.xml"), equalTo("application/xml"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.xml"), equalTo("application/xml"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.tar.gz"), equalTo("application/x-gzip"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.tar.bz2"), equalTo("application/x-bzip2"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.zip"), equalTo("application/zip"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.war"), equalTo("application/java-archive"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.rar"), equalTo("application/java-archive"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.ear"), equalTo("application/java-archive"));
    assertThat(underTest.guessMimeTypeFromPath("/some/path/some.ejb"), equalTo("application/java-archive"));
  }

  /**
   * Tests that repo with diverting MimeRulesSupport actually works. If both tests, this one and
   * {@link #testGuessMimeTypeFromPath()} passes, their conjunction proves it works.
   */
  @Test
  public void testGuessfakeMimeRulesSourceMimeTypeFromPath() {
    MimeRulesSource source = new MimeRulesSource()
    {
      @Override
      public MimeRule getRuleForName(String path) {
        return new MimeRule(false, "foo/bar");
      }
    };
    assertThat(underTest.guessMimeTypeFromPath("/some/path/artifact.pom", source), equalTo("foo/bar"));
  }

  @Test
  public void testGuessWithoutMimeRulesSourceMimeTypeFromPath() {
    assertThat(underTest.guessMimeTypeFromPath("/some/path/artifact.pom"), equalTo("application/xml"));
  }

  @Test
  public void useNexusMimeTypes() {
    this.underTest = new DefaultMimeSupport(mimeTypes);
    when(mimeTypes.getMimeRuleForExtension("test")).thenReturn(mimeType);
    when(mimeType.getMimetypes()).thenReturn(Lists.newArrayList("fake/mimetype"));

    assertThat(underTest.guessMimeTypeFromPath("foo.test"), is("fake/mimetype"));
  }

  @Test
  public void retainDefaultMimeTypes() {
    this.underTest = new DefaultMimeSupport(mimeTypes);

    assertThat(underTest.guessMimeTypeFromPath("foo.doc"), is("application/msword"));
  }

  @Test
  public void preferDefaultMimeType() {
    this.underTest = new DefaultMimeSupport(mimeTypes);

    when(mimeTypes.getMimeRuleForExtension("zip")).thenReturn(mimeType);
    when(mimeType.getMimetypes()).thenReturn(Lists.newArrayList("fake/mimetype"));

    final List<String> mimeTypes = underTest.guessMimeTypesListFromPath("foo.zip");
    assertThat(mimeTypes, contains("fake/mimetype", "application/zip", "application/x-zip-compressed"));

    assertThat(underTest.guessMimeTypeFromPath("foo.zip"), is("fake/mimetype"));
  }

  @Test
  public void overrideDefaultMimeType() {
    this.underTest = new DefaultMimeSupport(mimeTypes);

    when(mimeTypes.getMimeRuleForExtension("zip")).thenReturn(mimeType);
    when(mimeType.isOverride()).thenReturn(true);
    when(mimeType.getMimetypes()).thenReturn(Lists.newArrayList("fake/mimetype"));

    assertThat(underTest.guessMimeTypeFromPath("foo.zip"), is("fake/mimetype"));
  }

  private void assertFileMimeType(final File file, final String mimeType) throws Exception {
    try (InputStream is = new FileInputStream(file)) {
      assertThat(underTest.detectMimeType(is, file.getName()), equalTo(mimeType));
    }
  }

  @Test
  public void verifyBasicFileMimeTypeMatching() throws Exception {
    assertFileMimeType(util.resolveFile("src/test/resources/mime/file.gif"), "image/gif");
    assertFileMimeType(util.resolveFile("src/test/resources/mime/file.zip"), "application/zip");
    assertFileMimeType(util.resolveFile("src/test/resources/mime/empty.zip"), "application/zip");
    assertFileMimeType(util.resolveFile("src/test/resources/mime/file.jar"), "application/java-archive");
  }
}
