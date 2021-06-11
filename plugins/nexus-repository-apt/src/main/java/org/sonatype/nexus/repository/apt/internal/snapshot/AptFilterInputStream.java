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
package org.sonatype.nexus.repository.apt.internal.snapshot;

import java.io.FilterInputStream;
import java.io.IOException;

import org.bouncycastle.bcpg.ArmoredInputStream;

/**
 * @since 3.31
 */
public class AptFilterInputStream
    extends FilterInputStream
{
  private boolean done = false;

  private final ArmoredInputStream armoredIn;

  public AptFilterInputStream(final ArmoredInputStream armoredIn) {
    super(armoredIn);
    this.armoredIn = armoredIn;
  }

  @Override
  public int read() throws IOException {
    if (done) {
      return -1;
    }
    int c = armoredIn.read();
    if (c < 0 || !armoredIn.isClearText()) {
      done = true;
      return -1;
    }
    return c;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    for (int i = 0; i < len; i++) {
      int c = read();
      if (c == -1) {
        return i == 0 ? -1 : i;
      }
      b[off + i] = (byte) c;
    }
    return len;
  }
}
