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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.upgrade.Upgrade;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;

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

  /**
   * Runs a {@link BiConsumer} of {@link ODatabaseDocumentTx} and {@link OClass}
   * if the specified {@code className} exists.
   */
  protected static void withDatabaseAndClass(final Provider<DatabaseInstance> databaseInstance, final String className,
                                             final BiConsumer<ODatabaseDocumentTx, OClass> consumer)
  {
    withDatabase(databaseInstance, db -> {
      if (db.getMetadata().getSchema().existsClass(className)) {
        consumer.accept(db, db.getMetadata().getSchema().getClass(className));
      }
    });
  }

  /**
   * Runs a {@link Consumer} with a {@link ODatabaseDocumentTx}
   */
  protected static void withDatabase(final Provider<DatabaseInstance> databaseInstance,
                                     final Consumer<ODatabaseDocumentTx> consumer)
  {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      consumer.accept(db);
    }
  }
}
