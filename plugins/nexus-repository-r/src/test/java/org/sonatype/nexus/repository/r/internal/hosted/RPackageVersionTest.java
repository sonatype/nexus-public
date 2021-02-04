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
package org.sonatype.nexus.repository.r.internal.hosted;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link RPackageVersion} unit tests.
 */
public class RPackageVersionTest
    extends TestSupport
{
  @Test
  public void testComparingGreaterVersions() {
    // comparing dot versions to dot versions
    assertThat(new RPackageVersion("0.0.2"), is(greaterThan(new RPackageVersion("0.0.1"))));
    assertThat(new RPackageVersion("0.2.0"), is(greaterThan(new RPackageVersion("0.1.0"))));
    assertThat(new RPackageVersion("0.2.0"), is(greaterThan(new RPackageVersion("0.1.1"))));
    assertThat(new RPackageVersion("1.0.0"), is(greaterThan(new RPackageVersion("0.0.1"))));
    assertThat(new RPackageVersion("1.0.0"), is(greaterThan(new RPackageVersion("0.1.0"))));
    assertThat(new RPackageVersion("1.2.0"), is(greaterThan(new RPackageVersion("1.1.0"))));
    assertThat(new RPackageVersion("1.2.0"), is(greaterThan(new RPackageVersion("1.1.9"))));
    assertThat(new RPackageVersion("2.0.0"), is(greaterThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("2.0.0"), is(greaterThan(new RPackageVersion("1.1.0"))));
    assertThat(new RPackageVersion("2.0.0"), is(greaterThan(new RPackageVersion("1.9.9"))));
    assertThat(new RPackageVersion("10.0.0"), is(greaterThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("10.0.0"), is(greaterThan(new RPackageVersion("9.9.9999"))));
    assertThat(new RPackageVersion("10.0.0"), is(greaterThan(new RPackageVersion("9.9999.9999"))));

    // comparing dot-dash versions to dot-dash versions
    assertThat(new RPackageVersion("0.0-2"), is(greaterThan(new RPackageVersion("0.0-1"))));
    assertThat(new RPackageVersion("0.2-0"), is(greaterThan(new RPackageVersion("0.1-0"))));
    assertThat(new RPackageVersion("0.2-0"), is(greaterThan(new RPackageVersion("0.1-1"))));
    assertThat(new RPackageVersion("1.0-0"), is(greaterThan(new RPackageVersion("0.0-1"))));
    assertThat(new RPackageVersion("1.0-0"), is(greaterThan(new RPackageVersion("0.1-0"))));
    assertThat(new RPackageVersion("1.2-0"), is(greaterThan(new RPackageVersion("1.1-0"))));
    assertThat(new RPackageVersion("1.2-0"), is(greaterThan(new RPackageVersion("1.1-9"))));
    assertThat(new RPackageVersion("2.0-0"), is(greaterThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("2.0-0"), is(greaterThan(new RPackageVersion("1.1-0"))));
    assertThat(new RPackageVersion("2.0-0"), is(greaterThan(new RPackageVersion("1.9-9"))));
    assertThat(new RPackageVersion("10.0-0"), is(greaterThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("10.0-0"), is(greaterThan(new RPackageVersion("9.9-9999"))));
    assertThat(new RPackageVersion("10.0-0"), is(greaterThan(new RPackageVersion("9.9999-9999"))));

    // comparing dot versions to dot-dash versions
    assertThat(new RPackageVersion("0.0.2"), is(greaterThan(new RPackageVersion("0.0-1"))));
    assertThat(new RPackageVersion("0.2.0"), is(greaterThan(new RPackageVersion("0.1-0"))));
    assertThat(new RPackageVersion("0.2.0"), is(greaterThan(new RPackageVersion("0.1-1"))));
    assertThat(new RPackageVersion("1.0.0"), is(greaterThan(new RPackageVersion("0.0-1"))));
    assertThat(new RPackageVersion("1.0.0"), is(greaterThan(new RPackageVersion("0.1-0"))));
    assertThat(new RPackageVersion("1.2.0"), is(greaterThan(new RPackageVersion("1.1-0"))));
    assertThat(new RPackageVersion("1.2.0"), is(greaterThan(new RPackageVersion("1.1-9"))));
    assertThat(new RPackageVersion("2.0.0"), is(greaterThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("2.0.0"), is(greaterThan(new RPackageVersion("1.1-0"))));
    assertThat(new RPackageVersion("2.0.0"), is(greaterThan(new RPackageVersion("1.9-9"))));
    assertThat(new RPackageVersion("10.0.0"), is(greaterThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("10.0.0"), is(greaterThan(new RPackageVersion("9.9-9999"))));
    assertThat(new RPackageVersion("10.0.0"), is(greaterThan(new RPackageVersion("9.9999-9999"))));

    // comparing dot-dash versions to dot versions
    assertThat(new RPackageVersion("0.0-2"), is(greaterThan(new RPackageVersion("0.0.1"))));
    assertThat(new RPackageVersion("0.2-0"), is(greaterThan(new RPackageVersion("0.1.0"))));
    assertThat(new RPackageVersion("0.2-0"), is(greaterThan(new RPackageVersion("0.1.1"))));
    assertThat(new RPackageVersion("1.0-0"), is(greaterThan(new RPackageVersion("0.0.1"))));
    assertThat(new RPackageVersion("1.0-0"), is(greaterThan(new RPackageVersion("0.1.0"))));
    assertThat(new RPackageVersion("1.2-0"), is(greaterThan(new RPackageVersion("1.1.0"))));
    assertThat(new RPackageVersion("1.2-0"), is(greaterThan(new RPackageVersion("1.1.9"))));
    assertThat(new RPackageVersion("2.0-0"), is(greaterThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("2.0-0"), is(greaterThan(new RPackageVersion("1.1.0"))));
    assertThat(new RPackageVersion("2.0-0"), is(greaterThan(new RPackageVersion("1.9.9"))));
    assertThat(new RPackageVersion("10.0-0"), is(greaterThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("10.0-0"), is(greaterThan(new RPackageVersion("9.9.9999"))));
    assertThat(new RPackageVersion("10.0-0"), is(greaterThan(new RPackageVersion("9.9999.9999"))));
  }

  @Test
  public void testComparingEqualVersions() {
    // comparing dot versions to dot versions
    assertThat(new RPackageVersion("0.0.2"), is(equalTo(new RPackageVersion("0.0.2"))));
    assertThat(new RPackageVersion("0.2.0"), is(equalTo(new RPackageVersion("0.2.0"))));
    assertThat(new RPackageVersion("0.2.2"), is(equalTo(new RPackageVersion("0.2.2"))));
    assertThat(new RPackageVersion("1.0.0"), is(equalTo(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("1.2.0"), is(equalTo(new RPackageVersion("1.2.0"))));
    assertThat(new RPackageVersion("10.0.0"), is(equalTo(new RPackageVersion("10.0.0"))));
    assertThat(new RPackageVersion("9.9999.9999"), is(equalTo(new RPackageVersion("9.9999.9999"))));

    // comparing dot-dash versions to dot-dash versions
    assertThat(new RPackageVersion("0.0-2"), is(equalTo(new RPackageVersion("0.0-2"))));
    assertThat(new RPackageVersion("0.2-0"), is(equalTo(new RPackageVersion("0.2-0"))));
    assertThat(new RPackageVersion("0.2-2"), is(equalTo(new RPackageVersion("0.2-2"))));
    assertThat(new RPackageVersion("1.0-0"), is(equalTo(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("1.2-0"), is(equalTo(new RPackageVersion("1.2-0"))));
    assertThat(new RPackageVersion("10.0-0"), is(equalTo(new RPackageVersion("10.0-0"))));
    assertThat(new RPackageVersion("9.9999-9999"), is(equalTo(new RPackageVersion("9.9999-9999"))));

    // comparing dot versions to dot-dash versions
    assertThat(new RPackageVersion("0.0.2"), is(equalTo(new RPackageVersion("0.0-2"))));
    assertThat(new RPackageVersion("0.2.0"), is(equalTo(new RPackageVersion("0.2-0"))));
    assertThat(new RPackageVersion("0.2.2"), is(equalTo(new RPackageVersion("0.2-2"))));
    assertThat(new RPackageVersion("1.0.0"), is(equalTo(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("1.2.0"), is(equalTo(new RPackageVersion("1.2-0"))));
    assertThat(new RPackageVersion("10.0.0"), is(equalTo(new RPackageVersion("10.0-0"))));
    assertThat(new RPackageVersion("9.9999.9999"), is(equalTo(new RPackageVersion("9.9999-9999"))));

    // comparing dot-dash versions to dot versions
    assertThat(new RPackageVersion("0.0-2"), is(equalTo(new RPackageVersion("0.0.2"))));
    assertThat(new RPackageVersion("0.2-0"), is(equalTo(new RPackageVersion("0.2.0"))));
    assertThat(new RPackageVersion("0.2-2"), is(equalTo(new RPackageVersion("0.2.2"))));
    assertThat(new RPackageVersion("1.0-0"), is(equalTo(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("1.2-0"), is(equalTo(new RPackageVersion("1.2.0"))));
    assertThat(new RPackageVersion("10.0-0"), is(equalTo(new RPackageVersion("10.0.0"))));
    assertThat(new RPackageVersion("9.9999-9999"), is(equalTo(new RPackageVersion("9.9999.9999"))));
  }

  @Test
  public void testComparingNotEqualVersions() {
    // comparing dot versions to dot versions
    assertThat(new RPackageVersion("0.0.2"), is(not(equalTo(new RPackageVersion("0.0.1")))));
    assertThat(new RPackageVersion("0.2.0"), is(not(equalTo(new RPackageVersion("0.1.0")))));
    assertThat(new RPackageVersion("0.2.2"), is(not(equalTo(new RPackageVersion("0.2.1")))));
    assertThat(new RPackageVersion("1.0.0"), is(not(equalTo(new RPackageVersion("1.0.1")))));
    assertThat(new RPackageVersion("1.2.0"), is(not(equalTo(new RPackageVersion("1.0.0")))));
    assertThat(new RPackageVersion("10.0.0"), is(not(equalTo(new RPackageVersion("10.0.1")))));
    assertThat(new RPackageVersion("9.9999.9999"), is(not(equalTo(new RPackageVersion("9.9999.9998")))));

    // comparing dot-dash versions to dot-dash versions
    assertThat(new RPackageVersion("0.0-2"), is(not(equalTo(new RPackageVersion("0.0-1")))));
    assertThat(new RPackageVersion("0.2-0"), is(not(equalTo(new RPackageVersion("0.1-0")))));
    assertThat(new RPackageVersion("0.2-2"), is(not(equalTo(new RPackageVersion("0.2-1")))));
    assertThat(new RPackageVersion("1.0-0"), is(not(equalTo(new RPackageVersion("1.0-1")))));
    assertThat(new RPackageVersion("1.2-0"), is(not(equalTo(new RPackageVersion("1.0-0")))));
    assertThat(new RPackageVersion("10.0-0"), is(not(equalTo(new RPackageVersion("10.0-1")))));
    assertThat(new RPackageVersion("9.9999-9999"), is(not(equalTo(new RPackageVersion("9.9999-9998")))));

    // comparing dot versions to dot-dash versions
    assertThat(new RPackageVersion("0.0.2"), is(not(equalTo(new RPackageVersion("0.0-1")))));
    assertThat(new RPackageVersion("0.2.0"), is(not(equalTo(new RPackageVersion("0.1-0")))));
    assertThat(new RPackageVersion("0.2.2"), is(not(equalTo(new RPackageVersion("0.2-1")))));
    assertThat(new RPackageVersion("1.0.0"), is(not(equalTo(new RPackageVersion("1.0-1")))));
    assertThat(new RPackageVersion("1.2.0"), is(not(equalTo(new RPackageVersion("1.0-0")))));
    assertThat(new RPackageVersion("10.0.0"), is(not(equalTo(new RPackageVersion("10.0-1")))));
    assertThat(new RPackageVersion("9.9999.9999"), is(not(equalTo(new RPackageVersion("9.9999-9998")))));

    // comparing dot-dash versions to dot versions
    assertThat(new RPackageVersion("0.0-2"), is(not(equalTo(new RPackageVersion("0.0.1")))));
    assertThat(new RPackageVersion("0.2-0"), is(not(equalTo(new RPackageVersion("0.1.0")))));
    assertThat(new RPackageVersion("0.2-2"), is(not(equalTo(new RPackageVersion("0.2.1")))));
    assertThat(new RPackageVersion("1.0-0"), is(not(equalTo(new RPackageVersion("1.0.1")))));
    assertThat(new RPackageVersion("1.2-0"), is(not(equalTo(new RPackageVersion("1.0.0")))));
    assertThat(new RPackageVersion("10.0-0"), is(not(equalTo(new RPackageVersion("10.0.1")))));
    assertThat(new RPackageVersion("9.9999-9999"), is(not(equalTo(new RPackageVersion("9.9999.9998")))));
  }

  @Test
  public void testComparingLowerVersions() {
    // comparing dot versions to dot versions
    assertThat(new RPackageVersion("0.0.1"), is(lessThan(new RPackageVersion("0.0.2"))));
    assertThat(new RPackageVersion("0.1.0"), is(lessThan(new RPackageVersion("0.2.0"))));
    assertThat(new RPackageVersion("0.1.1"), is(lessThan(new RPackageVersion("0.2.0"))));
    assertThat(new RPackageVersion("0.0.1"), is(lessThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("0.1.0"), is(lessThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("1.1.0"), is(lessThan(new RPackageVersion("1.2.0"))));
    assertThat(new RPackageVersion("1.1.9"), is(lessThan(new RPackageVersion("1.2.0"))));
    assertThat(new RPackageVersion("1.0.0"), is(lessThan(new RPackageVersion("2.0.0"))));
    assertThat(new RPackageVersion("1.1.0"), is(lessThan(new RPackageVersion("2.0.0"))));
    assertThat(new RPackageVersion("1.9.9"), is(lessThan(new RPackageVersion("2.0.0"))));
    assertThat(new RPackageVersion("1.0.0"), is(lessThan(new RPackageVersion("10.0.0"))));
    assertThat(new RPackageVersion("9.9.9999"), is(lessThan(new RPackageVersion("10.0.0"))));
    assertThat(new RPackageVersion("9.9999.9999"), is(lessThan(new RPackageVersion("10.0.0"))));

    // comparing dot-dash versions to dot-dash versions
    assertThat(new RPackageVersion("0.0-1"), is(lessThan(new RPackageVersion("0.0-2"))));
    assertThat(new RPackageVersion("0.1-0"), is(lessThan(new RPackageVersion("0.2-0"))));
    assertThat(new RPackageVersion("0.1-1"), is(lessThan(new RPackageVersion("0.2-0"))));
    assertThat(new RPackageVersion("0.0-1"), is(lessThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("0.1-0"), is(lessThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("1.1-0"), is(lessThan(new RPackageVersion("1.2-0"))));
    assertThat(new RPackageVersion("1.1-9"), is(lessThan(new RPackageVersion("1.2-0"))));
    assertThat(new RPackageVersion("1.0-0"), is(lessThan(new RPackageVersion("2.0-0"))));
    assertThat(new RPackageVersion("1.1-0"), is(lessThan(new RPackageVersion("2.0-0"))));
    assertThat(new RPackageVersion("1.9-9"), is(lessThan(new RPackageVersion("2.0-0"))));
    assertThat(new RPackageVersion("1.0-0"), is(lessThan(new RPackageVersion("10.0-0"))));
    assertThat(new RPackageVersion("9.9-9999"), is(lessThan(new RPackageVersion("10.0-0"))));
    assertThat(new RPackageVersion("9.9999-9999"), is(lessThan(new RPackageVersion("10.0-0"))));

    // comparing dot versions to dot-dash versions
    assertThat(new RPackageVersion("0.0.1"), is(lessThan(new RPackageVersion("0.0-2"))));
    assertThat(new RPackageVersion("0.1.0"), is(lessThan(new RPackageVersion("0.2-0"))));
    assertThat(new RPackageVersion("0.1.1"), is(lessThan(new RPackageVersion("0.2-0"))));
    assertThat(new RPackageVersion("0.0.1"), is(lessThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("0.1.0"), is(lessThan(new RPackageVersion("1.0-0"))));
    assertThat(new RPackageVersion("1.1.0"), is(lessThan(new RPackageVersion("1.2-0"))));
    assertThat(new RPackageVersion("1.1.9"), is(lessThan(new RPackageVersion("1.2-0"))));
    assertThat(new RPackageVersion("1.0.0"), is(lessThan(new RPackageVersion("2.0-0"))));
    assertThat(new RPackageVersion("1.1.0"), is(lessThan(new RPackageVersion("2.0-0"))));
    assertThat(new RPackageVersion("1.9.9"), is(lessThan(new RPackageVersion("2.0-0"))));
    assertThat(new RPackageVersion("1.0.0"), is(lessThan(new RPackageVersion("10.0-0"))));
    assertThat(new RPackageVersion("9.9.9999"), is(lessThan(new RPackageVersion("10.0-0"))));
    assertThat(new RPackageVersion("9.9999.9999"), is(lessThan(new RPackageVersion("10.0-0"))));

    // comparing dot-dash versions to dot versions
    assertThat(new RPackageVersion("0.0-1"), is(lessThan(new RPackageVersion("0.0.2"))));
    assertThat(new RPackageVersion("0.1-0"), is(lessThan(new RPackageVersion("0.2.0"))));
    assertThat(new RPackageVersion("0.1-1"), is(lessThan(new RPackageVersion("0.2.0"))));
    assertThat(new RPackageVersion("0.0-1"), is(lessThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("0.1-0"), is(lessThan(new RPackageVersion("1.0.0"))));
    assertThat(new RPackageVersion("1.1-0"), is(lessThan(new RPackageVersion("1.2.0"))));
    assertThat(new RPackageVersion("1.1-9"), is(lessThan(new RPackageVersion("1.2.0"))));
    assertThat(new RPackageVersion("1.0-0"), is(lessThan(new RPackageVersion("2.0.0"))));
    assertThat(new RPackageVersion("1.1-0"), is(lessThan(new RPackageVersion("2.0.0"))));
    assertThat(new RPackageVersion("1.9-9"), is(lessThan(new RPackageVersion("2.0.0"))));
    assertThat(new RPackageVersion("1.0-0"), is(lessThan(new RPackageVersion("10.0.0"))));
    assertThat(new RPackageVersion("9.9-9999"), is(lessThan(new RPackageVersion("10.0.0"))));
    assertThat(new RPackageVersion("9.9999-9999"), is(lessThan(new RPackageVersion("10.0.0"))));
  }
}
