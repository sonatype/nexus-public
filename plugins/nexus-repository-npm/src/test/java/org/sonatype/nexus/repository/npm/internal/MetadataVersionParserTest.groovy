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


import spock.lang.Specification
import spock.lang.Unroll

class MetadataVersionParserTest
    extends Specification
{
  @Unroll
  def "It will extract all version numbers from the blob"() {
    given:
      def inputStream = new ByteArrayInputStream(json.bytes)
    when:
      def versions = MetadataVersionParser.readVersions(inputStream)
    then:
      versions == expectedVersions
    where:
      json                                                                 | expectedVersions
      '{"versions": {"1.1.1": {}, "1.2.1": {}, "1.3.1": {}}}'              | ['1.1.1', '1.2.1', '1.3.1']
      '{"id": "foo", "versions": {"1.1.1": {}}}'                           | ['1.1.1']
      '{"id": "foo", "versions": {"1.1.1": {}}, "other": "bar"}'           | ['1.1.1']
      '{"id": "foo", "versions": {"1.1.1": {}}, "other": {"bar": "baz"}}}' | ['1.1.1']
      '{"id": "foo", "versions": {}}'                                      | []
      '{"id": "foo", "versions": null}'                                    | []
      '{"id": "foo"}'                                                      | []
  }
}
