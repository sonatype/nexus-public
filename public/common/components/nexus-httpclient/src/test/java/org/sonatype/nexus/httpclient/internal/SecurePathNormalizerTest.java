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
package org.sonatype.nexus.httpclient.internal;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link SecurePathNormalizer}.
 */
public class SecurePathNormalizerTest
{
  @Test
  public void normalizePath_RemovesParentDirectoryReferences() {
    assertThat(SecurePathNormalizer.normalizePath("/bucket/../file.txt"), is("/file.txt"));
    assertThat(SecurePathNormalizer.normalizePath("/a/b/../c"), is("/a/c"));
    assertThat(SecurePathNormalizer.normalizePath("/a/../b/../c"), is("/c"));
  }

  @Test
  public void normalizePath_RemovesCurrentDirectoryReferences() {
    assertThat(SecurePathNormalizer.normalizePath("/./file.txt"), is("/file.txt"));
    assertThat(SecurePathNormalizer.normalizePath("/a/./b/./c"), is("/a/b/c"));
  }

  @Test
  public void normalizePath_RemovesMixedTraversalSequences() {
    assertThat(SecurePathNormalizer.normalizePath("/a/./b/../c"), is("/a/c"));
    assertThat(SecurePathNormalizer.normalizePath("/./a/../b"), is("/b"));
  }

  @Test
  public void normalizePath_PreventsBeyondRootTraversal() {
    assertThat(SecurePathNormalizer.normalizePath("/../file.txt"), is("/file.txt"));
    assertThat(SecurePathNormalizer.normalizePath("/../../etc/passwd"), is("/etc/passwd"));
    assertThat(SecurePathNormalizer.normalizePath("/bucket/../../../etc/passwd"), is("/etc/passwd"));
  }

  @Test
  public void normalizePath_PreservesTrailingSlash() {
    assertThat(SecurePathNormalizer.normalizePath("/a/b/../"), is("/a/"));
    assertThat(SecurePathNormalizer.normalizePath("/a/./b/"), is("/a/b/"));
  }

  @Test
  public void normalizePath_PreservesLeadingSlash() {
    assertThat(SecurePathNormalizer.normalizePath("/a/b/c"), is("/a/b/c"));
    assertThat(SecurePathNormalizer.normalizePath("/"), is("/"));
  }

  @Test
  public void normalizePath_HandlesRootPath() {
    assertThat(SecurePathNormalizer.normalizePath("/"), is("/"));
    assertThat(SecurePathNormalizer.normalizePath("/."), is("/"));
    assertThat(SecurePathNormalizer.normalizePath("/.."), is("/"));
  }

  @Test
  public void normalizePath_HandlesEmptyAndNull() {
    assertThat(SecurePathNormalizer.normalizePath(""), is(""));
    assertThat(SecurePathNormalizer.normalizePath(null), is((String) null));
  }

  @Test
  public void normalizePath_HandlesComplexPaths() {
    assertThat(SecurePathNormalizer.normalizePath("/a/b/c/../../d"), is("/a/d"));
    assertThat(SecurePathNormalizer.normalizePath("/a/b/./c/../d"), is("/a/b/d"));
  }

  @Test
  public void normalizePath_PreservesEncodedCharacters() {
    // %2B is encoded +, should be preserved
    assertThat(SecurePathNormalizer.normalizePath("/path/file%2Bname.txt"), is("/path/file%2Bname.txt"));

    // %23 is encoded #, should be preserved
    assertThat(SecurePathNormalizer.normalizePath("/path/file%23name.txt"), is("/path/file%23name.txt"));

    // Mixed encoding
    assertThat(SecurePathNormalizer.normalizePath("/path/Q1Hny%2BsM/file.txt"), is("/path/Q1Hny%2BsM/file.txt"));
  }

