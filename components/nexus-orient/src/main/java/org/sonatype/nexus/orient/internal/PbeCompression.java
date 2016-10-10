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
package org.sonatype.nexus.orient.internal;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.PbeCipherFactory;
import org.sonatype.nexus.crypto.PbeCipherFactory.PbeCipher;

import com.orientechnologies.orient.core.compression.OCompression;

/**
 * Implementation of OrientDB's {@code OCompression} interface to provide password-based-encryption of stored records.
 *
 * @since 3.0
 */
@Singleton
@Named(PbeCompression.NAME)
public class PbeCompression
    implements OCompression
{
  public static final String NAME = "pbe";

  private static final String CPREFIX = "${nexus.orient." + NAME;

  private final PbeCipher pbeCipher;

  @Inject
  public PbeCompression(final PbeCipherFactory pbeCipherFactory,
                        @Named(CPREFIX + ".password:-changeme}") final String password,
                        @Named(CPREFIX + ".salt:-changeme}") final String salt,
                        @Named(CPREFIX + ".iv:-0123456789ABCDEF}") final String iv) throws Exception
  {
    this.pbeCipher = pbeCipherFactory.create(password, salt, iv);
  }

  @Override
  public byte[] compress(final byte[] bytes) {
    return pbeCipher.encrypt(bytes);
  }

  @Override
  public byte[] uncompress(final byte[] bytes) {
    return pbeCipher.decrypt(bytes);
  }

  @Override
  public byte[] compress(final byte[] bytes, final int offset, final int length) {
    return pbeCipher.encrypt(Arrays.copyOfRange(bytes, offset, offset + length));
  }

  @Override
  public byte[] uncompress(final byte[] bytes, final int offset, final int length) {
    return pbeCipher.decrypt(Arrays.copyOfRange(bytes, offset, offset + length));
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public OCompression configure(final String options) {
    return this;
  }
}
