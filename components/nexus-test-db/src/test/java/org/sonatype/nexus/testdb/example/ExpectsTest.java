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
package org.sonatype.nexus.testdb.example;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.Test;

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Test the {@link Expects} annotation, used to declare access types to be registered first.
 */
public class ExpectsTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(TestExpectsDAO.class);

  @SuppressWarnings("java:S2699") // sonar expects assertions, but best to let this exception bubble up
  @Test
  public void testExpectedAccessTypesRegisteredFirst() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      // will fail if @Expects is not respected
      session.access(TestItemDAO.class);
      session.getTransaction().commit();
    }
  }
}
