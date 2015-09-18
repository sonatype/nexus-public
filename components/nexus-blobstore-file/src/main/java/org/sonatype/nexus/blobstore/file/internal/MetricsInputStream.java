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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.sonatype.nexus.blobstore.file.FileOperations.StreamMetrics;

import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CountingInputStream;

/**
 * A utility to collect metrics about the content of an input stream.
 *
 * @since 3.0
 */
public class MetricsInputStream
    extends FilterInputStream
{
  private final MessageDigest messageDigest;

  private final CountingInputStream countingInputStream;

  public MetricsInputStream(final InputStream input) {
    this(new CountingInputStream(input), createSha1());
  }

  private MetricsInputStream(final CountingInputStream countingStream, final MessageDigest messageDigest) {
    super(new DigestInputStream(countingStream, messageDigest));
    this.messageDigest = messageDigest;
    this.countingInputStream = countingStream;
  }

  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

  public String getMessageDigest() {
    return HEX.encode(messageDigest.digest());
  }

  public long getSize() {
    return countingInputStream.getCount();
  }

  public StreamMetrics getMetrics() {
    return new StreamMetrics(getSize(), getMessageDigest());
  }

  private static MessageDigest createSha1() {
    try {
      return MessageDigest.getInstance("SHA1");
    }
    catch (NoSuchAlgorithmException e) {
      // should never happen
      throw Throwables.propagate(e);
    }
  }
}
