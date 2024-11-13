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
package org.sonatype.nexus.datastore.mybatis;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.LegacyCipherFactory;
import org.sonatype.nexus.crypto.LegacyCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.LegacyCipherFactoryImpl;
import org.sonatype.nexus.security.PasswordHelper;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.sisu.Hidden;
import org.eclipse.sisu.Typed;

/**
 * Database cipher shared between all MyBatis handlers that want to encrypt data at rest.
 *
 * Note this is different from nested password attributes which should be encrypted with
 * {@link PasswordHelper}, this is about encrypting the entire content of a database cell.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
@Typed(PbeCipher.class)
@Hidden // don't publish this to other modules
final class MyBatisCipher
    implements PbeCipher
{
  private PbeCipher pbeCipher;

  @Inject
  MyBatisCipher(final LegacyCipherFactory legacyCipherFactory,
      @Named("${nexus.mybatis.cipher.password:-changeme}") final String password,
      @Named("${nexus.mybatis.cipher.salt:-changeme}") final String salt,
      @Named("${nexus.mybatis.cipher.iv:-0123456789ABCDEF}") final String iv) throws Exception
  {
    this.pbeCipher = legacyCipherFactory.create(password, salt, iv);
  }

  /**
   * Static configuration for testing purposes.
   */
  @VisibleForTesting
  MyBatisCipher() throws Exception {
    this(new LegacyCipherFactoryImpl(new CryptoHelperImpl()), "changeme", "changeme", "0123456789ABCDEF");
  }

  @Override
  public byte[] encrypt(final byte[] bytes) {
    return pbeCipher.encrypt(bytes);
  }

  @Override
  public byte[] decrypt(final byte[] bytes) {
    return pbeCipher.decrypt(bytes);
  }
}
