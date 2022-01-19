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
package org.sonatype.nexus.repository.content.search;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.browse.store.example.TestSearchDAO;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;

import org.junit.Test;

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Test {@link SearchDAO}.
 */
public class SearchDAOTest
    extends ExampleContentTestSupport
{
  public SearchDAOTest() {
    super(TestSearchDAO.class);
  }

  /**
   * This test exists to ensure the stubbed DAO doesn't break or cause exceptions
   */
  @Test
  public void testEmptyCreateSchema() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(TestSearchDAO.class);
    }
  }
}
