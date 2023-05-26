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
package org.sonatype.nexus.internal.capability.upgrades;

import java.util.Map;
import java.util.stream.StreamSupport;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.internal.capability.upgrade.SettingsCapabilityUpgrade_1_1;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.internal.capability.upgrade.SettingsCapabilityUpgrade_1_1.DEFAULT_LONG_REQUEST_TIMEOUT;
import static org.sonatype.nexus.internal.capability.upgrade.SettingsCapabilityUpgrade_1_1.DEFAULT_REQUEST_TIMEOUT;

public class SettingsCapabilityUpgrade_1_1_Test
    extends TestSupport
{
  private static final String DEFAULT_SESSION_TIMEOUT = "30";
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("capability")
      .build();

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config");

  SettingsCapabilityUpgrade_1_1 underTest;

  @Before
  public void setup() {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      schema.createClass(DB_CLASS);
    }
    underTest = new SettingsCapabilityUpgrade_1_1(configDatabase.getInstanceProvider());
  }

  @Test
  public void applyIfPropertiesMissing() throws Exception {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      ODocument capability = db.newInstance(DB_CLASS);

      Map<String, Object> properties = ImmutableMap.of("sessionTimeout", DEFAULT_SESSION_TIMEOUT);

      capability.field("type", "rapture.settings");
      capability.field("properties", properties);
      capability.save();
    }

    underTest.apply();

    ODocument updatedCapability = findCapability();
    Map<String, Object> updatedProperties = updatedCapability.field("properties");
    assertThat(updatedProperties, notNullValue());
    assertThat(updatedProperties.get("sessionTimeout"), is(DEFAULT_SESSION_TIMEOUT));
    assertThat(updatedProperties.get("requestTimeout"), is(DEFAULT_REQUEST_TIMEOUT));
    assertThat(updatedProperties.get("longRequestTimeout"), is(DEFAULT_LONG_REQUEST_TIMEOUT));
  }

  @Test
  public void applyIfPropertiesExists() throws Exception {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      ODocument capability = db.newInstance(DB_CLASS);

      Map<String, Object> properties = ImmutableMap.of("sessionTimeout", DEFAULT_SESSION_TIMEOUT,
          "requestTimeout", "90", "longRequestTimeout", "360");

      capability.field("type", "rapture.settings");
      capability.field("properties", properties);
      capability.save();
    }

    underTest.apply();

    ODocument updatedCapability = findCapability();
    Map<String, Object> updatedProperties = updatedCapability.field("properties");
    assertThat(updatedProperties, notNullValue());
    assertThat(updatedProperties.get("sessionTimeout"), is(DEFAULT_SESSION_TIMEOUT));
    assertThat(updatedProperties.get("requestTimeout"), is("90"));
    assertThat(updatedProperties.get("longRequestTimeout"), is("360"));
  }

  private ODocument findCapability() {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      return StreamSupport.stream(db.browseClass(DB_CLASS).spliterator(), false).findFirst().get();
    }
  }
}
