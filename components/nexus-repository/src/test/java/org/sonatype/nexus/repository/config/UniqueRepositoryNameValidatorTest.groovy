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
package org.sonatype.nexus.repository.config

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager

import spock.lang.Specification

/**
 * Tests validity of Repository names validated by {@link UniqueRepositoryNameValidator}
 * @since 3.0
 */
class UniqueRepositoryNameValidatorTest
    extends Specification
{
  RepositoryManager repositoryManager = Mock()
  
  UniqueRepositoryNameValidator validator = new UniqueRepositoryNameValidator(repositoryManager)

  def "Name is valid when it is unique case-insensitively"(String name, List<String> repositoryNames, boolean isValid) {
    when:
      def valid = validator.isValid(name, null)

    then:
      1 * repositoryManager.browse() >> repositoryNames.collect{ String repoName -> { -> repoName } as Repository}
      valid == isValid

    where:
      name  | repositoryNames | isValid
      'foo' | ['bar', 'baz']  | true
      'foo' | ['foobar']      | true
      'foo' | ['foo123']      | true
      'foo' | ['foo']         | false
      'foo' | ['Foo']         | false
  }
}
