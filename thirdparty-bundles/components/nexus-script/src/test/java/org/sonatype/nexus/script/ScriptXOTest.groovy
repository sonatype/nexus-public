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
package org.sonatype.nexus.script

import javax.inject.Inject
import javax.validation.Validator

import org.sonatype.nexus.validation.ValidationModule
import org.sonatype.nexus.validation.constraint.NamePatternConstants

import spock.guice.UseModules
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Validation tests for {@link ScriptXO}
 *
 */
@UseModules([ValidationModule])
class ScriptXOTest
    extends Specification
{
  public static final String INVALID_NAME_MSG = NamePatternConstants.MESSAGE

  @Inject
  @Shared
  Validator validator

  // semi-random sample of invalid characters from Latin and other character sets
  static List INVALID_NAMES = (
      '#,* #\'\\/?<>| \r\n\t,+@&å©不βخ'.collect { it }
          + '_leadingUnderscore'
          + '.'
          + '..'
  ).asImmutable()

  static final List VALID_NAMES = ['Foo_1.2-3', 'foo.', '-0.', 'a', '1'].asImmutable()

  def "Name is always required"(String name) {
    when:
      def validations = validator.validate(new ScriptXO(name, 'content', 'type'))

    then:
      validations.size()
      validations.every { it.propertyPath.toString() == 'name' }

    where:
      name << [null, '']
  }

  def "Content is always required"(String content) {
    when:
      def validations = validator.validate(new ScriptXO('name', content, 'type'))

    then:
      validations.size() == 1
      validations[0].propertyPath.toString() == 'content'

    where:
      content << [null, '']
  }

  @Unroll
  def "The name: #name should not validate"(String name) {
    when:
      def validations = validator.validate(new ScriptXO(name, 'content', 'type'))

    then:
      validations.size() == 1
      validations[0].propertyPath.toString() == 'name'

    where:
      name << INVALID_NAMES
  }

  @Unroll
  def "The name: #name should be valid"(String name) {
    when:
      def validations = validator.validate(new ScriptXO(name, 'content', 'type'))

    then:
      validations.isEmpty()

    where:
      name << VALID_NAMES
  }
}
