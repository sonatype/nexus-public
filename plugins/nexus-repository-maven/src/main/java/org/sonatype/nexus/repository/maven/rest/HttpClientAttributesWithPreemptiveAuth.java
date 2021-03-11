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
package org.sonatype.nexus.repository.maven.rest;

import javax.validation.Valid;

import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAttributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * REST API model for describing HTTP connection properties for proxy repositories supporting preemptive
 * authentication.
 *
 * @since 3.30
 */
public class HttpClientAttributesWithPreemptiveAuth
    extends HttpClientAttributes
{
  @Valid
  protected final HttpClientConnectionAuthenticationAttributesWithPreemptive authenticationWithPreemptive;

  @JsonCreator
  public HttpClientAttributesWithPreemptiveAuth(
      @JsonProperty("blocked") final Boolean blocked,
      @JsonProperty("autoBlock") final Boolean autoBlock,
      @JsonProperty("connection") final HttpClientConnectionAttributes connection,
      @JsonProperty("authentication") final HttpClientConnectionAuthenticationAttributesWithPreemptive authentication)
  {
    super(blocked, autoBlock, connection, null);
    this.authenticationWithPreemptive = authentication;
  }

  public HttpClientAttributesWithPreemptiveAuth(
      final HttpClientAttributes httpClientAttributes,
      final HttpClientConnectionAuthenticationAttributesWithPreemptive authentication)
  {
    super(httpClientAttributes.getBlocked(), httpClientAttributes.getAutoBlock(), httpClientAttributes.getConnection(),
        null);
    this.authenticationWithPreemptive = authentication;
  }

  @Override
  public HttpClientConnectionAuthenticationAttributesWithPreemptive getAuthentication() {
    return authenticationWithPreemptive;
  }
}
