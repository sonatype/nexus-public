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
package org.sonatype.nexus.quartz.orient;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrientDbRule
    implements TestRule
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String connectionString;

  public OrientDbRule(final String connectionString) {
    this.connectionString = connectionString;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        run(base);
      }
    };
  }

  @SuppressWarnings("resource")
  protected void before() throws Exception {
    log.info("Creating database: {}", connectionString);
    try (ODatabaseDocumentTx db = new ODatabaseDocumentTx(connectionString).create()) {
      OrientQuartzSchema.register(db);
    }
  }

  @SuppressWarnings("resource")
  protected void after() throws Exception {
    log.info("Dropping database: {}", connectionString);
    new ODatabaseDocumentTx(connectionString).open("admin", "admin").drop();
  }

  private void run(final Statement base) throws Throwable {
    Throwable error = null;
    try {
      before();
      base.evaluate();
    }
    catch (Throwable t) {
      error = t;
      throw t;
    }
    finally {
      try {
        after();
      }
      catch (Throwable t) {
        if (error != null) {
          error.addSuppressed(t);
        }
        else {
          throw t;
        }
      }
    }
  }
}
