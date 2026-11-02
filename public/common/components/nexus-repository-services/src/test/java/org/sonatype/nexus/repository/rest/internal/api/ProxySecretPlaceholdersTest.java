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
package org.sonatype.nexus.repository.rest.internal.api;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProxySecretPlaceholdersTest
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static Map<String, Map<String, Object>> attrs(
      final String section,
      final String child,
      final Map<String, Object> values)
  {
    Map<String, Object> sectionMap = new HashMap<>();
    sectionMap.put(child, new HashMap<>(values));
    Map<String, Map<String, Object>> result = new HashMap<>();
    result.put(section, sectionMap);
    return result;
  }

  private static ObjectNode json(final String text) throws Exception {
    return (ObjectNode) MAPPER.readTree(text);
  }

  @Test
  void maskIntoAddsPlaceholderForStoredPassword() throws Exception {
    ObjectNode tree = json("{\"httpClient\":{\"authentication\":{\"type\":\"username\",\"username\":\"u\"}}}");

    ProxySecretPlaceholders.maskInto(tree, attrs("httpclient", "authentication", Map.of("password", "_id")));

    assertThat(tree.at("/httpClient/authentication/password").asText(), is(ProxySecretPlaceholders.PLACEHOLDER));
  }

  @Test
  void maskIntoUsesBearerTokenIdAndFallsBackToLegacyBearerToken() throws Exception {
    ObjectNode current = json("{\"httpClient\":{\"authentication\":{\"type\":\"bearer\"}}}");
    ObjectNode legacy = json("{\"httpClient\":{\"authentication\":{\"type\":\"bearer\"}}}");

    ProxySecretPlaceholders.maskInto(current, attrs("httpclient", "authentication", Map.of("bearerTokenId", "_id")));
    ProxySecretPlaceholders.maskInto(legacy, attrs("httpclient", "authentication", Map.of("bearerToken", "_old")));

    assertThat(current.at("/httpClient/authentication/bearerToken").asText(), is(ProxySecretPlaceholders.PLACEHOLDER));
    assertThat(legacy.at("/httpClient/authentication/bearerToken").asText(), is(ProxySecretPlaceholders.PLACEHOLDER));
  }

  @Test
  void maskIntoAddsNothingWhenNoSecretIsStored() throws Exception {
    ObjectNode tree = json("{\"httpClient\":{\"authentication\":{\"type\":\"username\",\"username\":\"u\"}}}");

    ProxySecretPlaceholders.maskInto(tree, attrs("httpclient", "authentication", new HashMap<>()));

    assertThat(tree.at("/httpClient/authentication").has("password"), is(false));
  }

  @Test
  void maskIntoIgnoresMissingAuthenticationNode() throws Exception {
    ObjectNode tree = json("{\"httpClient\":{}}");

    ProxySecretPlaceholders.maskInto(tree, attrs("httpclient", "authentication", Map.of("password", "_id")));

    assertThat(tree.at("/httpClient").has("authentication"), is(false));
  }

  @Test
  void maskIntoAddsPlaceholderForNtlmPassword() throws Exception {
    ObjectNode tree = json("{\"httpClient\":{\"authentication\":{\"type\":\"ntlm\",\"username\":\"u\"}}}");

    ProxySecretPlaceholders.maskInto(tree, attrs("httpclient", "authentication",
        Map.of("password", "_id", "ntlmHost", "h")));

    assertThat(tree.at("/httpClient/authentication/password").asText(), is(ProxySecretPlaceholders.PLACEHOLDER));
  }

  @Test
  void maskIntoTreatsNullStoredPasswordAsNotStored() throws Exception {
    ObjectNode tree = json("{\"httpClient\":{\"authentication\":{\"type\":\"username\",\"username\":\"u\"}}}");
    Map<String, Object> values = new HashMap<>();
    values.put("password", null);

    ProxySecretPlaceholders.maskInto(tree, attrs("httpclient", "authentication", values));

    assertThat(tree.at("/httpClient/authentication").has("password"), is(false));
  }

  @Test
  void maskIntoToleratesNullStoredAttributes() throws Exception {
    ObjectNode tree = json("{\"httpClient\":{\"authentication\":{\"type\":\"username\"}}}");

    ProxySecretPlaceholders.maskInto(tree, null);

    assertThat(tree.at("/httpClient/authentication").has("password"), is(false));
  }

  private static Map<String, Map<String, Object>> storedWith(
      final String key,
      final Object value,
      final String remoteUrl)
  {
    Map<String, Object> auth = new HashMap<>();
    auth.put(key, value);
    Map<String, Object> httpclient = new HashMap<>();
    httpclient.put("authentication", auth);
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", remoteUrl);
    Map<String, Map<String, Object>> result = new HashMap<>();
    result.put("httpclient", httpclient);
    result.put("proxy", proxy);
    return result;
  }

  private static Map<String, Object> auth(final Map<String, Map<String, Object>> attributes) {
    Map<String, Object> httpclient = attributes.get("httpclient");
    if (httpclient == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> auth = (Map<String, Object>) httpclient.get("authentication");
    return auth;
  }

  @Test
  void restoreReplacesPlaceholderWithStoredSecret() {
    Map<String, Map<String, Object>> stored = storedWith("password", "_stored", "http://a/");
    Map<String, Map<String, Object>> incoming =
        storedWith("password", ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("password"), is("_stored"));
  }

  @Test
  void restoreKeepsANewPassword() {
    Map<String, Map<String, Object>> stored = storedWith("password", "_stored", "http://a/");
    Map<String, Map<String, Object>> incoming = storedWith("password", "newPassword", "http://a/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("password"), is("newPassword"));
  }

  @Test
  void restoreFallsBackToLegacyBearerToken() {
    Map<String, Object> auth = new HashMap<>();
    auth.put("bearerToken", "_legacy");
    Map<String, Object> httpclient = new HashMap<>();
    httpclient.put("authentication", auth);
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> stored = new HashMap<>();
    stored.put("httpclient", httpclient);
    stored.put("proxy", proxy);

    Map<String, Map<String, Object>> incoming =
        storedWith("bearerTokenId", ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("bearerTokenId"), is("_legacy"));
  }

  @Test
  void restoreUsesBearerTokenIdWhenBothStored() {
    Map<String, Object> auth = new HashMap<>();
    auth.put("bearerTokenId", "_current");
    auth.put("bearerToken", "_legacy");
    Map<String, Object> httpclient = new HashMap<>();
    httpclient.put("authentication", auth);
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> stored = new HashMap<>();
    stored.put("httpclient", httpclient);
    stored.put("proxy", proxy);

    Map<String, Map<String, Object>> incoming =
        storedWith("bearerTokenId", ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("bearerTokenId"), is("_current"));
  }

  @Test
  void urlChangeWithPlaceholderIsRejectedAndNothingIsRestored() {
    Map<String, Map<String, Object>> stored = storedWith("password", "_stored", "http://a/");
    Map<String, Map<String, Object>> incoming =
        storedWith("password", ProxySecretPlaceholders.PLACEHOLDER, "http://b/");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> ProxySecretPlaceholders.guardAndRestore(stored, incoming));

    assertThat(e.getMessage(), is(ProxySecretPlaceholders.URL_CHANGED_MESSAGE));
    assertThat(auth(incoming).get("password"), is(ProxySecretPlaceholders.PLACEHOLDER));
  }

  @Test
  void urlChangeWithNewPasswordIsAllowed() {
    Map<String, Map<String, Object>> stored = storedWith("password", "_stored", "http://a/");
    Map<String, Map<String, Object>> incoming = storedWith("password", "newPassword", "http://b/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("password"), is("newPassword"));
  }

  @Test
  void placeholderWithNoStoredSecretIsRejected() {
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> stored = new HashMap<>();
    stored.put("proxy", proxy);
    Map<String, Map<String, Object>> incoming =
        storedWith("password", ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> ProxySecretPlaceholders.guardAndRestore(stored, incoming));

    assertThat(e.getMessage(), is("A stored secret was referenced but none exists for password; enter it again."));
  }

  @Test
  void noAuthenticationInIncomingIsANoOp() {
    Map<String, Map<String, Object>> stored = storedWith("password", "_stored", "http://a/");
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> incoming = new HashMap<>();
    incoming.put("proxy", proxy);

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(incoming.containsKey("httpclient"), is(false));
  }

  @Test
  void noPlaceholderIsANoOp() {
    Map<String, Map<String, Object>> stored = storedWith("password", "_stored", "http://a/");
    Map<String, Map<String, Object>> incoming = storedWith("password", "otherValue", "http://a/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("password"), is("otherValue"));
  }

  @Test
  void nullStoredAttributesWithPlaceholderIsRejectedNotAnNpe() {
    Map<String, Map<String, Object>> incoming =
        storedWith("password", ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    assertThrows(IllegalArgumentException.class, () -> ProxySecretPlaceholders.guardAndRestore(null, incoming));
  }

  @Test
  void sameUrlWithPlaceholderRestoresNtlmPassword() {
    Map<String, Object> auth = new HashMap<>();
    auth.put("password", "_stored");
    auth.put("ntlmHost", "h");
    Map<String, Object> httpclient = new HashMap<>();
    httpclient.put("authentication", auth);
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> stored = new HashMap<>();
    stored.put("httpclient", httpclient);
    stored.put("proxy", proxy);

    Map<String, Object> incomingAuth = new HashMap<>();
    incomingAuth.put("password", ProxySecretPlaceholders.PLACEHOLDER);
    incomingAuth.put("ntlmHost", "h");
    Map<String, Object> incomingHttpclient = new HashMap<>();
    incomingHttpclient.put("authentication", incomingAuth);
    Map<String, Object> incomingProxy = new HashMap<>();
    incomingProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> incoming = new HashMap<>();
    incoming.put("httpclient", incomingHttpclient);
    incoming.put("proxy", incomingProxy);

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("password"), is("_stored"));
  }

  @Test
  void maskIntoAddsPlaceholderForStoredEcrSecrets() throws Exception {
    ObjectNode tree = json("{\"dockerProxy\":{\"ecrAuth\":{\"enabled\":true}}}");

    Map<String, Object> ecrAuth = new HashMap<>();
    ecrAuth.put("enabled", true);
    ecrAuth.put("secretAccessKey", "_keyId");
    ecrAuth.put("sessionToken", "_tokenId");
    Map<String, Map<String, Object>> stored = attrs("dockerProxy", "ecrAuth", ecrAuth);

    ProxySecretPlaceholders.maskInto(tree, stored);

    assertThat(tree.at("/dockerProxy/ecrAuth/secretAccessKey").asText(), is(ProxySecretPlaceholders.PLACEHOLDER));
    assertThat(tree.at("/dockerProxy/ecrAuth/sessionToken").asText(), is(ProxySecretPlaceholders.PLACEHOLDER));
  }

  @Test
  void maskIntoAddsOnlyTheStoredEcrSecret() throws Exception {
    ObjectNode tree = json("{\"dockerProxy\":{\"ecrAuth\":{\"enabled\":true}}}");

    Map<String, Object> ecrAuth = new HashMap<>();
    ecrAuth.put("enabled", true);
    ecrAuth.put("sessionToken", "_tokenId");
    Map<String, Map<String, Object>> stored = attrs("dockerProxy", "ecrAuth", ecrAuth);

    ProxySecretPlaceholders.maskInto(tree, stored);

    assertThat(tree.at("/dockerProxy/ecrAuth").has("secretAccessKey"), is(false));
    assertThat(tree.at("/dockerProxy/ecrAuth/sessionToken").asText(), is(ProxySecretPlaceholders.PLACEHOLDER));
  }

  @Test
  void maskIntoIgnoresMissingEcrAuthNode() throws Exception {
    ObjectNode tree = json("{\"dockerProxy\":{}}");

    Map<String, Object> ecrAuth = new HashMap<>();
    ecrAuth.put("sessionToken", "_tokenId");
    Map<String, Map<String, Object>> stored = attrs("dockerProxy", "ecrAuth", ecrAuth);

    ProxySecretPlaceholders.maskInto(tree, stored);

    assertThat(tree.at("/dockerProxy").has("ecrAuth"), is(false));
  }

  @Test
  void maskIntoAddsNothingForEcrWhenNoSecretStored() throws Exception {
    ObjectNode tree = json("{\"dockerProxy\":{\"ecrAuth\":{\"enabled\":true}}}");

    Map<String, Object> ecrAuth = new HashMap<>();
    ecrAuth.put("enabled", true);
    Map<String, Map<String, Object>> stored = attrs("dockerProxy", "ecrAuth", ecrAuth);

    ProxySecretPlaceholders.maskInto(tree, stored);

    assertThat(tree.at("/dockerProxy/ecrAuth").has("secretAccessKey"), is(false));
    assertThat(tree.at("/dockerProxy/ecrAuth").has("sessionToken"), is(false));
  }

  @Test
  void restoreReplacesEcrPlaceholdersWithStoredSecrets() {
    Map<String, Map<String, Object>> stored = storedWithEcr("_keyId", "_tokenId", "http://a/");
    Map<String, Map<String, Object>> incoming = incomingWithEcr(
        ProxySecretPlaceholders.PLACEHOLDER, ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    @SuppressWarnings("unchecked")
    Map<String, Object> ecrAuth = (Map<String, Object>) incoming.get("dockerProxy").get("ecrAuth");
    assertThat(ecrAuth.get("secretAccessKey"), is("_keyId"));
    assertThat(ecrAuth.get("sessionToken"), is("_tokenId"));
  }

  @Test
  void restoreKeepsANewEcrSessionToken() {
    Map<String, Map<String, Object>> stored = storedWithEcr("_keyId", "_tokenId", "http://a/");
    Map<String, Map<String, Object>> incoming = incomingWithEcr("_keyId", "newToken", "http://a/");

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    @SuppressWarnings("unchecked")
    Map<String, Object> ecrAuth = (Map<String, Object>) incoming.get("dockerProxy").get("ecrAuth");
    assertThat(ecrAuth.get("secretAccessKey"), is("_keyId"));
    assertThat(ecrAuth.get("sessionToken"), is("newToken"));
  }

  @Test
  void ecrPlaceholderWithNoStoredSecretIsRejected() {
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> stored = new HashMap<>();
    stored.put("proxy", proxy);

    Map<String, Map<String, Object>> incoming = incomingWithEcr(
        ProxySecretPlaceholders.PLACEHOLDER, ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> ProxySecretPlaceholders.guardAndRestore(stored, incoming));

    assertThat(e.getMessage(),
        is("A stored secret was referenced but none exists for secretAccessKey; enter it again."));
  }

  @Test
  void urlChangeWithEcrPlaceholderIsRejected() {
    Map<String, Map<String, Object>> stored = storedWithEcr("_keyId", "_tokenId", "http://a/");
    Map<String, Map<String, Object>> incoming = incomingWithEcr(
        ProxySecretPlaceholders.PLACEHOLDER, ProxySecretPlaceholders.PLACEHOLDER, "http://b/");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> ProxySecretPlaceholders.guardAndRestore(stored, incoming));

    assertThat(e.getMessage(), is(ProxySecretPlaceholders.URL_CHANGED_MESSAGE));
    @SuppressWarnings("unchecked")
    Map<String, Object> ecrAuth = (Map<String, Object>) incoming.get("dockerProxy").get("ecrAuth");
    assertThat(ecrAuth.get("secretAccessKey"), is(ProxySecretPlaceholders.PLACEHOLDER));
    assertThat(ecrAuth.get("sessionToken"), is(ProxySecretPlaceholders.PLACEHOLDER));
  }

  @Test
  void restoreHandlesPasswordAndEcrSessionTokenTogether() {
    Map<String, Object> storedAuth = new HashMap<>();
    storedAuth.put("password", "_pwd");
    Map<String, Object> storedHttpclient = new HashMap<>();
    storedHttpclient.put("authentication", storedAuth);
    Map<String, Object> storedEcrAuth = new HashMap<>();
    storedEcrAuth.put("sessionToken", "_tokenId");
    Map<String, Object> storedDockerProxy = new HashMap<>();
    storedDockerProxy.put("ecrAuth", storedEcrAuth);
    Map<String, Object> storedProxy = new HashMap<>();
    storedProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> stored = new HashMap<>();
    stored.put("httpclient", storedHttpclient);
    stored.put("dockerProxy", storedDockerProxy);
    stored.put("proxy", storedProxy);

    Map<String, Object> incomingAuth = new HashMap<>();
    incomingAuth.put("password", ProxySecretPlaceholders.PLACEHOLDER);
    Map<String, Object> incomingHttpclient = new HashMap<>();
    incomingHttpclient.put("authentication", incomingAuth);
    Map<String, Object> incomingEcrAuth = new HashMap<>();
    incomingEcrAuth.put("sessionToken", ProxySecretPlaceholders.PLACEHOLDER);
    Map<String, Object> incomingDockerProxy = new HashMap<>();
    incomingDockerProxy.put("ecrAuth", incomingEcrAuth);
    Map<String, Object> incomingProxy = new HashMap<>();
    incomingProxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> incoming = new HashMap<>();
    incoming.put("httpclient", incomingHttpclient);
    incoming.put("dockerProxy", incomingDockerProxy);
    incoming.put("proxy", incomingProxy);

    ProxySecretPlaceholders.guardAndRestore(stored, incoming);

    assertThat(auth(incoming).get("password"), is("_pwd"));
    @SuppressWarnings("unchecked")
    Map<String, Object> ecrAuth = (Map<String, Object>) incoming.get("dockerProxy").get("ecrAuth");
    assertThat(ecrAuth.get("sessionToken"), is("_tokenId"));
  }

  @Test
  void ecrPlaceholderWithNullStoredIsRejectedNotAnNpe() {
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", "http://a/");
    Map<String, Map<String, Object>> stored = new HashMap<>();
    stored.put("proxy", proxy);
    Map<String, Map<String, Object>> incoming = incomingWithEcr(
        ProxySecretPlaceholders.PLACEHOLDER, ProxySecretPlaceholders.PLACEHOLDER, "http://a/");

    assertThrows(IllegalArgumentException.class, () -> ProxySecretPlaceholders.guardAndRestore(stored, incoming));
  }

  private static Map<String, Map<String, Object>> storedWithEcr(
      final String secretKey,
      final String sessionToken,
      final String remoteUrl)
  {
    Map<String, Object> ecrAuth = new HashMap<>();
    ecrAuth.put("secretAccessKey", secretKey);
    ecrAuth.put("sessionToken", sessionToken);
    Map<String, Object> dockerProxy = new HashMap<>();
    dockerProxy.put("ecrAuth", ecrAuth);
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", remoteUrl);
    Map<String, Map<String, Object>> result = new HashMap<>();
    result.put("dockerProxy", dockerProxy);
    result.put("proxy", proxy);
    return result;
  }

  private static Map<String, Map<String, Object>> incomingWithEcr(
      final String secretKey,
      final String sessionToken,
      final String remoteUrl)
  {
    Map<String, Object> ecrAuth = new HashMap<>();
    ecrAuth.put("secretAccessKey", secretKey);
    ecrAuth.put("sessionToken", sessionToken);
    Map<String, Object> dockerProxy = new HashMap<>();
    dockerProxy.put("ecrAuth", ecrAuth);
    Map<String, Object> proxy = new HashMap<>();
    proxy.put("remoteUrl", remoteUrl);
    Map<String, Map<String, Object>> result = new HashMap<>();
    result.put("dockerProxy", dockerProxy);
    result.put("proxy", proxy);
    return result;
  }
}
