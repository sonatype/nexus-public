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
package org.sonatype.nexus.internal.wonderland;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.RandomBytesGenerator;

import com.google.common.io.BaseEncoding;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates random authentication tickets.
 *
 * @since 2.7
 */
@Named
public class AuthTicketGenerator
    extends ComponentSupport
{
  private static final String CPREFIX = "${wonderland.authTicketGenerator";

  private final RandomBytesGenerator randomBytes;

  private final int defaultSize;

  // NOTE: Default size is 66 to make full use of base64 encoding w/o padding

  @Inject
  public AuthTicketGenerator(
      final RandomBytesGenerator randomBytes,
      @Named(CPREFIX + ".defaultSize:-66}") final int defaultSize)
  {
    this.randomBytes = checkNotNull(randomBytes);
    this.defaultSize = defaultSize;
    log.debug("Default size: {}", defaultSize);
  }

  protected String encode(final byte[] bytes) {
    return BaseEncoding.base64().encode(bytes);
  }

  public String generate(final int size) {
    byte[] bytes = randomBytes.generate(size);
    return encode(bytes);
  }

  public String generate() {
    return generate(defaultSize);
  }
}
