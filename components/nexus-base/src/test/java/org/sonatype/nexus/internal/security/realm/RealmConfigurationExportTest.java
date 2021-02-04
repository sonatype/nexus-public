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
package org.sonatype.nexus.internal.security.realm;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link RealmConfiguration} by {@link RealmConfigurationExport}
 */
public class RealmConfigurationExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("RealmConfiguration", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    RealmConfiguration configuration = createRealmConfiguration();

    RealmConfigurationStoreImpl source = mock(RealmConfigurationStoreImpl.class);
    when(source.load()).thenReturn(configuration);

    RealmConfigurationExport exporter = new RealmConfigurationExport(source);
    exporter.export(jsonFile);
    Optional<RealmConfigurationData> importedData =
        jsonExporter.importObjectFromJson(jsonFile, RealmConfigurationData.class);

    assertTrue(importedData.isPresent());
    assertThat(importedData.get().getRealmNames(), containsInAnyOrder("MockRealmA", "MockRealmB"));
  }

  private RealmConfiguration createRealmConfiguration() {
    RealmConfigurationData configuration = new RealmConfigurationData();
    configuration.setRealmNames(ImmutableList.of("MockRealmA", "MockRealmB"));

    return configuration;
  }
}
