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
package org.sonatype.nexus.orient;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import com.google.common.primitives.Ints;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link RecordIdObfuscator} implementations.
 *
 * @since 3.0
 */
public abstract class RecordIdObfuscatorSupport
    extends ComponentSupport
    implements RecordIdObfuscator
{
  /**
   * @see #doEncode(OClass, ORID)
   */
  @Override
  public String encode(final OClass type, final ORID rid) {
    checkNotNull(type);
    checkNotNull(rid);

    log.trace("Encoding: {}->{}", type, rid);
    try {
      return doEncode(type, rid);
    }
    catch (Exception e) {
      log.error("Failed to encode: {}->{}", type, rid);
      throw Throwables.propagate(e);
    }
  }

  /**
   * @see #encode(OClass, ORID)
   */
  protected abstract String doEncode(final OClass type, final ORID rid) throws Exception;

  /**
   * @see #doDecode(OClass, String)
   */
  @Override
  public ORID decode(final OClass type, final String encoded) {
    checkNotNull(type);
    checkNotNull(encoded);

    log.trace("Decoding: {}->{}", type, encoded);
    ORID rid;
    try {
      rid = doDecode(type, encoded);
    }
    catch (Exception e) {
      log.error("Failed to decode: {}->{}", type, encoded);
      throw Throwables.propagate(e);
    }

    // ensure rid points to the right type
    checkArgument(Ints.contains(type.getClusterIds(), rid.getClusterId()),
        "Invalid RID '%s' for class: %s", rid, type);

    return rid;
  }

  /**
   * @see #decode(OClass, String)
   */
  protected abstract ORID doDecode(final OClass type, final String encoded) throws Exception;
}
