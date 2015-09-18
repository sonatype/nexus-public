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

import org.junit.Test;

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
  private void assertNullPath(final String input) {
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
    assertNullPath(null);
  }

  @Test
  public void emptyPath() {
    assertNullPath("");
  }

  @Test
  public void bareSlash() {
    assertNullPath("/");
  }

  @Test
  public void missingRepoPathSeperator() {
    assertNullPath("/repo");
  }

  @Test
  public void missingLeadingSlash() {
    assertNullPath("repo");
  }

  @Test
  public void repoDots() {
    // repository name can not be a . or ..
    assertNullPath("/./");
    assertNullPath("/../");
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
  public void invalidRelative1() {
    assertNullPath("/repo/..");
  }

  @Test
  public void invalidRelative2() {
    assertNullPath("/repo/../bar");
  }

  @Test
  public void invalidRelative3() {
    assertNullPath("/repo/foo/../../bar");
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
}