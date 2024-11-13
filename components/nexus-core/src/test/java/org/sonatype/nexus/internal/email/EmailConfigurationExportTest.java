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
package org.sonatype.nexus.internal.email;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link EmailConfiguration} by {@link EmailConfigurationExport}
 */
public class EmailConfigurationExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("EmailConfiguration", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    EmailConfiguration configuration = createEmailConfiguration();

    EmailConfigurationStore store = mock(EmailConfigurationStore.class);
    when(store.load()).thenReturn(configuration);

    EmailConfigurationExport exporter = new EmailConfigurationExport(store);
    exporter.export(jsonFile);
    Optional<EmailConfigurationData> importedDataOpt =
        jsonExporter.importObjectFromJson(jsonFile, EmailConfigurationData.class);

    assertTrue(importedDataOpt.isPresent());
    EmailConfigurationData importedData = importedDataOpt.get();
    assertThat(importedData.getUsername(), is("admin"));
    assertThat(importedData.getHost(), is("localhost"));
    assertThat(importedData.getPort(), is(90));
    // sensitive data shouldn't be stored
    assertThat(importedData.getPassword(), not("admin123"));
  }

  private EmailConfiguration createEmailConfiguration() {
    EmailConfiguration configuration = new EmailConfigurationData();
    configuration.setEnabled(true);
    configuration.setHost("localhost");
    configuration.setPort(90);
    configuration.setUsername("admin");
    configuration.setPassword(mock(Secret.class));
    configuration.setFromAddress("address");
    configuration.setSubjectPrefix("subject_prefix");
    configuration.setStartTlsEnabled(true);
    configuration.setStartTlsRequired(true);
    configuration.setSslOnConnectEnabled(true);
    configuration.setSslCheckServerIdentityEnabled(true);
    configuration.setNexusTrustStoreEnabled(true);

    return configuration;
  }
}
