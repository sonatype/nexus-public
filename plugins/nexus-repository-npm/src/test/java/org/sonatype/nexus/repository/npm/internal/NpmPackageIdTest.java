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
package org.sonatype.nexus.repository.npm.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Strings;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * Test for {@link NpmPackageId}.
 */
@RunWith(JUnitParamsRunner.class)
public class NpmPackageIdTest
    extends TestSupport
{
  @Test
  @Parameters
  public void parseGood(final String idStr, final String expectedScope, final String expectedName) {
    NpmPackageId id = NpmPackageId.parse(idStr);
    assertThat(id.id(), equalTo(idStr));
    assertThat(id.scope(), equalTo(expectedScope));
    assertThat(id.name(), equalTo(expectedName));

    NpmPackageId id2 = NpmPackageId.parse(idStr);

    assertThat(id2, equalTo(id));
    assertThat(id2.hashCode(), equalTo(id.hashCode()));
    assertThat(id2.compareTo(id), equalTo(0));
  }

  @SuppressWarnings("unused")
  private List<List<String>> parametersForParseGood() {
    return Arrays.asList(
        Arrays.asList("@scope/name", "scope", "name"),
        Arrays.asList("@scope/.name", "scope", ".name"),
        Arrays.asList("@scope/_name", "scope", "_name"),
        Arrays.asList("@sc.ope/name", "sc.ope", "name"),
        Arrays.asList("@sc_ope/name", "sc_ope", "name"),
        Arrays.asList("@scope/na.me", "scope", "na.me"),
        Arrays.asList("@scope/na_me", "scope", "na_me"),
        Arrays.asList("@sc.ope/na.me", "sc.ope", "na.me"),
        Arrays.asList("@sc_ope/na_me", "sc_ope", "na_me"),
        Arrays.asList("@sc.ope_/na.me_", "sc.ope_", "na.me_"),
        Arrays.asList("@sc_ope./na_me.", "sc_ope.", "na_me.")
      );
  }
  
  
  @Test(expected = IllegalArgumentException.class)
  @Parameters
  public void parseBad(String id) {
    NpmPackageId.parse(id);
  }

  @SuppressWarnings("unused")
  private List<String> parametersForParseBad() {
    return Arrays.asList(
        "",
        "@/",
        "@./",
        "@.scope/",
        "@_scope/",
        "scope/",
        "@scope/",
        "@/name",
        "/name",
        "@/name",
        "@sco/pe/name",
        "@scópe/name",
        "@.scope/name",
        "@_scope/name"
      );
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedTooLong() {
    NpmPackageId.parse(Strings.repeat("0123456789", 21) + "1234");
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedScopedTooLong() {
    // 1 + 110 + 3 + 100
    NpmPackageId.parse("@" + Strings.repeat("0123456789", 11) + "/ab" + Strings.repeat("0123456789", 10));
  }

}
