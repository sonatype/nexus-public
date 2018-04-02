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

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Strings;
import org.junit.Test;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@link NpmPackageId}.
 */
public class NpmPackageIdTest
    extends TestSupport
{
  @Test
  public void parseGood() {
    final String idStr = "@scope/name";
    NpmPackageId id = NpmPackageId.parse(idStr);
    assertThat(id.id(), equalTo(idStr));
    assertThat(id.scope(), equalTo("scope"));
    assertThat(id.name(), equalTo("name"));

    NpmPackageId id2 = NpmPackageId.parse(idStr);

    assertThat(id2, equalTo(id));
    assertThat(id2.hashCode(), equalTo(id.hashCode()));
    assertThat(id2.compareTo(id), equalTo(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedNonUrlSafeCharacters1() {
    NpmPackageId.parse("@sco/pe/name");
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedNonUrlSafeCharacters2() {
    NpmPackageId.parse("@scópe/name");
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedNameStartsWithDot() {
    NpmPackageId.parse("@scope/.name");
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedScopeStartsWithDot() {
    NpmPackageId.parse("@.scope/name");
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

  @Test(expected = IllegalArgumentException.class)
  public void malformedNoScope() {
    NpmPackageId.parse("@/name");
  }

  @Test(expected = IllegalArgumentException.class)
  public void malformedNoName() {
    NpmPackageId.parse("@scope/");
  }
}
