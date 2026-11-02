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
package org.sonatype.nexus.logging;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.sonatype.nexus.logging.LogDiscriminators.MAX_LENGTH;
import static org.sonatype.nexus.logging.LogDiscriminators.safe;

class LogDiscriminatorsTest
{
  private static final String ALLOWED = "[^/\\\\${}\\[\\]()|?*+%@,]+";

  @Test
  void validValueIsUnchanged() {
    assertThat(safe("maven-central.proxy_1"), is("maven-central.proxy_1"));
  }

  @Test
  void valueAtMaxLengthIsUnchanged() {
    String value = "a".repeat(MAX_LENGTH);
    assertThat(safe(value), is(value));
  }

  @Test
  void longValueIsShortened() {
    String value = "hosted.repository.evaluation.descope.pending.index-20261013101500123";
    String result = safe(value);
    assertThat(result.length(), lessThanOrEqualTo(MAX_LENGTH));
    assertThat(result, startsWith("hosted.repository.evaluation"));
  }

  @Test
  void longValuesWithSamePrefixStayDistinct() {
    String prefix = "x".repeat(MAX_LENGTH);
    assertThat(safe(prefix + "1"), not(is(safe(prefix + "2"))));
  }

  @Test
  void absolutePathIsMadeSafe() {
    String result = safe("/nexus-data/blobs/default/reconciliation");
    assertThat(result.length(), lessThanOrEqualTo(MAX_LENGTH));
    assertThat(result, matchesPattern(ALLOWED));
  }

  @Test
  void distinctPathsStayDistinct() {
    assertThat(safe("/a/b"), not(is(safe("/a_b"))));
  }

  @Test
  void dotDotIsRemoved() {
    String result = safe("../escape");
    assertThat(result.contains(".."), is(false));
    assertThat(result, matchesPattern(ALLOWED));
  }

  @Test
  void emptyValueIsReplaced() {
    assertThat(safe("").isEmpty(), is(false));
  }

  @Test
  void nullIsPassedThrough() {
    assertThat(safe(null), is(nullValue()));
  }

  @Test
  void customMaxLengthIsHonoured() {
    String result = safe("repository.pypi.generate-missing-sha256-checksums", 46);
    assertThat(result.length(), lessThanOrEqualTo(46));
    assertThat(result, startsWith("repository.pypi"));
  }
}
