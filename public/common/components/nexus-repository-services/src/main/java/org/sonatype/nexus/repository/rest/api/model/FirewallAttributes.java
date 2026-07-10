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

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.firewall.FirewallMode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API model for Firewall configuration attributes.
 * <p>
 * Firewall configuration controls policy compliance checking and quarantine behavior for proxy repositories.
 * The mode is the single setting controlling whether firewall is active and in what enforcement level.
 * Runtime operational timestamps are managed internally by the firewall subsystem.
 */
public class FirewallAttributes
{
  public static final String FIREWALL_CHILD_ATTRIBUTE_KEY = "firewall";

  public static final String MODE = "mode";

  @Schema(description = "Firewall mode (DISABLED, AUDIT, QUARANTINE, or PCCS)", example = "QUARANTINE")
  protected FirewallMode mode;

  /**
   * Default constructor.
   */
  public FirewallAttributes() {
    this.mode = null;
  }

  /**
   * Constructor for creating FirewallAttributes.
   *
   * @param mode the Firewall mode
   */
  @JsonCreator
  public FirewallAttributes(
      @JsonProperty(MODE) final String mode)
  {
    this.mode = mode != null ? FirewallMode.valueOf(mode) : null;
  }

  public FirewallAttributes(final FirewallMode mode) {
    this.mode = mode;
  }

  public FirewallMode getMode() {
    return mode;
  }

  public void setMode(final FirewallMode mode) {
    this.mode = mode;
  }

  /**
   * Reads the {@code firewall} sub-map from a repository's stored {@link Configuration} and returns
   * the corresponding API model. Returns {@code null} when firewall is not configured — callers can
   * pass the result directly into {@link SimpleApiProxyRepository#setFirewall} or
   * {@link ProxyRepositoryApiRequest#setFirewall}.
   *
   * @param configuration the repository configuration; may be {@code null} (returns {@code null})
   * @return the populated attributes, or {@code null} if firewall isn't configured / mode is unknown
   */
  @Nullable
  public static FirewallAttributes fromConfiguration(@Nullable final Configuration configuration) {
    if (configuration == null) {
      return null;
    }
    NestedAttributesMap firewall = configuration.attributes(FIREWALL_CHILD_ATTRIBUTE_KEY);
    if (firewall == null || firewall.isEmpty()) {
      return null;
    }
    String mode = firewall.get(MODE, String.class);
    if (mode == null) {
      return null;
    }
    try {
      return new FirewallAttributes(FirewallMode.valueOf(mode));
    }
    catch (IllegalArgumentException e) {
      // Unknown mode value (e.g. from a direct DB write) — surface as "not configured" rather
      // than blowing up the API response.
      return null;
    }
  }
}
