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
package org.sonatype.nexus.crypto.internal;

import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.RandomBytesGenerator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RandomBytesGenerator} implementation.
 *
 * @since 3.0
 */
@Named
public class RandomBytesGeneratorImpl
    extends ComponentSupport
    implements RandomBytesGenerator
{
  private final SecureRandom random;

  @Inject
  public RandomBytesGeneratorImpl(final CryptoHelper crypto) {
    this.random = checkNotNull(crypto).createSecureRandom();
  }

  public byte[] generate(final int size) {
    checkArgument(size > 0);
    byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    return bytes;
  }
}
