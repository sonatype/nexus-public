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
package org.sonatype.nexus.repository.search.sql.store;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.search.sql.store.SearchRecordData.MAX_TOTAL_SEARCH_PARAMS;

/**
 * Test for NEXUS-50251 - Verify parameter limit enforcement
 */
public class SearchRecordDataParameterLimitTest
{
  private SearchRecordData underTest;

  @BeforeEach
  public void setUp() {
    underTest = new SearchRecordData(1, 100, "test", true);
  }

  @Test
  public void testParameterLimitEnforcement() {
    // Test that the SQL parameter limit is enforced across ALL field types.
    // This ensures the shared counter properly tracks SQL parameters (not just Java collection sizes).
    //
    // IMPORTANT: INSERT...ON CONFLICT...DO UPDATE means parameters are used in BOTH clauses
    // - paths: 4× SQL parameters (2× per clause: join + toQuotedTsVector, × 2 clauses)
    // - All other collections: 2× SQL parameters (1× per clause: toTsVector, × 2 clauses)

    // To reach exactly 60,000 SQL parameters:
    // - paths: 3,000 items × 4 = 12,000 SQL params
    // - md5, sha1, sha256, sha512, tags: 5 × 4,800 items × 2 = 48,000 SQL params
    // - Total: 60,000 SQL params

    // Add 3,000 paths (= 12,000 SQL params)
    for (int i = 0; i < 3000; i++) {
      underTest.addPath("/path" + i);
    }

    // Add 4,800 each to other collections (= 48,000 SQL params)
    for (int i = 0; i < 4800; i++) {
      underTest.addMd5("md5_" + i);
      underTest.addSha1("sha1_" + i);
      underTest.addSha256("sha256_" + i);
      underTest.addSha512("sha256_" + i);
      underTest.setTags(List.of("tag_" + i));
    }

    // Verify we're at exactly 60,000 SQL params
    // Calculate: (paths×2 + others×1) × 2 for INSERT+UPDATE
    int insertUpdateParams = (underTest.getPaths().size() * 2) +
        underTest.getMd5().size() +
        underTest.getSha1().size() +
        underTest.getSha256().size() +
        underTest.getSha512().size() +
        underTest.getTags().size();
    int actualSqlParams = insertUpdateParams * 2;

    // Total SQL params should be exactly 60,000 (MAX_TOTAL_SEARCH_PARAMS)
    assertThat("Total SQL parameters should be exactly at limit", actualSqlParams, is(MAX_TOTAL_SEARCH_PARAMS));

    // Now try to add one more item to any field
    // This should be rejected since we're at the limit
    int pathsSizeBefore = underTest.getPaths().size();
    underTest.addPath("/one/more/path");
    int pathsSizeAfter = underTest.getPaths().size();

    // Verify the additional item was NOT added
    assertThat("Additional item should be rejected when at limit",
        pathsSizeAfter, is(pathsSizeBefore));
  }

  @Test
  public void testBelowLimitNoRestriction() {
    // Add items well below the limit
    // With 60,000 total SQL params:
    // - 1,000 paths = 4,000 SQL params
    // - 1,000 md5 = 2,000 SQL params
    // - 1,000 sha1 = 2,000 SQL params
    // - Total = 8,000 SQL params (well below 60,000 limit)
    for (int i = 0; i < 1000; i++) {
      underTest.addPath("/path" + i);
      underTest.addMd5("md5" + i);
      underTest.addSha1("sha1" + i);
    }

    // All items should be added
    assertThat(underTest.getPaths().size(), is(1000));
    assertThat(underTest.getMd5().size(), is(1000));
    assertThat(underTest.getSha1().size(), is(1000));
  }

