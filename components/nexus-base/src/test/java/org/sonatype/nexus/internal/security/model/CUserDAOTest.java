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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.security.config.CUser.STATUS_ACTIVE;
import static org.sonatype.nexus.security.config.CUser.STATUS_DISABLED;

@Category(SQLTestGroup.class)
public class CUserDAOTest
    extends TestSupport
{
  private static final String PASSWORD1 =
      "$shiro1$SHA-512$1024$NYQKemFvZqat9CepP2xO9A==$4m4dBi9f/EtJLpJSW6/7+IVxW3wHR4RNeGtbopiH+D5tlVDFqNKo667eMnqWUxFrRz4Y4IQvn5hv/BnWmEfN0Q==";

  private static final String PASSWORD2 =
      "$shiro1$SHA-512$1024$IDetfwWXaulpIe+XL7nOyQ==$ad70UxpgqaXRzaJ41mLnKMy1hzyu3+v7dQ44VHrrNVRpA11S17ZnQX22MZZhjih9DLDEWTe3hJmCfZ8s7/mRHQ==";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(CUserDAO.class);

  private DataSession session;

  private CUserDAO dao;

  @Before
  public void setup() {
    session = sessionRule.openSession(DataStoreManager.DEFAULT_DATASTORE_NAME);
    dao = (CUserDAO) session.access(CUserDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testCreateReadUpdateDelete() {
    CUserData user = new CUserData();
    user.setId("jdoe");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setPassword(PASSWORD1);
    user.setStatus(STATUS_ACTIVE);
    user.setEmail("jdoe@example.com");
    dao.create(user);

    CUserData read = dao.read("jdoe").get();

    assertThat(read, not(nullValue()));
    assertThat(read.getId(), is("jdoe"));
    assertThat(read.getFirstName(), is("John"));
    assertThat(read.getLastName(), is("Doe"));
    assertThat(read.getPassword(), is(PASSWORD1));
    assertThat(read.getStatus(), is(STATUS_ACTIVE));

    user.setFirstName("Jonathan");
    user.setLastName("DoeDoe");
    user.setPassword(PASSWORD2);
    user.setStatus(STATUS_DISABLED);
    dao.update(user);

    read = dao.read("jdoe").get();

    assertThat(read, not(nullValue()));
    assertThat(read.getFirstName(), is("Jonathan"));
    assertThat(read.getLastName(), is("DoeDoe"));
    assertThat(read.getPassword(), is(PASSWORD2));
    assertThat(read.getStatus(), is(STATUS_DISABLED));

    dao.delete("jdoe");

    assertThat(dao.read("jdoe").isPresent(), is(false));
  }

  @Test
  public void testBrowse() {
    CUserData user1 = new CUserData();
    user1.setId("jdoe");
    user1.setFirstName("John");
    user1.setLastName("Doe");
    user1.setPassword(PASSWORD1);
    user1.setStatus(STATUS_ACTIVE);
    user1.setEmail("jdoe@example.com");
    CUserData user2 = new CUserData();
    user2.setId("msmith");
    user2.setFirstName("Mark");
    user2.setLastName("Smith");
    user2.setPassword(PASSWORD1);
    user2.setStatus(STATUS_ACTIVE);
    user2.setEmail("msmith@example.com");
    CUserData user3 = new CUserData();
    user3.setId("ajones");
    user3.setFirstName("April");
    user3.setLastName("Jones");
    user3.setPassword(PASSWORD1);
    user3.setStatus(STATUS_ACTIVE);
    user3.setEmail("ajones@example.com");

    dao.create(user1);
    dao.create(user2);
    dao.create(user3);

    assertThat(Iterables.size(dao.browse()), is(3));
  }

  @Test
  public void testUpdate_returnsStatus() {
    CUserData user = new CUserData();
    user.setId("jdoe");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setPassword(PASSWORD1);
    user.setStatus(STATUS_ACTIVE);
    user.setEmail("jdoe@example.com");

    assertThat(dao.update(user), is(false));

    dao.create(user);
    user.setPassword(PASSWORD2);

    assertThat(dao.update(user), is(true));
  }
}
