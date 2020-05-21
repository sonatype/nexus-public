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
package org.sonatype.nexus.internal.security.apikey

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSession
import org.sonatype.nexus.testdb.DataSessionRule

import org.apache.shiro.subject.SimplePrincipalCollection
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification

import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME

@Category(SQLTestGroup.class)
class ApiKeyDAOTest
    extends Specification
{

  private static final DOMAIN = 'a domain'

  private static final ANOTHER_DOMAIN = 'another domain'

  private static final API_KEY1 = 'api_key;:"|\\}{}[]+-=-=3/><+"|:@!^%$£&*~`_+_o'.chars

  private static final API_KEY2 = 'api_key2;:"|\\}{}[]+-=-=3/><+"|:@!^%$£&*~`_+_o'.chars

  private static final API_KEY3 = 'api_key3;:"|\\}{}[]+-=-=3/><+"|:@!^%$£&*~`_+_o'.chars

  private static final API_KEY4 = 'api_key4;:"|\\}{}[]+-=-=3/><+"|:@!^%$£&*~`_+_o'.chars

  private static final YET_ANOTHER_API_KEY = 'yet_another_api_key;:"|\\}{}[]+-=-=3/><+"|:@!^%$£&*~`_+_o'.chars

  private static final  A_PRINCIPAL = 'principal1'

  private static final ANOTHER_PRINCIPAL = 'another_principal1'

  private static final A_REALM = 'realm1'

  private static final ANOTHER_REALM = 'another_realm'

  ApiKeyDAO apiKeyDAO

  DataSession session

  @Rule
  DataSessionRule sessionRule = new DataSessionRule().access(ApiKeyDAO).handle(new ApiKeyTokenTypeHandler())

  void setup() {
    session = sessionRule.openSession(CONFIG_DATASTORE_NAME)
    apiKeyDAO = session.access(ApiKeyDAO)
  }

  void cleanup() {
    session.close()
  }

  def 'create should successfully create and fetch an ApiKey record'() {
    given:
      def apiKeyEntity = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL)
    when:
      apiKeyDAO.save(apiKeyEntity)
    then:
      def savedApiKey = apiKeyDAO.findApiKey(DOMAIN, A_PRINCIPAL).get()
      assertSavedApiKey(savedApiKey, API_KEY1)
  }

  def 'update should successfully update matching ApiKey record'() {
    given:
      def apiKeyEntity = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL)
      def anotherApiKeyEntity = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL)
      apiKeyDAO.save(apiKeyEntity)
      apiKeyDAO.save(anotherApiKeyEntity)
    when:
      def savedApiKey1 = apiKeyDAO.findApiKey(DOMAIN, A_PRINCIPAL).get()
      def savedApiKey2 = apiKeyDAO.findApiKey(DOMAIN, ANOTHER_PRINCIPAL).get()
    then:
      assertSavedApiKey(savedApiKey1, API_KEY1)
      assertSavedApiKey(savedApiKey2, API_KEY2)
    when:
      apiKeyEntity.setApiKey(YET_ANOTHER_API_KEY)
      apiKeyDAO.save(apiKeyEntity)
    then:
      def updatedApiKey = apiKeyDAO.findApiKey(DOMAIN, A_PRINCIPAL).get()
      assertSavedApiKey(updatedApiKey, YET_ANOTHER_API_KEY)
  }

  def 'delete should successfully delete matching ApiKey record'() {
    given:
      def entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL)
      def entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL)
      apiKeyDAO.save(entity1)
      apiKeyDAO.save(entity2)
    when:
      def savedApiKey1 = apiKeyDAO.findApiKey(DOMAIN, A_PRINCIPAL)
      def savedApiKey2 = apiKeyDAO.findApiKey(DOMAIN, ANOTHER_PRINCIPAL)
    then:
      savedApiKey1.present
      savedApiKey2.present
    when:
      apiKeyDAO.deleteKeys(A_PRINCIPAL)
    and:
      savedApiKey1 = apiKeyDAO.findApiKey(DOMAIN, A_PRINCIPAL)
      savedApiKey2 = apiKeyDAO.findApiKey(DOMAIN, ANOTHER_PRINCIPAL)
    then:
      !savedApiKey1.present
      savedApiKey2.present
  }

  def 'browse should fetch all records'() {
    given:
      def entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL)
      def entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL)
      apiKeyDAO.save(entity1)
      apiKeyDAO.save(entity2)
    when:
      def allRecords = apiKeyDAO.browsePrincipals()
    then:
      allRecords.size() == 2
      allRecords.collect{ record -> record.primaryPrincipal }.containsAll(A_PRINCIPAL, ANOTHER_PRINCIPAL)
  }

  def 'findApiKey should fetch records matching given domain and primary principal'() {
    given:
      apiKeyDAO.save(anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL))
      apiKeyDAO.save(anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL))
      apiKeyDAO.save(anApiKeyEntity(API_KEY3, ANOTHER_DOMAIN, A_PRINCIPAL))
      apiKeyDAO.save(anApiKeyEntity(API_KEY4, ANOTHER_DOMAIN, ANOTHER_PRINCIPAL))
    when:
      def result = apiKeyDAO.findApiKey(DOMAIN, A_PRINCIPAL).get()
    then:
      assertSavedApiKey(result, API_KEY1)
    when:
      result = apiKeyDAO.findApiKey(DOMAIN, ANOTHER_PRINCIPAL).get()
    then:
      assertSavedApiKey(result, API_KEY2)
    when:
      result = apiKeyDAO.findApiKey(ANOTHER_DOMAIN, A_PRINCIPAL).get()
    then:
      assertSavedApiKey(result, API_KEY3)
    when:
      result = apiKeyDAO.findApiKey(ANOTHER_DOMAIN, ANOTHER_PRINCIPAL).get()
    then:
      assertSavedApiKey(result, API_KEY4)
  }

  def 'findPrincipals should fetch records matching given domain and api key'() {
    given:
      def apiKeyEntity = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL)
      def anotherApiKeyEntity = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL)
      apiKeyDAO.save(apiKeyEntity)
      apiKeyDAO.save(anotherApiKeyEntity)
    when:
      def result = apiKeyDAO.findPrincipals(DOMAIN, new ApiKeyToken(API_KEY1)).get()
    then:
      result.primaryPrincipal == A_PRINCIPAL
    when:
      result = apiKeyDAO.findPrincipals(DOMAIN, new ApiKeyToken(API_KEY2)).get()
    then:
      result.primaryPrincipal == ANOTHER_PRINCIPAL
  }

  def 'deleteAll should successfully delete all records'() {
    given:
      def entity1 = anApiKeyEntity(API_KEY1, DOMAIN, A_PRINCIPAL)
      def entity2 = anApiKeyEntity(API_KEY2, DOMAIN, ANOTHER_PRINCIPAL)
      apiKeyDAO.save(entity1)
      apiKeyDAO.save(entity2)
    when:
      def savedApiKey1 = apiKeyDAO.findApiKey(DOMAIN, A_PRINCIPAL)
      def savedApiKey2 = apiKeyDAO.findApiKey(DOMAIN, ANOTHER_PRINCIPAL)
    then:
      savedApiKey1.present
      savedApiKey2.present
    when:
      apiKeyDAO.deleteAllKeys()
    then:
      def allRecords = apiKeyDAO.browsePrincipals()
      allRecords.size() == 0
  }

  private static ApiKey anApiKeyEntity(char[] apiKey, String domain, String primaryPrincipal) {
    def principalCollection = principalCollection(primaryPrincipal, A_REALM)
    new ApiKeyData(domain: domain, principals: principalCollection, apiKey: apiKey)
  }

  private static SimplePrincipalCollection principalCollection(String principal, String realm) {
    new SimplePrincipalCollection(principal, realm)
  }

  private static void assertSavedApiKey(final ApiKeyToken actualApiKey, final char[] expectedApiKey) {
    assert Arrays.equals(actualApiKey.chars, expectedApiKey)
  }
}

