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
package org.sonatype.nexus.repository.npm.internal

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import org.sonatype.nexus.common.collect.NestedAttributesMap

import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.IsNot.not
import static org.hamcrest.core.IsNull.nullValue

/**
 * Tests {@link NpmJsonUtils}
 */
class NpmJsonUtilsTest
    extends Specification
{

  def 'normal utf-8 parsing'() {
    when: 'the string in the input is encoded using UTF-8'
      def parsed = parseWithNpmJsonUtils('{"name":"foo"}', StandardCharsets.UTF_8)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def 'parsing a fairly harmless iso-8859-1 string falls back to iso-8859-1'() {
    when: 'a fairly harmless string in the input is encoded using ISO-8859-1'
      def parsed = parseWithNpmJsonUtils('{"name":"foo"}', StandardCharsets.ISO_8859_1)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def 'parsing should fall back to iso-8859-1 when the bytes have an invalid middle byte in UTF-8'() {
    when: 'a string in the input is encoded using ISO-8859-1, but will have an invalid middle byte in UTF-8'
      def parsed = parseWithNpmJsonUtils('{"name":"foo","author":"bé"}', StandardCharsets.ISO_8859_1)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def 'parsing should fall back to iso-8859-1 when the bytes have an invalid start byte in UTF-8'() {
    when: 'a string in the input is encoded using ISO-8859-1, but will have an invalid start byte in UTF-8'
      def parsed = parseWithNpmJsonUtils('{"name":"foo","author":"¿foo?"}', StandardCharsets.ISO_8859_1)

    then: 'everything should go fine'
      assertThat(parsed, not(nullValue()))
  }

  def parseWithNpmJsonUtils(String toEncode, Charset charset) {
    NpmJsonUtils.parse(
        NpmJsonUtils.supplier(toEncode.getBytes(charset))
    )
  }

}
