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
package org.sonatype.nexus.cleanup.internal.storage.orient;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OrientCleanupPolicyEntityAdapterTest
    extends TestSupport
{
  private OrientCleanupPolicyEntityAdapter underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new OrientCleanupPolicyEntityAdapter();
  }

  @Test
  public void writingItemCanBeRead() throws Exception {
    OrientCleanupPolicy item = createCleanupItem();
    OrientCleanupPolicy actual = storeAndReadBack(item);

    assertThat(actual.getName(), is("name"));
    assertThat(actual.getNotes(), is("notes"));
    assertThat(actual.getFormat(), is("TestFormat"));
    assertThat(actual.getMode(), is("TestMode"));
  }

  private OrientCleanupPolicy storeAndReadBack(final OrientCleanupPolicy item) {
    ODocument doc = writeItem(item);
    return readItem(doc);
  }

  private OrientCleanupPolicy readItem(final ODocument doc) {
    OrientCleanupPolicy actual = new OrientCleanupPolicy();
    underTest.readFields(doc, actual);
    return actual;
  }

  private ODocument writeItem(final OrientCleanupPolicy item) {
    ODocument doc = new ODocument();
    underTest.writeFields(doc, item);
    return doc;
  }

  private OrientCleanupPolicy createCleanupItem() {
    return new OrientCleanupPolicy("name", "notes", "TestFormat", "TestMode",
          Maps.newHashMap());
  }
}
