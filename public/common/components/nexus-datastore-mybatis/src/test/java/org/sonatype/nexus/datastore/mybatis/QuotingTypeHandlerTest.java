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
package org.sonatype.nexus.datastore.mybatis;

import org.sonatype.nexus.datastore.mybatis.handlers.QuotingTypeHandler;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class QuotingTypeHandlerTest
{
  @Test
  void testTsEscapeBasic() {
    assertThat(QuotingTypeHandler.tsEscape("asdf"), is("'asdf'"));
    assertThat(QuotingTypeHandler.tsEscape("as'df"), is("'as\\'df'"));
    assertThat(QuotingTypeHandler.tsEscape("as\\df"), is("'as\\\\df'"));
  }

  /*
   * NEXUS-54105 - The MyBatis ::tsvector handler must never forward a 0x00 byte
   * to the driver; PostgreSQL rejects it as an invalid UTF-8 byte sequence.
   */
  @Test
  void testTsEscapeStripsNulBytes() {
    assertThat(QuotingTypeHandler.tsEscape("term\u0000x"), is("'termx'"));
    assertThat(QuotingTypeHandler.tsEscape("\u0000leading"), is("'leading'"));
    assertThat(QuotingTypeHandler.tsEscape("trailing\u0000"), is("'trailing'"));
    assertThat(QuotingTypeHandler.tsEscape("\u0000"), is("''"));
  }
}
