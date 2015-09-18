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
package org.sonatype.nexus.rapture

import org.sonatype.goodies.testsupport.TestSupport

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.gson.GsonBuilder
import org.junit.Test

/**
 * Hashing trials.
 */
class HashingTrial
    extends TestSupport
{
  @Test
  void 'test gson hash'() {
    def gson = new GsonBuilder().create()
    def data = ['a', 'b', 'c']
    def hash = Hashing.sha1().hashString(gson.toJson(data), Charsets.UTF_8).toString();
    log hash
    assert hash == 'e13460afb1e68af030bb9bee8344c274494661fa'
  }

  @Test
  void 'test object hash'() {
    def data = ['a', 'b', 'c']
    log data.hashCode()
    data << 'd'
    log data.hashCode()
  }
}
