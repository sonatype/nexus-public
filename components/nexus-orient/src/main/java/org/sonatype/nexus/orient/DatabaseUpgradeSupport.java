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
package org.sonatype.nexus.orient;

import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.upgrade.Upgrade;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/**
 * Support for upgrades of OrientDB databases.
 * 
 * @since 3.3
 */
public abstract class DatabaseUpgradeSupport
    extends ComponentSupport
    implements Upgrade
{
  /**
   * Returns {@code true} if the given schema class exists, otherwise {@code false}.
   */
  protected static boolean hasSchemaClass(final Provider<DatabaseInstance> databaseInstance, final String className) {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      return db.getMetadata().getSchema().existsClass(className);
    }
  }
}