  @Test
  public void testJustAtLimit() {
    // Add paths until we hit the SQL parameter limit
    // Paths count as 4× SQL parameters (used twice per clause × INSERT+UPDATE clauses)
    // We can add MAX_TOTAL_SEARCH_PARAMS / 4 = 15,000 paths
    int maxPaths = MAX_TOTAL_SEARCH_PARAMS / 4;
    for (int i = 0; i < maxPaths; i++) {
      underTest.addPath("/path" + i);
    }

    // Should have all 15,000 paths (= 60,000 SQL params total)
    assertThat(underTest.getPaths().size(), is(maxPaths));

    // Try to add one more
    underTest.addMd5("extraMd5");

    // MD5 collection should be empty (limit reached)
    assertThat(underTest.getMd5().size(), is(0));
  }

  @Test
  public void testTokenizationRespectsLimit() {
    // This test verifies the fix for the root cause: tokenization creating multiple parameters
    // Simulate the real-world scenario where paths get tokenized into multiple search parameters
    // With 65,534 assets × ~2.5 tokens each = ~160,000 potential SQL parameters
    // The fix ensures we stop at 60,000 total SQL parameters

    // First, fill up most of the limit with paths
    // Since paths count as 4× SQL parameters (2× per clause × INSERT+UPDATE),
    // we add 14,250 paths = 57,000 SQL params
    for (int i = 0; i < 14250; i++) {
      underTest.addPath("/path" + i);
    }

    // Now we have 3,000 SQL params left. Try to add keywords that tokenize
    // Keywords count as 2× SQL parameters (1× per clause × INSERT+UPDATE)
    // So we have room for 1,500 keywords
    // Each keyword "component name version X" tokenizes into multiple keyword params
    // Without the fix, this would exceed the 60,000 limit and cause PostgreSQL errors
    // With the fix, it should stop adding when limit is reached
    int keywordsBefore = underTest.getKeywords().size();

    for (int i = 0; i < 1500; i++) {
      // Multi-word phrases that will be tokenized
      underTest.addKeyword("component name version " + i);
    }

    int keywordsAfter = underTest.getKeywords().size();

    // Keywords should have been added, but not all 1,500 if tokenization multiplies the count
    // The key fix is that tokenization now checks the limit before adding the final result
    assertThat("Some keywords should have been added", keywordsAfter > keywordsBefore, is(true));

    // Verify we're at or very close to the limit by trying to add more
    int pathsBefore = underTest.getPaths().size();
    underTest.addPath("/one/more/path");
    int pathsAfter = underTest.getPaths().size();

    // Should not be able to add more paths (limit reached)
    assertThat("Should not add more paths after limit reached",
        pathsAfter, is(pathsBefore));
  }

  @Test
  public void testTokenizationFinalAddRespectsLimit() {
    // Edge case: ensure the final tokenized string addition also checks the limit
    // This tests the fix for the canAddSearchParam() check after the tokenization loop

    // Fill to exactly 59,998 SQL parameters (just below the 60,000 limit)
    // Since paths count as 4× SQL parameters (2× per clause × INSERT+UPDATE),
    // and keywords count as 2× SQL parameters (1× per clause × INSERT+UPDATE):
    // - 14,999 paths = 59,996 SQL params
    // - 1 keyword = 2 SQL params
    // - Total = 59,998 SQL params
    for (int i = 0; i < 14999; i++) {
      underTest.addPath("/path" + i);
    }
    underTest.addKeyword("existing");

    // Verify we're at 14,999 paths + 1 keyword = 59,998 SQL params
    assertThat(underTest.getPaths().size(), is(14999));
    int insertUpdateParams = (underTest.getPaths().size() * 2) + underTest.getKeywords().size();
    int actualSqlParams = insertUpdateParams * 2; // Double for INSERT + UPDATE
    assertThat(actualSqlParams, is(59998));

    // Now add a keyword that tokenizes. This will:
    // 1. Add the whole phrase as 2 SQL params (59,998 → 60,000)
    // 2. Try to add the tokenized version as additional params (would exceed limit, should be rejected!)
    int keywordsBefore = underTest.getKeywords().size();
    underTest.addKeyword("component name version");
    int keywordsAfter = underTest.getKeywords().size();

    // The whole phrase should be added (bringing us to 60,000 SQL params)
    // But the tokenized version should NOT be added (would exceed limit)
    // So keywords collection should increase (by 1 for the whole phrase)
    assertThat("Keyword should have been added", keywordsAfter > keywordsBefore, is(true));

    // Verify we cannot add any more items
    int pathsBefore = underTest.getPaths().size();
    underTest.addPath("/extra/path");
    int pathsAfter = underTest.getPaths().size();

    assertThat("Should not add more paths after hitting limit",
        pathsAfter, is(pathsBefore));
  }

