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
package org.sonatype.nexus.repository.group.rendered;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link RenderedViewFingerprint}.
 */
public class RenderedViewFingerprintTest
{
  @Test
  public void sameInputs_equalTokenAndMatches() {
    List<String> members = Arrays.asList("repo1", "repo2", "repo3");
    RenderedViewFingerprint fp1 = RenderedViewFingerprint.of("http://example.com", members);
    RenderedViewFingerprint fp2 = RenderedViewFingerprint.of("http://example.com", members);

    assertThat(fp1.toToken(), equalTo(fp2.toToken()));
    assertThat(fp1.matches(fp2.toToken()), is(true));
    assertThat(fp2.matches(fp1.toToken()), is(true));
    assertThat(fp1, equalTo(fp2));
    assertThat(fp1.hashCode(), equalTo(fp2.hashCode()));
  }

  @Test
  public void differentBaseUrl_noMatch() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint fp1 = RenderedViewFingerprint.of("http://example.com", members);
    RenderedViewFingerprint fp2 = RenderedViewFingerprint.of("http://other.com", members);

    assertThat(fp1.toToken(), not(equalTo(fp2.toToken())));
    assertThat(fp1.matches(fp2.toToken()), is(false));
    assertThat(fp1, not(equalTo(fp2)));
  }

  @Test
  public void memberReorder_differentHashNoMatch() {
    List<String> members1 = Arrays.asList("repo1", "repo2", "repo3");
    List<String> members2 = Arrays.asList("repo3", "repo2", "repo1");
    RenderedViewFingerprint fp1 = RenderedViewFingerprint.of("http://example.com", members1);
    RenderedViewFingerprint fp2 = RenderedViewFingerprint.of("http://example.com", members2);

    assertThat(fp1.getMemberSetHash(), not(equalTo(fp2.getMemberSetHash())));
    assertThat(fp1.toToken(), not(equalTo(fp2.toToken())));
    assertThat(fp1.matches(fp2.toToken()), is(false));
    assertThat(fp1, not(equalTo(fp2)));
  }

  @Test
  public void toTokenFromToken_roundTrip() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint original = RenderedViewFingerprint.of("http://example.com", members);

    String token = original.toToken();
    RenderedViewFingerprint parsed = RenderedViewFingerprint.fromToken(token);

    assertThat(parsed, equalTo(original));
    assertThat(parsed.getBaseUrl(), equalTo("http://example.com"));
    assertThat(parsed.getMemberSetHash(), equalTo(original.getMemberSetHash()));
  }

  @Test
  public void nullBaseUrl_handledAndRoundTrips() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint original = RenderedViewFingerprint.of(null, members);

    String token = original.toToken();
    RenderedViewFingerprint parsed = RenderedViewFingerprint.fromToken(token);

    assertThat(parsed.getBaseUrl(), is(nullValue()));
    assertThat(parsed, equalTo(original));
    assertThat(parsed.matches(token), is(true));
  }

  @Test
  public void baseUrlContainingSeparator_roundTrips() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint original = RenderedViewFingerprint.of("http://example.com/a|b", members);

    String token = original.toToken();
    RenderedViewFingerprint parsed = RenderedViewFingerprint.fromToken(token);

    assertThat(parsed.getBaseUrl(), equalTo("http://example.com/a|b"));
    assertThat(parsed.getMemberSetHash(), equalTo(original.getMemberSetHash()));
    assertThat(parsed, equalTo(original));
    assertThat(parsed.matches(token), is(true));
  }

  @Test
  public void nullStoredToken_matchesReturnsFalse() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint fp = RenderedViewFingerprint.of("http://example.com", members);

    assertThat(fp.matches(null), is(false));
  }

  @Test
  public void blankStoredToken_matchesReturnsFalse() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint fp = RenderedViewFingerprint.of("http://example.com", members);

    assertThat(fp.matches(""), is(false));
    assertThat(fp.matches("   "), is(false));
    assertThat(fp.matches("\t"), is(false));
  }

  @Test
  public void emptyMembers_validHash() {
    List<String> members = Collections.emptyList();
    RenderedViewFingerprint fp = RenderedViewFingerprint.of("http://example.com", members);

    assertThat(fp.getMemberSetHash(), not(equalTo("")));
    assertThat(fp.toToken(), containsString("|"));
  }

  @Test(expected = NullPointerException.class)
  public void nullMemberList_throwsNPE() {
    RenderedViewFingerprint.of("http://example.com", null);
  }

  @Test(expected = NullPointerException.class)
  public void fromNullToken_throwsNPE() {
    RenderedViewFingerprint.fromToken(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedTokenMissingSeparator_throws() {
    RenderedViewFingerprint.fromToken("no-separator");
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedTokenEmptyHash_throws() {
    RenderedViewFingerprint.fromToken("baseUrl|");
  }

  @Test
  public void staticComputeMemberSetHash_consistent() {
    List<String> members = Arrays.asList("a", "b", "c");
    String hash1 = RenderedViewFingerprint.computeMemberSetHash(members);
    String hash2 = RenderedViewFingerprint.computeMemberSetHash(members);

    assertThat(hash1, equalTo(hash2));
    assertThat(hash1.length(), equalTo(64)); // SHA-256 hex string length
  }

  @Test
  public void memberSetHash_collisionSafe_boundaryAmbiguity() {
    // Without length-prefixed encoding, ["a","bc"] and ["ab","c"] would have the same hash
    List<String> members1 = Arrays.asList("a", "bc");
    List<String> members2 = Arrays.asList("ab", "c");
    RenderedViewFingerprint fp1 = RenderedViewFingerprint.of("http://example.com", members1);
    RenderedViewFingerprint fp2 = RenderedViewFingerprint.of("http://example.com", members2);

    assertThat(fp1.getMemberSetHash(), not(equalTo(fp2.getMemberSetHash())));
    assertThat(fp1.toToken(), not(equalTo(fp2.toToken())));
  }

  @Test
  public void memberSetHash_collisionSafe_newlineInName() {
    // A name containing a newline should not collide with a two-name list
    List<String> members1 = Arrays.asList("repo1\nrepo2");
    List<String> members2 = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint fp1 = RenderedViewFingerprint.of("http://example.com", members1);
    RenderedViewFingerprint fp2 = RenderedViewFingerprint.of("http://example.com", members2);

    assertThat(fp1.getMemberSetHash(), not(equalTo(fp2.getMemberSetHash())));
    assertThat(fp1.toToken(), not(equalTo(fp2.toToken())));
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedTokenInvalidHashFormat_throws() {
    RenderedViewFingerprint.fromToken("base|xyz"); // not a valid SHA-256 hex string
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedTokenWrongHashLength_throws() {
    RenderedViewFingerprint.fromToken("base|abc123"); // too short to be SHA-256 hex
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedTokenUppercaseHash_throws() {
    // Only lowercase hex is accepted
    RenderedViewFingerprint.fromToken("base|ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890");
  }

  @Test
  public void toString_containsUsefulInfo() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint fp = RenderedViewFingerprint.of("http://example.com", members);

    String str = fp.toString();
    assertThat(str, containsString("RenderedViewFingerprint"));
    assertThat(str, containsString("baseUrl='http://example.com'"));
    assertThat(str, containsString("memberSetHash='"));
  }

  @Test
  public void twoArgOf_pinnedTokenUnchanged() {
    // Regression guard: PyPI depends on this exact token format/value staying byte-identical.
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint fp = RenderedViewFingerprint.of("http://example.com", members);

    assertThat(fp.toToken(),
        equalTo("http://example.com|e202a0d874661703b85ba76ec587b5cfd08d32f79c91d7948c7df2764b1281d5"));
  }

  @Test
  public void threeArgOf_withEmptyExtras_matchesTwoArgOf() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint twoArg = RenderedViewFingerprint.of("http://example.com", members);
    RenderedViewFingerprint threeArg =
        RenderedViewFingerprint.of("http://example.com", members, Collections.emptyList());

    assertThat(threeArg.toToken(), equalTo(twoArg.toToken()));
    assertThat(threeArg, equalTo(twoArg));
  }

  @Test
  public void threeArgOf_withExtraInputs_differsFromTwoArgOf() {
    List<String> members = Arrays.asList("repo1", "repo2");
    RenderedViewFingerprint twoArg = RenderedViewFingerprint.of("http://example.com", members);
    RenderedViewFingerprint threeArg = RenderedViewFingerprint.of(
        "http://example.com", members, Arrays.asList("extra-input"));

    assertThat(threeArg.toToken(), not(equalTo(twoArg.toToken())));
    assertThat(threeArg.matches(twoArg.toToken()), is(false));
  }

  @Test
  public void computeMemberSetHash_withEmptyExtras_matchesOneArgOverload() {
    List<String> members = Arrays.asList("a", "b", "c");

    String oneArg = RenderedViewFingerprint.computeMemberSetHash(members);
    String twoArg = RenderedViewFingerprint.computeMemberSetHash(members, Collections.emptyList());

    assertThat(twoArg, equalTo(oneArg));
  }

  @Test
  public void computeMemberSetHash_withExtraInputs_differsFromOneArgOverload() {
    List<String> members = Arrays.asList("a", "b", "c");

    String oneArg = RenderedViewFingerprint.computeMemberSetHash(members);
    String twoArg = RenderedViewFingerprint.computeMemberSetHash(members, Arrays.asList("pccs-state"));

    assertThat(twoArg, not(equalTo(oneArg)));
  }
}
