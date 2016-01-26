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

import java.math.BigInteger;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.RandomBytesGenerator;
import org.sonatype.nexus.security.authc.apikey.ApiKeyFactory;

import com.google.common.base.Charsets;
import org.apache.shiro.subject.PrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link ApiKeyFactory} that creates random UUID.
 *
 * @since 3.0
 */
@Named("default")
@Singleton
public class DefaultApiKeyFactory
    extends ComponentSupport
    implements ApiKeyFactory
{
  private final RandomBytesGenerator randomBytesGenerator;

  @Inject
  public DefaultApiKeyFactory(final RandomBytesGenerator randomBytesGenerator) {
    this.randomBytesGenerator = checkNotNull(randomBytesGenerator);
  }

  @Override
  public char[] makeApiKey(final PrincipalCollection principals) {
    final String salt = new BigInteger(randomBytesGenerator.generate(4)).toString(32);
    final byte[] code = ("~nexus~default~" + principals + salt).getBytes(Charsets.UTF_8);
    final String apiKey = UUID.nameUUIDFromBytes(code).toString();
    return apiKey.toCharArray();
  }
}
