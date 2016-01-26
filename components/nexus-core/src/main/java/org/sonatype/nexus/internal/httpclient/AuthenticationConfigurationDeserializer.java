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
package org.sonatype.nexus.internal.httpclient;

import java.io.IOException;

import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.security.PasswordHelper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.httpclient.config.AuthenticationConfiguration.TYPES;

/**
 * {@link AuthenticationConfiguration} deserializer.
 *
 * Determines the instance class by the {@code type} field and decrypts sensitive data.
 *
 * @since 3.0
 */
public class AuthenticationConfigurationDeserializer
    extends StdDeserializer<AuthenticationConfiguration>
{
  private final PasswordHelper passwordHelper;

  public AuthenticationConfigurationDeserializer(final PasswordHelper passwordHelper) {
    super(AuthenticationConfiguration.class);
    this.passwordHelper = checkNotNull(passwordHelper);
  }

  @Override
  public AuthenticationConfiguration deserialize(final JsonParser parser, final DeserializationContext context)
      throws IOException
  {
    JsonNode node = parser.readValueAsTree();
    String typeName = node.get("type").textValue();
    Class<? extends AuthenticationConfiguration> type = TYPES.get(typeName);
    checkState(type != null, "Unknown %s type: %s", AuthenticationConfiguration.class.getSimpleName(), typeName);
    AuthenticationConfiguration configuration = parser.getCodec().treeToValue(node, type);
    if (UsernameAuthenticationConfiguration.class.equals(type)) {
      UsernameAuthenticationConfiguration upc = (UsernameAuthenticationConfiguration) configuration;
      upc.setUsername(upc.getUsername());
      upc.setPassword(passwordHelper.decrypt(upc.getPassword()));
    }
    else if (NtlmAuthenticationConfiguration.class.equals(type)) {
      NtlmAuthenticationConfiguration ntc = (NtlmAuthenticationConfiguration) configuration;
      ntc.setUsername(ntc.getUsername());
      ntc.setPassword(passwordHelper.decrypt(ntc.getPassword()));
      ntc.setDomain(ntc.getDomain());
      ntc.setHost(ntc.getHost());
    }
    return configuration;
  }
}
