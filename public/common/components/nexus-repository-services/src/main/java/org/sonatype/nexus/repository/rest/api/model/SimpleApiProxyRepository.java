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
package org.sonatype.nexus.repository.rest.api.model;

import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.repository.types.ProxyType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API Proxy Repository for simple formats which do not have custom attributes for proxies.
 *
 * @since 3.20
 */
public class SimpleApiProxyRepository
    extends AbstractApiRepository
{
  @NotNull
  protected final StorageAttributes storage;

  protected final CleanupPolicyAttributes cleanup;

  @NotNull
  protected final ProxyAttributes proxy;

  @NotNull
  protected final NegativeCacheAttributes negativeCache;

  @NotNull
  protected final HttpClientAttributes httpClient;

  @Schema(description = "The name of the routing rule assigned to this repository")
  protected final String routingRuleName;

  protected final ReplicationAttributes replication;

  /*
   * Firewall attributes are populated cross-format by {@link ApiRepositoryAdapter#adaptDecorated}
   * after the per-format adapter constructs the proxy DTO. Keeping it as a settable property
   * (instead of a constructor parameter) means per-format subclasses don't need to thread the
   * firewall field through their own constructors. Jackson deserialization populates it via the
   * {@code @JsonProperty} setter; serialization uses the getter.
   */
  protected FirewallAttributes firewall;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public SimpleApiProxyRepository(
      @JsonProperty("name") final String name,
      @JsonProperty("format") final String format,
      @JsonProperty("url") final String url,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final StorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("proxy") final ProxyAttributes proxy,
      @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
      @JsonProperty("httpClient") final HttpClientAttributes httpClient,
      @JsonProperty("routingRuleName") final String routingRuleName,
      @JsonProperty("replication") @JsonInclude(value = Include.NON_EMPTY,
          content = Include.NON_NULL) final ReplicationAttributes replication)
  {
    super(name, format, ProxyType.NAME, url, online);
    this.storage = storage;
    this.cleanup = cleanup;
    this.proxy = proxy;
    this.negativeCache = negativeCache;
    this.httpClient = httpClient;
    this.routingRuleName = routingRuleName;
    this.replication = replication;
  }

  public StorageAttributes getStorage() {
    return storage;
  }

  public CleanupPolicyAttributes getCleanup() {
    return cleanup;
  }

  public ProxyAttributes getProxy() {
    return proxy;
  }

  public NegativeCacheAttributes getNegativeCache() {
    return negativeCache;
  }

  public HttpClientAttributes getHttpClient() {
    return httpClient;
  }

  public String getRoutingRuleName() {
    return routingRuleName;
  }

  public ReplicationAttributes getReplication() {
    return replication;
  }

  // NON_NULL (not the replication field's NON_EMPTY/content=NON_NULL) because FirewallAttributes
  // is a plain single-field POJO, not a container: NON_EMPTY's content-level check is a no-op here,
  // and Jackson's default POJO isEmpty() would not treat a non-null FirewallAttributes with a null
  // mode as "empty" anyway. NON_NULL says exactly what is intended: omit only when no firewall
  // attributes were ever set on this repository (fromConfiguration() returns null in that case).
  // This holds because FirewallAttributes.fromConfiguration() is the sole producer of non-null
  // FirewallAttributes instances in production, and it returns null for every "not configured"
  // case (null Configuration, missing/empty firewall attributes map, null mode, or an unrecognised
  // mode value) rather than a FirewallAttributes with a null mode inside it.
  @JsonInclude(Include.NON_NULL)
  public FirewallAttributes getFirewall() {
    return firewall;
  }

  @JsonProperty("firewall")
  public void setFirewall(final FirewallAttributes firewall) {
    this.firewall = firewall;
  }
}
