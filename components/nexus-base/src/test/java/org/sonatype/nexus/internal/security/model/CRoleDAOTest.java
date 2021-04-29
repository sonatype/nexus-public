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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Category(SQLTestGroup.class)
public class CRoleDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(CRoleDAO.class);

  private DataSession session;

  private CRoleDAO dao;

  @Before
  public void setup() {
    session = sessionRule.openSession(DataStoreManager.DEFAULT_DATASTORE_NAME);
    dao = (CRoleDAO) session.access(CRoleDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testCreateReadUpdateDelete() {
    CRoleData role = new CRoleData();
    role.setId("admin");
    role.setName("administrator");
    role.setDescription("Admin User");
    role.setPrivileges(Stream.of("priv1", "priv2").collect(Collectors.toSet()));
    role.setRoles(Stream.of("role1", "role2").collect(Collectors.toSet()));
    role.setReadOnly(true);

    dao.create(role);

    CRoleData read = dao.read("admin").get();

    assertThat(read, not(nullValue()));
    assertThat(read.getId(), is("admin"));
    assertThat(read.getName(), is("administrator"));
    assertThat(read.getDescription(), is("Admin User"));
    assertThat(read.getPrivileges(), is(Stream.of("priv1", "priv2").collect(Collectors.toSet())));
    assertThat(read.getRoles(), is(Stream.of("role1", "role2").collect(Collectors.toSet())));
    assertThat(read.isReadOnly(), is(true));

    role.setName("Administrator");
    role.setDescription("Admin 2");
    role.setPrivileges(Stream.of("priv2", "priv3").collect(Collectors.toSet()));
    role.setRoles(singleton("role3"));
    role.setReadOnly(false);
    dao.update(role);

    read = dao.read("admin").get();

    assertThat(read, not(nullValue()));
    assertThat(read.getName(), is("Administrator"));
    assertThat(read.getDescription(), is("Admin 2"));
    assertThat(read.getPrivileges(), is(Stream.of("priv2", "priv3").collect(Collectors.toSet())));
    assertThat(read.getRoles(), is(singleton("role3")));
    assertThat(read.isReadOnly(), is(false));

    dao.delete("admin");

    assertThat(dao.read("admin").isPresent(), is(false));
  }

  @Test
  public void testBrowse() {
    CRoleData role1 = new CRoleData();
    role1.setId("role1");
    role1.setName("Role1");
    role1.setDescription("Role 1");
    role1.setPrivileges(emptySet());
    role1.setRoles(emptySet());
    role1.setReadOnly(false);
    CRoleData role2 = new CRoleData();
    role2.setId("role2");
    role2.setName("Role2");
    role2.setDescription("Role 2");
    role2.setPrivileges(emptySet());
    role2.setRoles(emptySet());
    role2.setReadOnly(false);
    CRoleData role3 = new CRoleData();
    role3.setId("role3");
    role3.setName("Role3");
    role3.setDescription("Role 3");
    role3.setPrivileges(emptySet());
    role3.setRoles(emptySet());
    role3.setReadOnly(false);

    dao.create(role1);
    dao.create(role2);
    dao.create(role3);

    assertThat(Iterables.size(dao.browse()), is(3));
  }

  @Test
  public void testUpdate_returnsStatus() {
    CRoleData role = new CRoleData();
    role.setId("role1");
    role.setName("Role1");
    role.setDescription("Role 1");
    role.setPrivileges(emptySet());
    role.setRoles(emptySet());
    role.setReadOnly(false);

    assertThat(dao.update(role), is(false));

    dao.create(role);
    role.setDescription("Role 2");

    assertThat(dao.update(role), is(true));
  }
}
