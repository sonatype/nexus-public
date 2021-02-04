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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.internal.security.model.SecurityUserExport.SecurityUserData;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Serialization/Deserialization {@link CUser} and {@link CUserRoleMapping}
 * by {@link SecurityUserExport}
 */
public class SecurityUserExportTest
{
  private final JsonExporter jsonExporter = new JsonExporter();

  private File jsonFile;

  @Before
  public void setup() throws IOException {
    jsonFile = File.createTempFile("SecurityUser", ".json");
  }

  @After
  public void tearDown() {
    jsonFile.delete();
  }

  @Test
  public void testExportImportToJson() throws Exception {
    List<CUser> users = new ArrayList<>(2);
    CUser user1 = createUser("user1", "firstName_1", "lastName_1");
    CUser user2 = createUser("user2", "firstName_2", "lastName_2");
    users.add(user1);
    users.add(user2);

    List<CUserRoleMapping> userRoleMappings = Arrays.asList(
        createCUserRoleMapping(user1),
        createCUserRoleMapping(user2));

    SecurityConfiguration store = mock(SecurityConfiguration.class);
    when(store.getUsers()).thenReturn(users);
    when(store.getUserRoleMappings()).thenReturn(userRoleMappings);

    SecurityUserExport exporter = new SecurityUserExport(store);
    exporter.export(jsonFile);
    List<SecurityUserData> importedData = jsonExporter.importFromJson(jsonFile, SecurityUserData.class);

    assertThat(importedData.size(), is(2));
    for (SecurityUserData data : importedData) {
      CUser user = data.getUser();
      assertThat(user.getFirstName(), anyOf(is("firstName_1"), is("firstName_2")));
      assertThat(user.getLastName(), anyOf(is("lastName_1"), is("lastName_2")));
      assertThat(user.getEmail(), is("name@example.com"));
      assertThat(user.getStatus(), is("active"));
      assertThat(user.getVersion(), is(1));
      // deserialized object shouldn't contain sensitive data
      assertThat(user.getPassword(), not("admin123"));

      CUserRoleMapping cUserRoleMapping = data.getUserRoleMappings().get(0);
      assertThat(cUserRoleMapping.getSource(), is(UserManager.DEFAULT_SOURCE));
      assertThat(cUserRoleMapping.getRoles(), containsInAnyOrder("nx-admin", "nx-user"));
    }
  }

  private CUser createUser(final String id, final String firstName, final String lastName) {
    CUserData user = new CUserData();
    user.setId(id);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setPassword("admin123");
    user.setEmail("name@example.com");
    user.setStatus("active");
    user.setVersion(1);

    return user;
  }

  private CUserRoleMapping createCUserRoleMapping(final CUser user) {
    CUserRoleMappingData roleMappingData = new CUserRoleMappingData();
    roleMappingData.setUserId(user.getId());
    roleMappingData.setSource(UserManager.DEFAULT_SOURCE);
    roleMappingData.setRoles(ImmutableSet.of("nx-admin", "nx-user"));

    return roleMappingData;
  }
}