  @Test
  public void normalizePath_PreservesEncodingWhileNormalizingLiteralSequences() {
    // Mix of literal .. and encoded characters
    assertThat(SecurePathNormalizer.normalizePath("/path/../file%2Bname.txt"), is("/file%2Bname.txt"));
    assertThat(SecurePathNormalizer.normalizePath("/a/b%2Bc/../d"), is("/a/d"));
  }

  @Test
  public void normalizePath_DoesNotDecodeEncodedTraversal() {
    // %2e%2e is encoded .., but normalizePath works on raw form
    // so it won't decode and remove it (this is expected - security check uses decoded form)
    assertThat(SecurePathNormalizer.normalizePath("/bucket/%2e%2e/file.txt"), is("/bucket/%2e%2e/file.txt"));
  }

  @Test
  public void normalizePath_RealWorldAwsS3Scenario() {
    // Real AWS S3 signed URL with literal + preserved
    assertThat(SecurePathNormalizer.normalizePath("/bucket/Q1Hny+sM/../file.txt"),
        is("/bucket/file.txt"));
  }

  @Test
  public void normalizePath_RealWorldEncodedPlusScenario() {
    // C++ package with %2B encoding preserved
    assertThat(SecurePathNormalizer.normalizePath("/packages/ncurses-c%2B%2Blibs-6.2-4.rpm"),
        is("/packages/ncurses-c%2B%2Blibs-6.2-4.rpm"));
  }

  @Test
  public void containsPathTraversal_DetectsParentDirectoryReferences() {
    assertThat(SecurePathNormalizer.containsPathTraversal("/bucket/../file.txt"), is(true));
    assertThat(SecurePathNormalizer.containsPathTraversal("/a/b/../c"), is(true));
    assertThat(SecurePathNormalizer.containsPathTraversal("/../etc/passwd"), is(true));
  }

  @Test
  public void containsPathTraversal_DetectsCurrentDirectoryReferences() {
    assertThat(SecurePathNormalizer.containsPathTraversal("/./file.txt"), is(true));
    assertThat(SecurePathNormalizer.containsPathTraversal("/a/./b"), is(true));
  }

  @Test
  public void containsPathTraversal_DetectsMixedSequences() {
    assertThat(SecurePathNormalizer.containsPathTraversal("/a/./b/../c"), is(true));
  }

  @Test
  public void containsPathTraversal_ReturnsFalseForCleanPaths() {
    assertThat(SecurePathNormalizer.containsPathTraversal("/path/to/file.txt"), is(false));
    assertThat(SecurePathNormalizer.containsPathTraversal("/a/b/c"), is(false));
    assertThat(SecurePathNormalizer.containsPathTraversal("/"), is(false));
  }

  @Test
  public void containsPathTraversal_HandlesEmptyAndNull() {
    assertThat(SecurePathNormalizer.containsPathTraversal(""), is(false));
    assertThat(SecurePathNormalizer.containsPathTraversal(null), is(false));
  }

  @Test
  public void containsPathTraversal_DetectsEncodedTraversal() {
    // This is called on DECODED paths, so encoded .. would already be decoded
    assertThat(SecurePathNormalizer.containsPathTraversal("/bucket/../file.txt"), is(true));
  }

  @Test
  public void containsPathTraversal_RealWorldScenarios() {
    // Attack scenarios
    assertThat(SecurePathNormalizer.containsPathTraversal("/bucket/../../../etc/passwd"), is(true));
    assertThat(SecurePathNormalizer.containsPathTraversal("/app/./config/../../../etc/passwd"), is(true));

    // Legitimate paths
    assertThat(SecurePathNormalizer.containsPathTraversal("/packages/ncurses-c++libs-6.2-4.rpm"), is(false));
    assertThat(SecurePathNormalizer.containsPathTraversal("/crates/libgit2-sys/0.13.1+1.4.2.crate"), is(false));
  }

