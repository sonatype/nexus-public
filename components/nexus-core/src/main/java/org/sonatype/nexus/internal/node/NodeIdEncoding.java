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
package org.sonatype.nexus.internal.node;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Locale;

import org.sonatype.nexus.common.text.Strings2;

import com.google.common.hash.Hashing;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Node ID encoding helpers.
 *
 * @since 3.0
 */
public class NodeIdEncoding
{
  /**
   * Encode plain Certificate SHA1 into node-id string.
   */
  public static String nodeIdForSha1(final String input) {
    checkNotNull(input);
    return Strings2.encodeSeparator(input, '-', 8);
  }

  /**
   * Decode node-id into plain SHA1 string.
   */
  public static String sha1ForNodeId(final String input) {
    checkNotNull(input);
    return input.replaceAll("-", "");
  }

  /**
   * Return node-id for certificate.
   */
  public static String nodeIdForCertificate(final Certificate cert) throws CertificateEncodingException {
    checkNotNull(cert);
    String sha1 = Hashing.sha1().hashBytes(cert.getEncoded()).toString().toUpperCase(Locale.US);
    return nodeIdForSha1(sha1);
  }

  /**
   * Return node-id for certificate fingerprint.
   */
  public static String nodeIdForFingerprint(final String fingerprint) {
    checkNotNull(fingerprint);
    String sha1 = fingerprint.replace(":", "");
    return nodeIdForSha1(sha1);
  }
}
