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
package org.sonatype.nexus.crypto.secrets;

import org.sonatype.nexus.crypto.internal.error.CipherException;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

/**
 * An application secret (e.g. a password persisted with reversible encryption.
 * <p>
 * Marked as {@link JsonIgnoreType} to prevent serialization in case of accidental return.
 */
@JsonIgnoreType
public interface Secret
{
  /**
   * May have latency due to decryption or external secret management.
   */
  char[] decrypt() throws CipherException;

  /**
   * Returns the token id. Note, if the system has not been upgraded this may be a legacy encrypted string for
   * compatibility.
   */
  String getId();
}
