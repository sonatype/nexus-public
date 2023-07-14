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
package org.sonatype.nexus.internal.security.apikey;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.security.authc.apikey.ApiKey;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.Iterables;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class ApiKeyDAOTest
    extends TestSupport
{
  private static final String DOMAIN = "a domain";

  private static final String ANOTHER_DOMAIN = "another domain";

  private static final char[] API_KEY1 = "api_key;:\"|\\}{}[]+-=-=3/><+\"|:@!^%$£&*~`_+_o".toCharArray();

  private static final char[] API_KEY2 = "api_key2;:\"|\\}{}[]+-=-=3/><+\"|:@!^%$£&*~`_+_o".toCharArray();

  private static final char[] API_KEY3 = "api_key3;:\"|\\}{}[]+-=-=3/><+\"|:@!^%$£&*~`_+_o".toCharArray();

  private static final char[] API_KEY4 = "api_key4;:\"|\\}{}[]+-=-=3/><+\"|:@!^%$£&*~`_+_o".toCharArray();

  private static final char[] YET_ANOTHER_API_KEY = "yet_another_api_key;:\"|\\}{}[]+-=-=3/><+\"|:@!^%$£&*~`_+_o".toCharArray();

  private static final String  A_PRINCIPAL = "principal1";

  private static final String ANOTHER_PRINCIPAL = "another_principal1";

  private static final String A_REALM = "realm1";

  private static final String ANOTHER_REALM = "another_realm";

  ApiKeyDAO apiKeyDAO;

  DataSession<?> session;

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(ApiKeyDAO.class).handle(new ApiKeyTokenTypeHandler());

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    apiKeyDAO = session.access(ApiKeyDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  /*
   * create should successfully create and fetch an ApiKey record
   */
  @Test
  public void testCreate() {
    ApiKeyData apiKeyEntity = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);

    apiKeyDAO.save(apiKeyEntity);

    ApiKey savedApiKey = findApiKey(DOMAIN, A_PRINCIPAL).get();
    assertSavedApiKey(savedApiKey, API_KEY1);
  }

  /*
   * update should successfully update matching ApiKey record
   */
  @Test
  public void testUpdate() {
    ApiKeyData apiKeyEntity = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData anotherApiKeyEntity = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(apiKeyEntity);
    apiKeyDAO.save(anotherApiKeyEntity);

    ApiKey savedApiKey1 = findApiKey(DOMAIN, A_PRINCIPAL).get();
    ApiKey savedApiKey2 = findApiKey(DOMAIN, ANOTHER_PRINCIPAL).get();

    // retrieve
    assertSavedApiKey(savedApiKey1, API_KEY1);
    assertSavedApiKey(savedApiKey2, API_KEY2);

    // make a change
    apiKeyEntity.setApiKey(YET_ANOTHER_API_KEY);
    apiKeyDAO.save(apiKeyEntity);

    ApiKey updatedApiKey = findApiKey(DOMAIN, A_PRINCIPAL).get();
    assertSavedApiKey(updatedApiKey, YET_ANOTHER_API_KEY);
  }

  /*
   * delete should successfully delete matching ApiKey record
   */
  public void testDelete() {
    ApiKeyData entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(entity1);
    apiKeyDAO.save(entity2);

    Optional<ApiKey> savedApiKey1 = findApiKey(DOMAIN, A_PRINCIPAL);
    Optional<ApiKey> savedApiKey2 = findApiKey(DOMAIN, ANOTHER_PRINCIPAL);

    assertTrue(savedApiKey1.isPresent());
    assertTrue(savedApiKey2.isPresent());

    apiKeyDAO.deleteKey(DOMAIN, new ApiKeyToken(API_KEY1));
    savedApiKey1 = findApiKey(DOMAIN, A_PRINCIPAL);
    savedApiKey2 = findApiKey(DOMAIN, ANOTHER_PRINCIPAL);
    assertFalse(savedApiKey1.isPresent());
    assertTrue(savedApiKey2.isPresent());
  }

  /*
   * browse should fetch all records
   */
  @Test
  public void testBrowsePrincipals() {
    ApiKeyData entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(entity1);
    apiKeyDAO.save(entity2);

    Collection<PrincipalCollection> allRecords = (Collection<PrincipalCollection>) apiKeyDAO.browsePrincipals();
    assertThat(allRecords, hasSize(2));
    assertThat(allRecords.stream().map(PrincipalCollection::getPrimaryPrincipal).collect(Collectors.toList()),
        containsInAnyOrder(A_PRINCIPAL, ANOTHER_PRINCIPAL));
  }

  /*
   * findApiKey should fetch records matching given domain and primary principal
   */
  @Test
  public void testFindApiKey() {
    apiKeyDAO.save(anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL));
    apiKeyDAO.save(anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL));
    apiKeyDAO.save(anApiKeyEntity(API_KEY3, ANOTHER_DOMAIN, A_PRINCIPAL));
    apiKeyDAO.save(anApiKeyEntity(API_KEY4, ANOTHER_DOMAIN, ANOTHER_PRINCIPAL));
    apiKeyDAO.save(anApiKeyEntity(API_KEY4, ANOTHER_DOMAIN, ANOTHER_PRINCIPAL));

    Collection<ApiKey> result = apiKeyDAO.findApiKeys(DOMAIN, A_PRINCIPAL);
    assertSavedApiKey(Iterables.getFirst(result, null), API_KEY1);

    //
    result = apiKeyDAO.findApiKeys(DOMAIN, ANOTHER_PRINCIPAL);
    assertSavedApiKey(Iterables.getFirst(result, null), API_KEY2);

    result = apiKeyDAO.findApiKeys(ANOTHER_DOMAIN, A_PRINCIPAL);
    assertSavedApiKey(Iterables.getFirst(result, null), API_KEY3);

    result = apiKeyDAO.findApiKeys(ANOTHER_DOMAIN, ANOTHER_PRINCIPAL);
    assertSavedApiKey(Iterables.getFirst(result, null), API_KEY4);
  }

  /*
   * findPrincipals should fetch records matching given domain and api key
   */
  @Test
  public void testFindPrincipals() {
    ApiKeyData apiKeyEntity = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData anotherApiKeyEntity = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(apiKeyEntity);
    apiKeyDAO.save(anotherApiKeyEntity);

    Optional<ApiKey>  result = apiKeyDAO.findPrincipals(DOMAIN, new ApiKeyToken(API_KEY1));
    assertThat(result.get().getPrincipals().getPrimaryPrincipal(), is(A_PRINCIPAL));

    result = apiKeyDAO.findPrincipals(DOMAIN, new ApiKeyToken(API_KEY2));
    assertThat(result.get().getPrincipals().getPrimaryPrincipal(), is(ANOTHER_PRINCIPAL));
  }

  /*
   * deleteAll should successfully delete all records
   */
  @Test
  public void testDeleteAll() {
    ApiKeyData entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(entity1);
    apiKeyDAO.save(entity2);

    Optional<ApiKey> savedApiKey1 = findApiKey(DOMAIN, A_PRINCIPAL);
    Optional<ApiKey> savedApiKey2 = findApiKey(DOMAIN, ANOTHER_PRINCIPAL);
    assertTrue(savedApiKey1.isPresent());
    assertTrue(savedApiKey2.isPresent());

    apiKeyDAO.deleteAllKeys();
    Collection<PrincipalCollection> allRecords = (Collection<PrincipalCollection>) apiKeyDAO.browsePrincipals();
    assertThat(allRecords, hasSize(0));
  }

  @Test
  public void testCount() {
    ApiKeyData entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData entity2 = anApiKeyEntity(API_KEY2, ANOTHER_DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(entity1);
    apiKeyDAO.save(entity2);

    assertThat(apiKeyDAO.count(DOMAIN), is(1));
  }

  @Test
  public void testBrowse() {
    ApiKeyData entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(entity1);
    apiKeyDAO.save(entity2);
    apiKeyDAO.save(anApiKeyEntity(API_KEY2, ANOTHER_DOMAIN, A_PRINCIPAL));

    assertThat(apiKeyDAO.browse(DOMAIN), containsInAnyOrder(token(entity1), token(entity2)));
  }

  @Test
  public void testBrowseByCreatedDate() {
    ApiKeyData entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL);
    entity1.setCreated(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
    apiKeyDAO.save(entity1);
    OffsetDateTime date = OffsetDateTime.now();
    apiKeyDAO.save(entity2);
    apiKeyDAO.save(anApiKeyEntity(API_KEY2, ANOTHER_DOMAIN, A_PRINCIPAL));

    assertThat(apiKeyDAO.browseByCreatedDate(DOMAIN, date), containsInAnyOrder(token(entity2)));
  }

  @Test
  public void testDeleteApiKeysByDomain() {
    // keys in different domains
    ApiKeyData entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL);
    ApiKeyData entity2 = anApiKeyEntity(API_KEY2, ANOTHER_DOMAIN, ANOTHER_PRINCIPAL);
    apiKeyDAO.save(entity1);
    apiKeyDAO.save(entity2);

    // Sanity check
    assertThat(apiKeyDAO.browse(DOMAIN), hasSize(1));

    // Remove keys in DOMAIN
    apiKeyDAO.deleteApiKeysByDomain(DOMAIN);

    // Verify only keys in DOMAIN were removed
    assertThat(apiKeyDAO.browse(DOMAIN), empty());
    assertThat(apiKeyDAO.browse(ANOTHER_DOMAIN), hasSize(1));
  }

  private static ApiKeyData anApiKeyEntity(final char[] apiKey, final String domain, final String primaryPrincipal) {
    PrincipalCollection principalCollection = principalCollection(primaryPrincipal, A_REALM);
    return anApiKeyEntity(apiKey, domain, principalCollection);
  }

  private static ApiKeyData anApiKeyEntity(
      final char[] apiKey,
      final String domain,
      final PrincipalCollection principal)
  {
    ApiKeyData data = new ApiKeyData();
    data.setDomain(domain);
    data.setPrincipals(principal);
    data.setApiKey(apiKey);
    return data;
  }

  private Optional<ApiKey> findApiKey(final String domain, final String primaryPrincipal) {
    return apiKeyDAO.findApiKeys(domain, primaryPrincipal).stream()
        .findFirst();
  }

  private static SimplePrincipalCollection principalCollection(final String principal, final String realm) {
    return new SimplePrincipalCollection(principal, realm);
  }

  private static void assertSavedApiKey(final ApiKey actualApiKey, final char[] expectedApiKey) {
    assertThat(new String(actualApiKey.getApiKey()), is(new String(expectedApiKey)));
  }

  private static Matcher<ApiKey> token(final ApiKey key) {
    return new TypeSafeMatcher<ApiKey>(ApiKey.class)
    {
      @Override
      public void describeTo(final Description description) {
        description.appendValue(key);
      }

      @Override
      protected boolean matchesSafely(final ApiKey item) {
        return item.getPrincipals().equals(key.getPrincipals()) && item.getDomain().equals(key.getDomain())
            && Arrays.equals(item.getApiKey(), key.getApiKey());
      }
    };
  }
}
