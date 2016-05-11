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
package org.sonatype.nexus.internal.app

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.app.KarafManager

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @since 3.1
 */
class KarafManagerImplTest
    extends Specification
{
  @Rule
  TemporaryFolder temporaryFolder = new TemporaryFolder()

  def applicationDirectories = Mock(ApplicationDirectories)

  def "When needed we can tell the karaf container to clear its cache on the next restart by writing a marker file"() {
    given:
      KarafManager karafManager = new KarafManagerImpl(applicationDirectories)
      def cacheCleanFile = new File(temporaryFolder.root, 'clean_cache')
      assert !cacheCleanFile.exists(): 'File should not exist prior to exercising manager'

    when: 'Instructing the manager to set up cache cleaning on restart'
      karafManager.setCleanCacheOnRestart()

    then: 'The file is created where expected'
      1 * applicationDirectories.workDirectory >> temporaryFolder.root
      cacheCleanFile.exists()
  }
}
