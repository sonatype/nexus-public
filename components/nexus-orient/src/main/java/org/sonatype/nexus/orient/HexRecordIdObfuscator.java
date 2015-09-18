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

import javax.inject.Named;
import javax.inject.Singleton;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;

/**
 * HEX-encoding {@link RecordIdObfuscator}.
 *
 * @since 3.0
 */
@Named("hex")
@Singleton
public class HexRecordIdObfuscator
  extends RecordIdObfuscatorSupport
{
  @Override
  protected String doEncode(final OClass type, final ORID rid) throws Exception {
    return Hex.encode(rid.toStream());
  }

  @Override
  protected ORID doDecode(final OClass type, final String encoded) throws Exception {
    byte[] decoded = Hex.decode(encoded);
    return new ORecordId().fromStream(decoded);
  }
}
