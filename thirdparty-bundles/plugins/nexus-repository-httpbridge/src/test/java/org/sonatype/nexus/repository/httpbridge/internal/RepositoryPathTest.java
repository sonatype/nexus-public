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
package org.sonatype.nexus.repository.httpbridge.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link RepositoryPath}.
 */
public class RepositoryPathTest
    extends TestSupport
{
  static final String RELATIVE_TOKEN_MESSAGE = "Repository path must not contain a relative token";

  static final String NULL_OR_EMPTY_MESSAGE = "Repository path must not be null or empty";

  static final String MULTIPLE_SLASH_MESSAGE = "Repository path must have another '/' after initial '/'";

  static final String START_WITH_SLASH_MESSAGE = "Repository path must start with '/'";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private void assertExceptionOnInvalidPath(final String input, final String message) {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(message);
    RepositoryPath path = RepositoryPath.parse(input);
    assertThat(path, nullValue());
  }

  private void assertPath(final String path, final String expectedRepoName, final String expectedRemainingPath) {
    final RepositoryPath parsedPath = RepositoryPath.parse(path);
    assertThat(parsedPath, notNullValue());
    assertThat(parsedPath.getRepositoryName(), is(expectedRepoName));
    assertThat(parsedPath.getRemainingPath(), is(expectedRemainingPath));
  }

  @Test
  public void nullPath() {
    assertExceptionOnInvalidPath(null, NULL_OR_EMPTY_MESSAGE);
  }

  @Test
  public void emptyPath() {
    assertExceptionOnInvalidPath("", NULL_OR_EMPTY_MESSAGE);
  }

  @Test
  public void bareSlash() {
    assertExceptionOnInvalidPath("/", MULTIPLE_SLASH_MESSAGE);
  }

  @Test
  public void missingRepoPathSeperator() {
    assertExceptionOnInvalidPath("/repo", MULTIPLE_SLASH_MESSAGE);
  }

  @Test
  public void missingLeadingSlash() {
    assertExceptionOnInvalidPath("repo", START_WITH_SLASH_MESSAGE);
  }

  @Test
  public void repoDot() {
    assertExceptionOnInvalidPath("/./", RELATIVE_TOKEN_MESSAGE);
  }

  @Test
  public void repoDots() {
    assertExceptionOnInvalidPath("/../", RELATIVE_TOKEN_MESSAGE);
  }

  @Test
  public void invalidRelative1() {
    assertExceptionOnInvalidPath("/repo/..", RELATIVE_TOKEN_MESSAGE);
  }

  @Test
  public void invalidRelative2() {
    assertExceptionOnInvalidPath("/repo/../bar", RELATIVE_TOKEN_MESSAGE);
  }

  @Test
  public void invalidRelative3() {
    assertExceptionOnInvalidPath("/repo/foo/../../bar", RELATIVE_TOKEN_MESSAGE);
  }

  @Test
  public void repoAndRootPath() {
    // allow root path
    assertPath("/repo/", "repo", "/");
  }

  @Test
  public void repoAndSimplePath() {
    assertPath("/repo/path", "repo", "/path");
  }

  @Test
  public void complexPath() {
    assertPath("/repo/foo/bar/baz", "repo", "/foo/bar/baz");
  }

  @Test
  public void sillySlashes() {
    assertPath("/repo/foo/////bar/baz", "repo", "/foo/bar/baz");
  }

  @Test
  public void relativePath() {
    assertPath("/repo/foo/../bar/../baz", "repo", "/baz");
  }

  @Test
  public void dotReference1() {
    assertPath("/repo/foo/./baz", "repo", "/foo/baz");
  }

  @Test
  public void dotReference2() {
    assertPath("/repo/foo/././baz", "repo", "/foo/baz");
  }

  @Test
  public void fileWithDot() {
    assertPath("/repo/foo/baz.bar", "repo", "/foo/baz.bar");
  }

  @Test
  public void fileWithSpaces() throws Exception {
    assertPath("/repo/foo/abc bar.txt", "repo", "/foo/abc bar.txt");
  }
}
