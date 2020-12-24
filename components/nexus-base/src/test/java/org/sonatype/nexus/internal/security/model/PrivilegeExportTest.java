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
package org.sonatype.nexus.internal.security.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link CPrivilege} by {@link PrivilegeExport}
 */
public class PrivilegeExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("CPrivilege", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    List<CPrivilege> privileges = Arrays.asList(
        createCPrivilege("Privilege 1"),
        createCPrivilege("Privilege 2"));

    SecurityConfiguration source = mock(SecurityConfiguration.class);
    when(source.getPrivileges()).thenReturn(privileges);

    PrivilegeExport exporter = new PrivilegeExport(source);
    exporter.export(jsonFile);
    List<CPrivilegeData> importedData = jsonExporter.importFromJson(jsonFile, CPrivilegeData.class);

    assertThat(importedData.size(), is(2));
    assertThat(importedData.toString(), allOf(
        containsString("Privilege 1"),
        containsString("Privilege 2"),
        containsString("Application"),
        containsString("Privilege description")));
    // check connection doesn't contain sensitive data.
    assertThat(importedData.stream().map(data -> data.getProperty("password")).collect(toList()),
        not(contains("admin123")));
  }

  private CPrivilege createCPrivilege(final String name) {
    CPrivilegeData privilege = new CPrivilegeData();
    privilege.setName(name);
    privilege.setType("Application");
    privilege.setDescription("Privilege description");
    privilege.setReadOnly(true);
    privilege.setProperty("password", "admin123");

    return privilege;
  }
}
