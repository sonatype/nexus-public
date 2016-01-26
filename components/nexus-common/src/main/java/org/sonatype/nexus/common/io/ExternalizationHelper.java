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
package org.sonatype.nexus.common.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutput;

import javax.annotation.Nullable;

/**
 * Helper methods for externalizing primitives to {@link ObjectOutput} instances.
 *
 * @since 3.0
 */
public class ExternalizationHelper
{
  private ExternalizationHelper() {
    // empty
  }

  /**
   * Writes a possibly null {@link Long} to an {@link DataOutput}.
   *
   * @see #readNullableLong(DataInput)
   */
  public static void writeNullableLong(DataOutput out, Long value) throws IOException {
    out.writeBoolean(value != null);
    if (value != null) {
      out.writeLong(value);
    }
  }

  @Nullable
  public static Long readNullableLong(DataInput in) throws IOException {
    if (in.readBoolean()) {
      return in.readLong();
    }
    return null;
  }

  /**
   * Writes a possibly null {@link String} to an {@link DataOutput}.
   *
   * @see #readNullableString(DataInput)
   */
  public static void writeNullableString(DataOutput out, String value) throws IOException {
    out.writeBoolean(value != null);
    if (value != null) {
      out.writeUTF(value);
    }
  }

  @Nullable
  public static String readNullableString(DataInput in) throws IOException {
    if (in.readBoolean()) {
      return in.readUTF();
    }
    return null;
  }
}
