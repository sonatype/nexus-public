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
package org.sonatype.nexus.internal.atlas;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * {@link ZipOutputStream} which has {@link DeflaterOutputStream#syncFlush} exposed.
 *
 * @since 2.7
 */
public class FlushableZipOutputStream
    extends ZipOutputStream
{
  private boolean syncFlush;

  public FlushableZipOutputStream(final OutputStream out) {
    super(out);
  }

  public void setSyncFlush(final boolean syncFlush) {
    this.syncFlush = syncFlush;
  }

  /**
   * Copied (unmodified sans formatting) from {@link DeflaterOutputStream#flush()}.
   */
  public void flush() throws IOException {
    if (syncFlush && !def.finished()) {
      int len = 0;
      while ((len = def.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH)) > 0) {
        out.write(buf, 0, len);
        if (len < buf.length) {
          break;
        }
      }
    }
    out.flush();
  }
}
