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
package org.sonatype.nexus.internal.jwt.datastore;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.SingletonDataAccess;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class JwtSecretDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(JwtSecretDAO.class);

  private DataSession<?> session;

  private JwtSecretDAO dao;

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(JwtSecretDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testSetAndGet() {
    Optional<String> emptySecret = withDao(SingletonDataAccess::get);
    assertThat(emptySecret.isPresent(), is(false));

    callDao(dao -> dao.setIfEmpty("secret"));
    Optional<String> initialSecret = withDao(SingletonDataAccess::get);

    assertThat(initialSecret.isPresent(), is(true));
    assertThat(initialSecret.get(), is("secret"));

    callDao(dao -> dao.set("new-secret"));
    Optional<String> newSecret = withDao(SingletonDataAccess::get);

    assertThat(newSecret.isPresent(), is(true));
    assertThat(newSecret.get(), is("new-secret"));
  }

  private void callDao(final Consumer<JwtSecretDAO> fn) {
    fn.accept(dao);
    session.getTransaction().commit();
  }

  private <T> T withDao(final Function<JwtSecretDAO, T> fn) {
    T result = fn.apply(dao);
    session.getTransaction().commit();
    return result;
  }
}
