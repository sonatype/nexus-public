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
package org.sonatype.nexus.repository.storage;

import java.util.concurrent.Callable;

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;

/**
 * Utility class for working with different databases in the same thread. If you can find a way to avoid using this,
 * you probably should.
 *
 * @since 3.1
 */
public final class DatabaseThreadUtils
{
  /**
   * Utility function for working around "ODatabaseException: Database instance is not set in current thread" issues.
   * The current database ThreadLocal is preserved and restored after calling the lambda.
   */
  public static <T> T withOtherDatabase(Callable<T> function) {
    final ODatabaseDocumentInternal db = ODatabaseRecordThreadLocal.INSTANCE.getIfDefined();
    try {
      return function.call();
    }
    catch (Exception e) {
      Throwables.propagate(e);
    }
    finally {
      ODatabaseRecordThreadLocal.INSTANCE.set(db);
    }
    throw new IllegalStateException(); // shouldn't happen
  }

  private DatabaseThreadUtils() {
    // empty
  }
}
