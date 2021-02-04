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
import java.util.UUID;

import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link CRole} by {@link RoleExport}
 */
public class RoleExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("CRole", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    List<CRole> roles = Arrays.asList(
        createCRole("Role 1"),
        createCRole("Role 2"));

    SecurityConfiguration source = mock(SecurityConfiguration.class);
    when(source.getRoles()).thenReturn(roles);

    RoleExport exporter = new RoleExport(source);
    exporter.export(jsonFile);
    List<CRoleData> importedData = jsonExporter.importFromJson(jsonFile, CRoleData.class);

    assertThat(importedData.size(), is(2));
    assertThat(importedData.toString(), allOf(
        containsString("Role 1"),
        containsString("Role 2"),
        containsString("Description"),
        containsString("nx-all"),
        containsString("nx-admin"),
        containsString("nx-user"),
        containsString("custom-role"),
        containsString("admin-role"),
        containsString("user-role")));
  }

  private CRole createCRole(final String name) {
    CRoleData role = new CRoleData();
    role.setId(UUID.randomUUID().toString());
    role.setName(name);
    role.setDescription("Description");
    role.setReadOnly(true);
    role.setVersion(1);
    role.setPrivileges(ImmutableSet.of("nx-all", "nx-admin", "nx-user"));
    role.setRoles(ImmutableSet.of("custom-role", "admin-role", "user-role"));

    return role;
  }
}
