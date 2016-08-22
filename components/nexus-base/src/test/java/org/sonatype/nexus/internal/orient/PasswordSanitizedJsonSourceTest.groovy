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
package org.sonatype.nexus.internal.orient

import javax.inject.Provider

import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type
import org.sonatype.nexus.orient.DatabaseExternalizer
import org.sonatype.nexus.orient.DatabaseInstance

import org.junit.rules.TemporaryFolder
import org.junit.Rule
import spock.lang.Specification

/**
 * Tests for {@link PasswordSanitizedJsonSource}.
 */
class PasswordSanitizedJsonSourceTest
    extends Specification
{
  @Rule
  TemporaryFolder temporaryFolder = new TemporaryFolder()

  def 'Excludes sensitive classes from the export.'() {
    given: 'a PasswordSanitizedJsonSource'
      DatabaseInstance instance = Mock(DatabaseInstance)
      DatabaseExternalizer externalizer = Mock(DatabaseExternalizer)
      Provider<DatabaseInstance> instanceProvider = Mock(Provider)
      instanceProvider.get() >> instance
      instance.externalizer() >> externalizer
      PasswordSanitizedJsonSource passwordSanitizedJsonSource = new PasswordSanitizedJsonSource(Type.CONFIG,
          'path', instanceProvider)
      File exportJsonFile = temporaryFolder.newFile('export.json')

    when: 'a database is exported'
      passwordSanitizedJsonSource.generate(exportJsonFile)

    then: 'the sensitive class names are passed to the externalizer to be excluded'
      1 * externalizer.export(_, _) >> { output, excludedClasses ->
        assert excludedClasses == ['api_key', 'usertoken_record'] as Set
        output.write('{"records": []}'.bytes)
      }
  }

}
