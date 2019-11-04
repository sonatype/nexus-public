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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.testdb.example.template.metal.MetalSprocketDAO;
import org.sonatype.nexus.testdb.example.template.plastic.PlasticSprocketDAO;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.common.text.Strings2.lower;

/**
 * Test the {@link SchemaTemplate} annotation.
 */
public class SchemaTemplateTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule()
      .access(PlasticSprocketDAO.class)
      .access(MetalSprocketDAO.class);

  @Test
  public void testExpectedAccessTypesRegisteredFirst() throws SQLException {
    try (DataSession<?> session = sessionRule.openSession("config")) {
      // will fail if @Expects and @SchemaTemplate are not respected
      session.access(MetalSprocketDAO.class);
      session.access(PlasticSprocketDAO.class).extendSchema();
      session.getTransaction().commit();
    }

    // check the extra column added by plastic_sprocket exists
    assertColumns("metal_sprocket", containsInAnyOrder("id", "spindle_id", "widget_id"));
    assertColumns("plastic_sprocket", containsInAnyOrder("id", "spindle_id", "widget_id", "notes"));
  }

  private void assertColumns(final String tableName, final Matcher<Iterable<?>> matcher) throws SQLException {
    List<String> columnNames = new ArrayList<>();
    try (Connection connection = sessionRule.openConnection("config");
        ResultSet rs = connection.getMetaData().getColumns(null, null, "%", null)) {

      while (rs.next()) {
        if (tableName.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
          columnNames.add(lower(rs.getString("COLUMN_NAME")));
        }
      }
    }
    assertThat(columnNames, matcher);
  }
}
