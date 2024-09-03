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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link Secret} serializer
 * <p>
 * Stores the secret id if the system has migrated, otherwise stores the legacy encrypted value
 */
public class SecretSerializer
    extends StdSerializer<Secret>
{
  public SecretSerializer() {
    super(Secret.class);
  }

  @Override
  public void serialize(
      final Secret secret,
      final JsonGenerator jsonGenerator,
      final SerializerProvider serializerProvider)
      throws IOException
  {
    if (secret != null) {
      //if the secret is not null we just write the id, in the worst case will be the legacy value
      jsonGenerator.writeString(secret.getId());
    }
    else {
      //if the secret is null we write null
      jsonGenerator.writeNull();
    }
  }
}
