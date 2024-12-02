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
package org.sonatype.nexus.common.text;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class UnitFormatterTest
    extends TestSupport
{
  @Test
  public void testFormatStorage_units() {
    assertThat(UnitFormatter.formatStorage(round(pow(10, 18))), is("1.00 EB"));
    assertThat(UnitFormatter.formatStorage(round(pow(10, 15))), is("1.00 PB"));
    assertThat(UnitFormatter.formatStorage(round(pow(10, 12))), is("1.00 TB"));
    assertThat(UnitFormatter.formatStorage(round(pow(10, 9))), is("1.00 GB"));
    assertThat(UnitFormatter.formatStorage(round(pow(10, 6))), is("1.00 MB"));
    assertThat(UnitFormatter.formatStorage(round(pow(10, 3))), is("1.00 KB"));
    assertThat(UnitFormatter.formatStorage(round(pow(10, 0))), is("1.00 B"));
    assertThat(UnitFormatter.formatStorage(0), is("0.00 B"));
    assertThat(UnitFormatter.formatStorage(-1 * round(pow(10, 18))), is("-1.00 EB"));
    assertThat(UnitFormatter.formatStorage(-1 * round(pow(10, 15))), is("-1.00 PB"));
    assertThat(UnitFormatter.formatStorage(-1 * round(pow(10, 12))), is("-1.00 TB"));
    assertThat(UnitFormatter.formatStorage(-1 * round(pow(10, 9))), is("-1.00 GB"));
    assertThat(UnitFormatter.formatStorage(-1 * round(pow(10, 6))), is("-1.00 MB"));
    assertThat(UnitFormatter.formatStorage(-1 * round(pow(10, 3))), is("-1.00 KB"));
    assertThat(UnitFormatter.formatStorage(-1 * round(pow(10, 0))), is("-1.00 B"));
  }

  @Test
  public void testFormatStorage_rounding() {
    assertThat(UnitFormatter.formatStorage(round(3.7 * pow(10, 18))), is("3.70 EB"));
    assertThat(UnitFormatter.formatStorage(round(3.4 * pow(10, 18))), is("3.40 EB"));
    assertThat(UnitFormatter.formatStorage(round(2.999 * pow(10, 18))), is("3.00 EB"));
    assertThat(UnitFormatter.formatStorage(round(2.444 * pow(10, 18))), is("2.44 EB"));
  }
}
