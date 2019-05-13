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
package org.sonatype.nexus.orient.transaction

import com.orientechnologies.orient.core.db.ODatabase.STATUS
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import spock.lang.Specification
import spock.lang.Unroll

class OrientScriptCleanupHandlerTest
    extends Specification
{

  @Unroll
  def 'it will close connections when an open connection exists after script execution'() {
    given: 'the handler and a database document'
      def handler = new OrientScriptCleanupHandler()
      def databaseDocument = Mock(ODatabaseDocumentInternal)
      databaseDocument.getStatus() >> status
      ODatabaseRecordThreadLocal.instance().set(databaseDocument)
    when: 'cleanup executes'
      handler.cleanup()
    then: 'close may be called'
      callClose * databaseDocument.close()

    where:
      status        | callClose
      STATUS.OPEN   | 1
      STATUS.CLOSED | 0
  }

  def 'it will do nothing if no open connection exists'() {
    given: 'the handler and no instance value'
      def handler = new OrientScriptCleanupHandler()
      ODatabaseRecordThreadLocal.instance().set(null)
    when: 'cleanup executes'
      handler.cleanup()
    then: 'nothing happens'
      noExceptionThrown()
  }
}
