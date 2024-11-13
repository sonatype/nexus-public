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
package org.sonatype.nexus.crypto.secrets;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * {@link Secret} deserializer
 * <p>
 * wraps the value coming from the json in a {@link Secret} object
 */
public class SecretDeserializer
    extends StdDeserializer<Secret>
{
  private final SecretsFactory secretsFactory;

  public SecretDeserializer(final SecretsFactory secretsFactory) {
    super(Secret.class);
    this.secretsFactory = secretsFactory;
  }

  @Override
  public Secret deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
      throws IOException
  {
    //if the value is null we return null
    if (jsonParser.getCurrentToken() == JsonToken.VALUE_NULL) {
      return null;
    }
    //use factory to parse it , in the worst case we have the legacy secret and decrypt will handle that
    return secretsFactory.from(jsonParser.getText());
  }
}