  @Test
  public void testTsvectorByteLimitPreventsOverflow() {
    // NEXUS-52625: Simulate the tsvector overflow scenario.
    // Add keywords with long paths (~400 bytes each) until the byte limit is reached.
    // The byte limit should stop additions before hitting PostgreSQL's 1MB tsvector limit.

    int addedCount = 0;
    for (int i = 0; i < 5000; i++) {
      int keywordsBefore = underTest.getKeywords().size();
      // ~400 byte keyword simulating long YUM directory paths
      String longKeyword = "enterprise-datacenter-region-" + String.format("%06d", i) +
          "-westeurope/satellite-release-channel-production-" + String.format("%06d", i * 3) +
          "-stable/rhel8-server-appstream-optional-" + String.format("%06d", i * 7) +
          "-updates/x86_64-baseos-packages-multilib-" + String.format("%06d", i * 11) +
          "-debuginfo/repository-snapshot-nightly-build-" + String.format("%06d", i * 13);
      underTest.addKeyword(longKeyword);

      if (underTest.getKeywords().size() == keywordsBefore) {
        break; // Limit reached
      }
      addedCount++;
    }

    // Should have stopped well before 5000 due to byte limit
    assertThat("Byte limit should prevent adding all 5000 keywords",
        addedCount < 5000, is(true));
    // Should have added at least some keywords before hitting the limit
    assertThat("Should have added some keywords before hitting limit",
        addedCount > 100, is(true));
  }

  @Test
  public void testPathByteLimitPreventsOverflow() {
    // NEXUS-52625: Verify that path additions are also protected by byte limit

    int addedCount = 0;
    for (int i = 0; i < 5000; i++) {
      int pathsBefore = underTest.getPaths().size();
      String longPath = "/enterprise-datacenter-" + String.format("%06d", i) +
          "/satellite-release-channel-" + String.format("%06d", i * 3) +
          "/rhel8-server-appstream-" + String.format("%06d", i * 7) +
          "/x86_64-baseos-packages-" + String.format("%06d", i * 11) +
          "/repository-snapshot-build-" + String.format("%06d", i * 13) +
          "/updates-security-advisory-" + String.format("%06d", i * 17) +
          "/Packages/ant.rpm";
      underTest.addPath(longPath);

      if (underTest.getPaths().size() == pathsBefore) {
        break; // Limit reached
      }
      addedCount++;
    }

    // Should have stopped before 5000 due to byte limit
    assertThat("Byte limit should prevent adding all 5000 paths",
        addedCount < 5000, is(true));
    assertThat("Should have added some paths before hitting limit",
        addedCount > 100, is(true));
  }

  @Test
  public void testSmallKeywordsDontTriggerByteLimit() {
    // Normal usage: small keywords should never hit the byte limit
    for (int i = 0; i < 100; i++) {
      underTest.addKeyword("component-" + i);
    }
    // All 100 small keywords should be added (each adds 2 entries: phrase + tokenized)
    // The exact count depends on tokenization, but should be well over 100
    assertThat("Small keywords should not be blocked by byte limit",
        underTest.getKeywords().size() > 100, is(true));
  }
}
