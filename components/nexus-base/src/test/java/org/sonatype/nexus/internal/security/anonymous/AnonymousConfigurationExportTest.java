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
package org.sonatype.nexus.internal.security.anonymous;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization AnonymousConfigurationData by {@link AnonymousConfigurationExport}
 */
public class AnonymousConfigurationExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File binFile;

  @Before
  public void setup() throws IOException {
    binFile = File.createTempFile("AnonymousConfiguration", ".json");
  }

  @After
  public void tearDown() {
    binFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    AnonymousConfiguration configuration = buildAnonymousConfiguration();
    AnonymousConfigurationStore configurationStore = mock(AnonymousConfigurationStore.class);
    when(configurationStore.load()).thenReturn(configuration);

    AnonymousConfigurationExport exporter = new AnonymousConfigurationExport(configurationStore);
    exporter.export(binFile);
    Optional<AnonymousConfigurationData> importedItem =
        jsonExporter.importObjectFromJson(binFile, AnonymousConfigurationData.class);
    assertTrue(importedItem.isPresent());
    assertThat(importedItem.get(), is(configuration));
  }

  private AnonymousConfiguration buildAnonymousConfiguration() {
    AnonymousConfiguration configuration = new AnonymousConfigurationData();
    configuration.setUserId("userId");
    configuration.setRealmName("realName");
    configuration.setEnabled(true);

    return configuration;
  }
}
