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
package org.sonatype.nexus.internal.security.apikey;

import java.util.UUID;

import org.sonatype.nexus.security.authc.apikey.ApiKeyFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Default {@link ApiKeyFactory} that creates random UUID v4 API keys.
 * <p>
 * Uses {@link java.security.SecureRandom} internally through {@link UUID#randomUUID()}.
 * This provides 122 bits of entropy from a cryptographically secure random source.
 */
@Primary
@Component
@Qualifier("default")
public class DefaultApiKeyFactory
    implements ApiKeyFactory
{
  @Override
  public char[] makeApiKey() {
    final String apiKey = UUID.randomUUID().toString();
    return apiKey.toCharArray();
  }
}
