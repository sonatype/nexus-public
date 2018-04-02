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

import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.repository.npm.internal.NpmGroupPackageHandler
import org.sonatype.nexus.repository.npm.internal.NpmPackageId
import spock.lang.Specification
import spock.lang.Unroll

class NpmGroupPackageHandlerTest
    extends Specification
{
  static final NpmPackageId SCOPED = NpmPackageId.parse('@scope/a')

  static final NpmPackageId A = NpmPackageId.parse('a')

  @Unroll
  def "system merge value: #systemPropertyValue , packages: #packages.size() with id: #id should be: #expectedResult"() {

    when:
      NpmGroupPackageHandler handler = new NpmGroupPackageHandler(systemPropertyValue)
      boolean shouldServeFirstResult = handler.shouldServeFirstResult(packages, id)

    then:
      shouldServeFirstResult == expectedResult

    where:
      systemPropertyValue | packages | id     | expectedResult
      true              | maps(1)  | A      | true
      true              | maps(2)  | A      | false
      true              | maps(1)  | SCOPED | true
      true              | maps(2)  | SCOPED | false
      false             | maps(1)  | A      | true
      false             | maps(2)  | A      | true
      false             | maps(2)  | SCOPED | false
  }

  /**
   * Generate a list with bunk content (strictly used for size comparison)
   */
  static List<NestedAttributesMap> maps(int number) {
    (0..<number).collect { new NestedAttributesMap('foo', [:]) }
  }
}
