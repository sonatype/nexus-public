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
package org.sonatype.nexus.internal.security.secrets;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.assertj.db.type.Table;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.db.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class SecretsStoreImplTests
    extends TestSupport
{
  private static final String LDAP = "ldap";

  private static final String MY_KEY2 = "my-key2";

  private static final String MY_SECRET2 = "my-secret2";

  private static final String MY_SECRET = "my-secret";

  private static final String MY_KEY = "my-key";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(SecretsDAO.class);

  @Mock
  private EventManager eventManager;

  SecretsStoreImpl underTest;

  @Before
  public void setUp() {
    underTest = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      EventManager getEventManager() {
        return eventManager;
      }
    }).getInstance(SecretsStoreImpl.class);
  }

  @Test
  public void testCreate() {
    final int id = underTest.create(LDAP, MY_KEY, MY_SECRET, "jsmith");

    assertThat(table()).row()
        .value("id").isEqualTo(id)
        .value("purpose").isEqualTo(LDAP)
        .value("key_id").isEqualTo(MY_KEY)
        .value("secret").isEqualTo(MY_SECRET)
        .value("user_id").isEqualTo("jsmith");
  }

  @Test
  public void testDelete() {
    // create another row so the DB has data
    final int id = underTest.create(LDAP, MY_KEY, MY_SECRET, null);

    assertTrue("Removed record", underTest.delete(id));

    // sanity check row remoevd
    assertThat(table()).hasNumberOfRows(0);
  }

  @Test
  public void testDelete_missing() {
    // create another row so the DB has data
    final int id = underTest.create(LDAP, MY_KEY, MY_SECRET, null);

    assertFalse("No rows removed", underTest.delete(id + 1));

    // sanity check no data touched
    assertThat(table()).hasNumberOfRows(1);
  }

  @Test
  public void testUpdate() {
    final int id = underTest.create(LDAP, MY_KEY, MY_SECRET, null);

    assertTrue(underTest.update(id, MY_KEY2, MY_SECRET2));

    assertThat(table()).row()
        .value("id").isEqualTo(id)
        .value("purpose").isEqualTo(LDAP)
        .value("key_id").isEqualTo(MY_KEY2)
        .value("secret").isEqualTo(MY_SECRET2)
        .value("user_id").isNull();
  }

  @Test
  public void testUpdate_missing() {
    // create a different row so the db has data
    int id = underTest.create(LDAP, MY_KEY, MY_SECRET, null);

    assertFalse("No rows updated", underTest.update(id + 1, MY_KEY, MY_SECRET));
  }

  @Test
  public void testRead() {
    final int id = underTest.create(LDAP, MY_KEY, MY_SECRET, "jsmith");

    Optional<SecretData> actual = underTest.read(id);
    assertSecret(actual, id, MY_KEY, MY_SECRET, "jsmith");
  }

  @Test
  public void testRead_missing() {
    final int id = underTest.create(LDAP, MY_KEY, MY_SECRET, "jsmith");

    Optional<SecretData> actual = underTest.read(id + 1);
    assertFalse(actual.isPresent());
  }

  @Test
  public void testBrowse() {
    // check empty
    Continuation<SecretData> actual = underTest.browse(null, 100);
    assertThat(actual, notNullValue());
    assertThat(actual, empty());

    underTest.create(LDAP, MY_KEY, MY_SECRET, "jsmith");
    underTest.create(LDAP, MY_KEY2, MY_SECRET2, "jlopez");

    // verify first page
    actual = underTest.browse(null, 1);
    assertThat(actual, hasSize(1));
    assertSecret(actual.iterator().next(), MY_KEY, MY_SECRET, "jsmith");

    // verify using continuation to get second page
    actual = underTest.browse(actual.nextContinuationToken(), 1);
    assertThat(actual, hasSize(1));
    assertSecret(actual.iterator().next(), MY_KEY2, MY_SECRET2, "jlopez");

    // verify going beyond the current page
    actual = underTest.browse(actual.nextContinuationToken(), 1);
    assertThat(actual, notNullValue());
    assertThat(actual, empty());
  }

  private Table table() {
    DataStore<?> dataStore = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).orElseThrow(RuntimeException::new);
    return new Table(dataStore.getDataSource(), "secrets");
  }

  private void assertSecret(final SecretData next, final String myKey, final String mySecret2, final String userId) {
    assertSecret(Optional.ofNullable(next), myKey, mySecret2, userId);
  }

  private static void assertSecret(
      final Optional<SecretData> actual,
      final int id,
      final String keyId,
      final String secret,
      final String userId)
  {
    assertSecret(actual, keyId, secret, userId);
    assertThat(actual.get().getId(), is(id));
  }

  private static void assertSecret(
      final Optional<SecretData> actual,
      final String keyId,
      final String secret,
      final String userId)
  {
    assertThat(actual, notNullValue());
    assertTrue(actual.isPresent());
    assertThat(actual.get().getId(), notNullValue());
    assertThat(actual.get().getKeyId(), is(keyId));
    assertThat(actual.get().getSecret(), is(secret));
    assertThat(actual.get().getUserId(), is(userId));
  }
}
