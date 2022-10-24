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
package org.sonatype.nexus.datastore.mybatis;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.datastore.mybatis.PlaceholderTypes.configurePlaceholderTypes;

/**
 * Test {@link PlaceholderTypes}.
 */
public class PlaceholderTypesTest
    extends TestSupport
{
  @Test
  public void hasBuiltInDefaultsForH2() {
    Configuration config = new Configuration();
    config.setDatabaseId("H2");

    configurePlaceholderTypes(config);

    assertThat(config.getVariables().get("UUID_TYPE"), is("UUID"));
    assertThat(config.getVariables().get("JSON_TYPE"), is("JSON"));
    assertThat(config.getVariables().get("BINARY_TYPE"), is("BYTEA"));
  }

  @Test
  public void hasBuiltInDefaultsForPostgreSQL() {
    Configuration config = new Configuration();
    config.setDatabaseId("PostgreSQL");

    configurePlaceholderTypes(config);

    assertThat(config.getVariables().get("UUID_TYPE"), is("UUID"));
    assertThat(config.getVariables().get("JSON_TYPE"), is("JSONB"));
    assertThat(config.getVariables().get("BINARY_TYPE"), is("BYTEA"));
  }

  @Test
  public void canSupplyTypesForOtherDatabases() {
    Configuration config = new Configuration();
    config.setDatabaseId("MyDB");

    config.getVariables().setProperty("UUID_TYPE.MyDB", "CHARACTER VARYING (36)");
    config.getVariables().setProperty("JSON_TYPE.MyDB", "CLOB");
    config.getVariables().setProperty("BINARY_TYPE.MyDB", "BLOB");

    configurePlaceholderTypes(config);

    assertThat(config.getVariables().get("UUID_TYPE"), is("CHARACTER VARYING (36)"));
    assertThat(config.getVariables().get("JSON_TYPE"), is("CLOB"));
    assertThat(config.getVariables().get("BINARY_TYPE"), is("BLOB"));
  }

  @Test
  public void failOnMissingType() {
    Configuration config = new Configuration();
    config.setDatabaseId("MyDB");
    try {
      configurePlaceholderTypes(config);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("No database type configured for UUID_TYPE.MyDB"));
    }
  }

  @Test
  public void failOnInvalidTypes() {
    Configuration config = new Configuration();
    config.setDatabaseId("MyDB");

    assertTypeIsInvalid(config, "1");
    assertTypeIsInvalid(config, "A1");
    assertTypeIsInvalid(config, "AA ");
    assertTypeIsInvalid(config, "AA 2");
    assertTypeIsInvalid(config, "AA B2");
    assertTypeIsInvalid(config, "AA BB ");
    assertTypeIsInvalid(config, "AA BB 3");
    assertTypeIsInvalid(config, "AA BB C");
    assertTypeIsInvalid(config, "A(");
    assertTypeIsInvalid(config, "A)");
    assertTypeIsInvalid(config, "A()");
    assertTypeIsInvalid(config, "A(B)");
    assertTypeIsInvalid(config, "A(1");
    assertTypeIsInvalid(config, "A(1(");
    assertTypeIsInvalid(config, "A  (1)");
    assertTypeIsInvalid(config, "A B(");
    assertTypeIsInvalid(config, "A B)");
    assertTypeIsInvalid(config, "A B()");
    assertTypeIsInvalid(config, "A B(C)");
    assertTypeIsInvalid(config, "A B(2");
    assertTypeIsInvalid(config, "A B(2(");
    assertTypeIsInvalid(config, "A B  (2)");

    assertTypeIsValid(config, "A");
    assertTypeIsValid(config, "AA BB");
    assertTypeIsValid(config, "A(1)");
    assertTypeIsValid(config, "A (1)");
    assertTypeIsValid(config, "A B(2)");
    assertTypeIsValid(config, "A B (2)");
  }

  private void assertTypeIsInvalid(Configuration config, String type) {
    try {
      config.getVariables().setProperty("UUID_TYPE.MyDB", type);
      configurePlaceholderTypes(config);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("Invalid database type " + type + " configured for UUID_TYPE.MyDB"));
    }
  }

  private void assertTypeIsValid(Configuration config, String type) {
    config.getVariables().setProperty("UUID_TYPE.MyDB", type);
    config.getVariables().setProperty("JSON_TYPE.MyDB", type);
    config.getVariables().setProperty("BINARY_TYPE.MyDB", type);
    configurePlaceholderTypes(config);
  }
}