  @Test
  public void containsPathTraversal_DoesNotFlagConsecutiveSlashes_NEXUS_52769() {
    // Cloudflare R2 / AWS S3 signed redirects can contain '//' between path segments.
    // The signature is computed against the exact path bytes, so '//' must not be
    // misclassified as path traversal — collapsing it to '/' invalidates the signature
    // and the storage backend returns 403.

    // Customer's exact path from the JIRA debug log
    assertThat(SecurePathNormalizer.containsPathTraversal(
        "/chainguard-apk-prod/Q1Zk/UAXY46oqyudp6UGE5F6W//gI"), is(false));

    // Synthetic cases
    assertThat(SecurePathNormalizer.containsPathTraversal("/a//b"), is(false));
    assertThat(SecurePathNormalizer.containsPathTraversal("/a///b"), is(false));
    assertThat(SecurePathNormalizer.containsPathTraversal("//"), is(false));

    // Mixed: real '..' alongside '//' must still be flagged
    assertThat(SecurePathNormalizer.containsPathTraversal("/a//b/../c"), is(true));

    // Trailing '//' at end of path
    assertThat(SecurePathNormalizer.containsPathTraversal("/a/b//"), is(false));
  }

  @Test
  public void encodeSpecialCharacters_EncodesLiteralPlus() {
    // Literal + should be encoded to %2B
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/Q1Hny+sM/file.txt"),
        is("/path/Q1Hny%2BsM/file.txt"));
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/file+name.txt"),
        is("/file%2Bname.txt"));
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/c++libs"),
        is("/c%2B%2Blibs"));
  }

  @Test
  public void encodeSpecialCharacters_EncodesHash() {
    // Literal # should be encoded to %23
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file#test.txt"),
        is("/path/file%23test.txt"));
  }

  @Test
  public void encodeSpecialCharacters_EncodesSpace() {
    // Literal space should be encoded to %20
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file name.txt"),
        is("/path/file%20name.txt"));
  }

  @Test
  public void encodeSpecialCharacters_PreservesAlreadyEncodedSequences() {
    // Already-encoded sequences should be preserved
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file%2Bname.txt"),
        is("/path/file%2Bname.txt"));
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file%23name.txt"),
        is("/path/file%23name.txt"));
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file%20name.txt"),
        is("/path/file%20name.txt"));
  }

  @Test
  public void encodeSpecialCharacters_MixedLiteralAndEncoded() {
    // Mix of literal and already-encoded characters
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file+name%2Btest.txt"),
        is("/path/file%2Bname%2Btest.txt"));
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/Q1Hny+sM/file%2Bname#test.txt"),
        is("/path/Q1Hny%2BsM/file%2Bname%23test.txt"));
  }

  @Test
  public void encodeSpecialCharacters_PreservesNormalCharacters() {
    // Normal characters should not be encoded
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file-name_test.txt"),
        is("/path/file-name_test.txt"));
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/path/file~name.txt"),
        is("/path/file~name.txt"));
  }

  @Test
  public void encodeSpecialCharacters_HandlesEmptyAndNull() {
    assertThat(SecurePathNormalizer.encodeSpecialCharacters(""), is(""));
    assertThat(SecurePathNormalizer.encodeSpecialCharacters(null), is((String) null));
  }

  @Test
  public void encodeSpecialCharacters_RealWorldChainguardScenario() {
    // Real Chainguard APK redirect with literal + in base64 hash
    // Note: = is safe in paths and doesn't need encoding
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/chainguard-apk-prod/Q1Hny+sM/BUTTvD1WdIaAuSeTTg10="),
        is("/chainguard-apk-prod/Q1Hny%2BsM/BUTTvD1WdIaAuSeTTg10="));
  }

  @Test
  public void encodeSpecialCharacters_RealWorldAwsS3Scenario() {
    // Real AWS S3 scenario with C++ package
    assertThat(SecurePathNormalizer.encodeSpecialCharacters("/packages/ncurses-c++libs-6.2-4.rpm"),
        is("/packages/ncurses-c%2B%2Blibs-6.2-4.rpm"));
  }
}
