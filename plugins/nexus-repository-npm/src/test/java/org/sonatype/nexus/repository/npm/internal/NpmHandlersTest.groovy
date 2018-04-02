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
package org.sonatype.nexus.repository.npm.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.view.Parameters

import org.joda.time.DateTime
import org.junit.Test

/**
 * UT for {@link NpmHandlers}
 */
class NpmHandlersTest
    extends TestSupport
{
  NpmHandlers handlers = new NpmHandlers()

  @Test
  void 'Test incremental index request with null params'() {
    DateTime time = handlers.indexSince(null)
    assert time == null
  }

  @Test
  void 'Test incremental index request with no params'() {
    Parameters params = new Parameters()
    DateTime time = handlers.indexSince(params)
    assert time == null
  }

  @Test
  void 'Test incremental index request with valid params'() {
    final DateTime now = DateTime.now()
    Parameters params = new Parameters()
    params.set("stale", "update_after")
    params.set("startkey", String.valueOf(now.millis))
    DateTime time = handlers.indexSince(params)
    assert time == now
  }
}
