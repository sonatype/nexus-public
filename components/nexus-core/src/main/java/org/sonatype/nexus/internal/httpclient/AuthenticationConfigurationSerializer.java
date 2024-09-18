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
import org.sonatype.nexus.httpclient.config.BearerTokenAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import static java.util.Optional.ofNullable;

/**
 * {@link AuthenticationConfiguration} serializer.
 * <p>
 * Encrypts sensitive data.
 *
 * @since 3.0
 */
public class AuthenticationConfigurationSerializer
    extends StdSerializer<AuthenticationConfiguration>
{
  public AuthenticationConfigurationSerializer() {
    super(AuthenticationConfiguration.class);
  }

  @Override
  public void serialize(final AuthenticationConfiguration value,
                        final JsonGenerator jgen,
                        final SerializerProvider provider)
      throws IOException
  {
    serialize(value, jgen);
    jgen.writeEndObject();
  }

  @Override
  public void serializeWithType(final AuthenticationConfiguration value,
                                final JsonGenerator jgen,
                                final SerializerProvider provider,
                                final TypeSerializer typeSer)
      throws IOException
  {
    serialize(value, jgen);
    if (value instanceof UsernameAuthenticationConfiguration) {
      jgen.writeStringField(typeSer.getPropertyName(), UsernameAuthenticationConfiguration.TYPE);
    }
    else if (value instanceof NtlmAuthenticationConfiguration) {
      jgen.writeStringField(typeSer.getPropertyName(), NtlmAuthenticationConfiguration.TYPE);
    }
    else if (value instanceof BearerTokenAuthenticationConfiguration) {
      jgen.writeStringField(typeSer.getPropertyName(), BearerTokenAuthenticationConfiguration.TYPE);
    }
    else {
      // be foolproof, if new type added but this class is not updated
      throw new JsonGenerationException("Unsupported type:" + value.getClass().getName(), jgen);
    }
    jgen.writeEndObject();
  }

  private void serialize(final AuthenticationConfiguration value, final JsonGenerator jgen) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", value.getType());
    jgen.writeBooleanField("preemptive", value.isPreemptive());
    if (value instanceof UsernameAuthenticationConfiguration) {
      UsernameAuthenticationConfiguration upc = (UsernameAuthenticationConfiguration) value;
      jgen.writeStringField("username", upc.getUsername());
      if (upc.getPassword() != null) {
        jgen.writeStringField("password", upc.getPassword().getId());
      }
    }
    else if (value instanceof NtlmAuthenticationConfiguration) {
      NtlmAuthenticationConfiguration ntc = (NtlmAuthenticationConfiguration) value;
      jgen.writeStringField("username", ntc.getUsername());
      if (ntc.getPassword() != null) {
        jgen.writeStringField("password", ntc.getPassword().getId());
      }
      jgen.writeStringField("domain", ntc.getDomain());
      jgen.writeStringField("host", ntc.getHost());
    }
    else if (value instanceof BearerTokenAuthenticationConfiguration) {
      BearerTokenAuthenticationConfiguration btac = (BearerTokenAuthenticationConfiguration) value;
      jgen.writeStringField(BearerTokenAuthenticationConfiguration.TYPE, btac.getBearerToken().getId());
    }
    else {
      // be foolproof, if new type added but this class is not updated
      throw new JsonGenerationException("Unsupported type:" + value.getClass().getName());
    }
  }
}
