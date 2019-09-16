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
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * Test the {@link DataSessionRule}.
 */
public class DataSessionRuleTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(TestItemDAO.class);

  @Test
  public void testCrudOperations() {
    try (DataSession<?> session = sessionRule.openSession("config")) {
      TestItemDAO dao = session.access(TestItemDAO.class);

      assertThat(dao.browse(), emptyIterable());

      TestItem itemA = new TestItem();
      itemA.setId(EntityId.of(randomUUID().toString()));
      itemA.setVersion(1);
      itemA.setEnabled(true);
      itemA.setNotes("test-entity");
      itemA.setProperties(ImmutableMap.of("sample", "data"));

      dao.create(itemA);

      assertThat(dao.read(itemA.getId()).get(), is(itemA));

      assertThat(dao.browse(), contains(itemA));

      TestItem itemB = new TestItem();
      itemB.setId(EntityId.of(randomUUID().toString()));
      itemB.setVersion(2);
      itemB.setEnabled(false);
      itemB.setNotes("test-entity");
      itemB.setProperties(ImmutableMap.of());

      dao.create(itemB);

      assertThat(dao.read(itemB.getId()).get(), is(itemB));

      assertThat(dao.browse(), contains(itemA, itemB));

      assertFalse(dao.read(EntityId.of("missing")).isPresent());

      dao.delete(itemA.getId());

      assertThat(dao.browse(), contains(itemB));

      dao.delete(itemB.getId());

      assertThat(dao.browse(), emptyIterable());
    }
  }
}
