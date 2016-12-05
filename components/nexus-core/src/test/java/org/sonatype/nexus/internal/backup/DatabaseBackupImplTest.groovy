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
package org.sonatype.nexus.internal.backup

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.orient.DatabaseManager
import org.sonatype.nexus.orient.DatabaseServer

import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.is

/**
 * Tests for {@link DatabaseBackupImpl}
 *
 */
class DatabaseBackupImplTest
    extends Specification
{

  def 'checks that a file can be accessed'() {
    def databaseManager = Mock(DatabaseManager)
    def databaseServer = Mock(DatabaseServer)
    def applicationDirectories = Mock(ApplicationDirectories)
    def databaseBackup = new DatabaseBackupImpl(databaseServer, databaseManager, 9, 1024, applicationDirectories)

    when: 'using a temp folder and a temp file'
      File temp = databaseBackup.checkTarget(System.getProperty("java.io.tmpdir"), "test")

    then: 'the file should be intact'
      assertThat(temp, notNullValue())
      assertThat(temp.delete(), is(true))
      1 * applicationDirectories.getWorkDirectory(_) >> { String name -> new File(name) }
  }
}
