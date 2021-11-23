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
package org.sonatype.nexus.upgrade.internal.orient;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import com.google.common.collect.ImmutableMap;

import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.upgrade.internal.orient.OrientAnalyticsPermissionReset.ANALYTICS_CAPABILITY;
import static org.sonatype.nexus.upgrade.internal.orient.OrientAnalyticsPermissionReset.P_ENABLED;
import static org.sonatype.nexus.upgrade.internal.orient.OrientAnalyticsPermissionReset.P_PROPERTIES;
import static org.sonatype.nexus.upgrade.internal.orient.OrientAnalyticsPermissionReset.P_TYPE;
import static org.sonatype.nexus.upgrade.internal.orient.OrientAnalyticsPermissionReset.SUBMIT_ANALYTICS;

public class OrientAnalyticsPermissionResetTest
    extends TestSupport
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("capability")
      .build();

  @Rule
  public DatabaseInstanceRule config = DatabaseInstanceRule.inMemory("test_config");

  @Mock
  private ApplicationVersion applicationVersion;

  private OrientAnalyticsPermissionReset underTest;

  @Before
  public void setUp() {
    when(applicationVersion.getEdition()).thenReturn("OSS");
    try (ODatabaseDocumentTx db = config.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();

      OClass capability = schema.createClass(DB_CLASS);
      capability.createProperty(P_ENABLED, OType.BOOLEAN);
      capability.createProperty(P_PROPERTIES, OType.EMBEDDEDMAP);
      capability.createProperty(P_TYPE, OType.STRING);
    }

    underTest = new OrientAnalyticsPermissionReset(config.getInstanceProvider(), applicationVersion);
  }

  @Test
  public void shouldDeleteCapabilityWhenDisabled() {
    putAnalyticsCapability();
    assertThat(analyticsCapabilityPresent(), is(true));

    underTest.resetAnalyticsPermissionIfDisabled();
    assertThat(analyticsCapabilityPresent(), is(false));
  }

  @Test
  public void shouldDeleteCapabilityWhenEnabledAndSubmitAnalyticsIsFalse() {
    putAnalyticsCapability(true, false);
    assertThat(analyticsCapabilityPresent(), is(true));

    underTest.resetAnalyticsPermissionIfDisabled();
    assertThat(analyticsCapabilityPresent(), is(false));
  }

  @Test
  public void shouldNotDeleteCapabilityWhenEnabledAndSubmitAnalyticsIsTrue() {
    putAnalyticsCapability(true, true);
    assertThat(analyticsCapabilityPresent(), is(true));

    underTest.resetAnalyticsPermissionIfDisabled();
    assertThat(analyticsCapabilityPresent(), is(true));
  }

  @Test
  public void shouldDoNothingIfEditionIsNotOSS() {
    putAnalyticsCapability();
    assertThat(analyticsCapabilityPresent(), is(true));
    when(applicationVersion.getEdition()).thenReturn("PRO");

    underTest.resetAnalyticsPermissionIfDisabled();

    assertThat(analyticsCapabilityPresent(), is(true));
  }

  private void putAnalyticsCapability() {
    putAnalyticsCapability(false, false);
  }

  private void putAnalyticsCapability(final boolean enabled, final boolean submitAnalytics) {
    try (ODatabaseDocumentTx db = config.getInstance().connect()) {
      ODocument doc = db.newInstance(DB_CLASS);
      doc.field(P_TYPE, ANALYTICS_CAPABILITY);
      doc.field(P_ENABLED, String.valueOf(enabled));
      doc.field(P_PROPERTIES, ImmutableMap.of(SUBMIT_ANALYTICS, String.valueOf(submitAnalytics)));
      doc.save();
      db.commit();
    }
  }

  private boolean analyticsCapabilityPresent() {
    try (ODatabaseDocumentTx db = config.getInstance().connect()) {
      return stream(db.browseClass(DB_CLASS).spliterator(), false).count() == 1;
    }
  }
}
