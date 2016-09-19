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
package org.sonatype.nexus.rest.client;

import javax.annotation.Nullable;
import javax.ws.rs.core.Configurable;

/**
 * REST client configuration.
 *
 * @since 3.1
 */
public class RestClientConfiguration
{
  // NOTE: Exposing JAX-RS api here to simplify advanced client semantics

  public interface Customizer
  {
    void apply(Configurable<?> builder);
  }

  public static final RestClientConfiguration DEFAULTS = new RestClientConfiguration(null, false);

  private final Customizer customizer;

  private final boolean useTrustStore;

  private RestClientConfiguration(@Nullable final Customizer customizer, final boolean useTrustStore) {
    this.customizer = customizer;
    this.useTrustStore = useTrustStore;
  }

  public RestClientConfiguration withCustomizer(final Customizer customizer) {
    return new RestClientConfiguration(customizer, this.useTrustStore);
  }

  @Nullable
  public Customizer getCustomizer() {
    return customizer;
  }

  public RestClientConfiguration withUseTrustStore(final boolean useTrustStore) {
    return new RestClientConfiguration(this.customizer, useTrustStore);
  }

  public boolean getUseTrustStore() {
    return useTrustStore;
  }
}
