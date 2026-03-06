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
package org.sonatype.nexus.repository.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Test for Maven classifier and extension extraction regex.
 * This regex is used to auto-extract classifier and extension from uploaded filenames.
 *
 * @see MavenUploadHandlerSupport#getDefinition()
 */
public class MavenClassifierExtractionRegexTest
    extends TestSupport
{
  /**
   * Regex pattern that extracts classifier and extension from Maven artifact filenames.
   * <p>
   * Pattern explanation:
   * - Matches version part: -<digits>[-<SNAPSHOT|word>]*
   * - Optionally matches whitelisted classifier: -(sources|javadoc|tests|...)
   * - Matches extension: .<word>
   * <p>
   * Whitelisted classifiers are standard Maven classifiers.
   * Non-whitelisted suffixes (like -test, -alpha, -RC1) are treated as version suffixes, not classifiers.
   */
  private static final String MAVEN_CLASSIFIER_AND_EXTENSION_EXTRACTOR_REGEX =
      "-(?:(?:\\.?\\d)+(?:-(?:SNAPSHOT|[\\w]+))*?)(?:-(sources|javadoc|tests|test-sources|test-jar|client|server|shaded))?\\.((?:\\.?\\w)+)$";

  private static final Pattern PATTERN = Pattern.compile(MAVEN_CLASSIFIER_AND_EXTENSION_EXTRACTOR_REGEX);

  @Test
  public void testExtractClassifier_versionSuffixNotExtractedAsClassifier() {
    // NEXUS-44467: Version suffixes like -test, -alpha, -beta, -RC1 should NOT be extracted as classifiers
    assertClassifierAndExtension("intersmash-core-0.0.3-test.pom", "", "pom");
    assertClassifierAndExtension("artifact-2.0.0-alpha.jar", "", "jar");
    assertClassifierAndExtension("artifact-3.0.0-RC1.jar", "", "jar");
    assertClassifierAndExtension("library-2.1.3-beta.war", "", "war");
  }

  @Test
  public void testExtractClassifier_whitelistedClassifiersExtracted() {
    // Standard Maven classifiers should be extracted
    assertClassifierAndExtension("artifact-1.0.0-sources.jar", "sources", "jar");
    assertClassifierAndExtension("artifact-1.0.0-javadoc.jar", "javadoc", "jar");
    assertClassifierAndExtension("component-5.0-tests.jar", "tests", "jar");
    assertClassifierAndExtension("artifact-1.0.0-test-sources.jar", "test-sources", "jar");
  }

  @Test
  public void testExtractClassifier_versionSuffixAndClassifier() {
    // Files with both version suffix and classifier should extract only the classifier
    assertClassifierAndExtension("library-1.0-alpha-sources.jar", "sources", "jar");
    assertClassifierAndExtension("tool-2.0-RC1-javadoc.jar", "javadoc", "jar");
    assertClassifierAndExtension("artifact-3.0-beta-tests.jar", "tests", "jar");
  }

  @Test
  public void testExtractClassifier_snapshotVersions() {
    // SNAPSHOT versions should work correctly
    assertClassifierAndExtension("artifact-1.0.0-SNAPSHOT.jar", "", "jar");
    assertClassifierAndExtension("artifact-1.0.0-SNAPSHOT-sources.jar", "sources", "jar");
  }

  @Test
  public void testExtractClassifier_noClassifier() {
    // Files without classifiers should have empty classifier
    assertClassifierAndExtension("artifact-1.0.0.jar", "", "jar");
    assertClassifierAndExtension("library-2.1.3.war", "", "war");
    assertClassifierAndExtension("component-3.0.0.pom", "", "pom");
  }

  @Test
  public void testExtractExtension_variousExtensions() {
    // Various file extensions should be extracted correctly
    assertClassifierAndExtension("artifact-1.0.0.jar", "", "jar");
    assertClassifierAndExtension("artifact-1.0.0.war", "", "war");
    assertClassifierAndExtension("artifact-1.0.0.pom", "", "pom");
    assertClassifierAndExtension("artifact-1.0.0.zip", "", "zip");
    assertClassifierAndExtension("artifact-1.0.0.tar.gz", "", "tar.gz");
  }

  private void assertClassifierAndExtension(String filename, String expectedClassifier, String expectedExtension) {
    Matcher matcher = PATTERN.matcher(filename);
    assertThat("Filename should match pattern: " + filename, matcher.find(), is(true));

    String actualClassifier = matcher.group(1);
    String actualExtension = matcher.group(2);

    assertThat("Classifier for " + filename,
        actualClassifier != null ? actualClassifier : "",
        equalTo(expectedClassifier));
    assertThat("Extension for " + filename, actualExtension, equalTo(expectedExtension));
  }
}
