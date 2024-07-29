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
package org.sonatype.nexus.httpclient.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Authentication configuration.
 *
 * @since 3.0
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type")
@JsonSubTypes({
    @Type(value = BearerTokenAuthenticationConfiguration.class, name = BearerTokenAuthenticationConfiguration.TYPE),
    @Type(value = NtlmAuthenticationConfiguration.class, name = NtlmAuthenticationConfiguration.TYPE),
    @Type(value = UsernameAuthenticationConfiguration.class, name = UsernameAuthenticationConfiguration.TYPE),
    @Type(value = GoogleAuthenticationConfiguration.class, name = GoogleAuthenticationConfiguration.TYPE),    
})
public abstract class AuthenticationConfiguration
    implements Cloneable
{
  /**
   * Mapping of type-name to type-class used by deserializers. If you add new type of auth config, this map needs
   * to be updated as well.
   */
  public static final Map<String, Class<? extends AuthenticationConfiguration>> TYPES = ImmutableMap.of(
      UsernameAuthenticationConfiguration.TYPE, UsernameAuthenticationConfiguration.class,
      NtlmAuthenticationConfiguration.TYPE, NtlmAuthenticationConfiguration.class,
      BearerTokenAuthenticationConfiguration.TYPE, BearerTokenAuthenticationConfiguration.class,
      GoogleAuthenticationConfiguration.TYPE, GoogleAuthenticationConfiguration.class
  );

  private final String type;

  private boolean preemptive;

  public AuthenticationConfiguration(final String type) {
    this.type = checkNotNull(type);
  }

  public String getType() {
    return type;
  }

  public boolean isPreemptive() {
    return preemptive;
  }

  public void setPreemptive(final boolean preemptive) {
    this.preemptive = preemptive;
  }

  public AuthenticationConfiguration copy() {
    try {
      return (AuthenticationConfiguration) clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
