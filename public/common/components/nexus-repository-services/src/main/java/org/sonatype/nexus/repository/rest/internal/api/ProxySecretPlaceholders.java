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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Adds placeholder markers for stored proxy secrets into a JSON response tree.
 * This class never reads secret values, only checks for their presence.
 */
public final class ProxySecretPlaceholders
{
  // Must equal org.sonatype.nexus.rapture.PasswordPlaceholder.get(); a drift test enforces it.
  public static final String PLACEHOLDER = "#~NXRM~PLACEHOLDER~PASSWORD~#";

  public static final String URL_CHANGED_MESSAGE =
      "Remote URL has changed. Authentication credentials have been reset and must be re-entered.";

  private static final String HTTPCLIENT = "httpclient";

  private static final String AUTHENTICATION = "authentication";

  private static final String PROXY = "proxy";

  private static final String REMOTE_URL = "remoteUrl";

  private static final String DOCKER_PROXY = "dockerProxy";

  private static final String ECR_AUTH = "ecrAuth";

  private static final List<String> SECRET_KEYS = List.of("password", "bearerTokenId");

  private static final List<String> ECR_SECRET_KEYS = List.of("secretAccessKey", "sessionToken");

  private ProxySecretPlaceholders() {
    // static helpers
  }

  /** Adds the placeholder to {@code json} for every secret that is stored; never reads secret values. */
  public static void maskInto(final ObjectNode json, final Map<String, Map<String, Object>> stored) {
    JsonNode auth = json.path("httpClient").path(AUTHENTICATION);
    if (auth instanceof ObjectNode authNode) {
      Map<String, Object> storedAuth = child(stored, HTTPCLIENT, AUTHENTICATION);
      if (storedAuth != null) {
        if (storedAuth.get("password") != null) {
          authNode.put("password", PLACEHOLDER);
        }
        if (storedAuth.get("bearerTokenId") != null || storedAuth.get("bearerToken") != null) {
          authNode.put("bearerToken", PLACEHOLDER);
        }
      }
    }

    JsonNode ecrAuth = json.path(DOCKER_PROXY).path(ECR_AUTH);
    if (ecrAuth instanceof ObjectNode ecrAuthNode) {
      Map<String, Object> storedEcrAuth = child(stored, DOCKER_PROXY, ECR_AUTH);
      if (storedEcrAuth != null) {
        for (String key : ECR_SECRET_KEYS) {
          if (storedEcrAuth.get(key) != null) {
            ecrAuthNode.put(key, PLACEHOLDER);
          }
        }
      }
    }
  }

  /**
   * Guards against remote URL changes carrying placeholders, then restores stored secrets.
   * Throws IllegalArgumentException if remoteUrl changed while carrying a placeholder,
   * or if a placeholder references a secret that doesn't exist in stored.
   * Mutates incoming attributes in place when restoring.
   */
  public static void guardAndRestore(
      final Map<String, Map<String, Object>> stored,
      final Map<String, Map<String, Object>> incoming)
  {
    if (incoming == null) {
      return;
    }

    if (!hasPlaceholder(incoming)) {
      return;
    }

    String storedUrl = remoteUrl(stored);
    String incomingUrl = remoteUrl(incoming);
    if (storedUrl != null && incomingUrl != null && !storedUrl.equals(incomingUrl)) {
      throw new IllegalArgumentException(URL_CHANGED_MESSAGE);
    }

    Map<String, Object> incomingAuth = child(incoming, HTTPCLIENT, AUTHENTICATION);
    Map<String, Object> storedAuth = child(stored, HTTPCLIENT, AUTHENTICATION);
    for (String key : SECRET_KEYS) {
      if (PLACEHOLDER.equals(incomingAuth == null ? null : incomingAuth.get(key))) {
        Object storedValue = storedAuth == null ? null : storedAuth.get(key);
        // Fall back to legacy bearerToken for bearerTokenId
        if ("bearerTokenId".equals(key) && storedValue == null && storedAuth != null) {
          storedValue = storedAuth.get("bearerToken");
        }
        if (storedValue == null) {
          throw new IllegalArgumentException(
              "A stored secret was referenced but none exists for " + key + "; enter it again.");
        }
        if (incomingAuth != null) {
          incomingAuth.put(key, storedValue);
        }
      }
    }

    Map<String, Object> incomingEcrAuth = child(incoming, DOCKER_PROXY, ECR_AUTH);
    Map<String, Object> storedEcrAuth = child(stored, DOCKER_PROXY, ECR_AUTH);
    for (String key : ECR_SECRET_KEYS) {
      if (PLACEHOLDER.equals(incomingEcrAuth == null ? null : incomingEcrAuth.get(key))) {
        Object storedValue = storedEcrAuth == null ? null : storedEcrAuth.get(key);
        if (storedValue == null) {
          throw new IllegalArgumentException(
              "A stored secret was referenced but none exists for " + key + "; enter it again.");
        }
        if (incomingEcrAuth != null) {
          incomingEcrAuth.put(key, storedValue);
        }
      }
    }
  }

  private static boolean hasPlaceholder(final Map<String, Map<String, Object>> attributes) {
    Map<String, Object> auth = child(attributes, HTTPCLIENT, AUTHENTICATION);
    for (String key : SECRET_KEYS) {
      if (PLACEHOLDER.equals(auth == null ? null : auth.get(key))) {
        return true;
      }
    }

    Map<String, Object> ecrAuth = child(attributes, DOCKER_PROXY, ECR_AUTH);
    for (String key : ECR_SECRET_KEYS) {
      if (PLACEHOLDER.equals(ecrAuth == null ? null : ecrAuth.get(key))) {
        return true;
      }
    }

    return false;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> child(
      final Map<String, Map<String, Object>> attributes,
      final String section,
      final String name)
  {
    Map<String, Object> sectionMap = attributes == null ? null : attributes.get(section);
    Object value = sectionMap == null ? null : sectionMap.get(name);
    return value instanceof Map ? (Map<String, Object>) value : null;
  }

  private static String remoteUrl(final Map<String, Map<String, Object>> attributes) {
    Map<String, Object> proxy = attributes == null ? null : attributes.get(PROXY);
    Object url = proxy == null ? null : proxy.get(REMOTE_URL);
    return url instanceof String ? (String) url : null;
  }
}
