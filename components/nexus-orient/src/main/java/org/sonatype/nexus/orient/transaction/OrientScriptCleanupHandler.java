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
package org.sonatype.nexus.orient.transaction;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.script.ScriptCleanupHandler;

import com.orientechnologies.orient.core.db.ODatabase.STATUS;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;

/**
 * Orient handler that cleans up open transactions
 *
 * @since 3.17
 */
@Named
@Singleton
public class OrientScriptCleanupHandler
    extends ComponentSupport
    implements ScriptCleanupHandler
{
  @Override
  public void cleanup(final String context) {
    ODatabaseDocument database = ODatabaseRecordThreadLocal.instance().getIfDefined();
    if (database != null && database.getStatus() == STATUS.OPEN) {
      database.close();
      log.warn("{} left a database connection open. Any opened connections should also be explicitly closed.", context);
    }
  }
}
