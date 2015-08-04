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
package org.sonatype.nexus.proxy.item;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.util.DigesterUtils;

/**
 * A content locator that wraps another content locator, but also calculates hash of it while reading it and putting
 * the
 * result into passed in Context.
 *
 * @author cstamas
 */
public class ChecksummingContentLocator
    extends AbstractWrappingContentLocator
{
  private final MessageDigest messageDigest;

  private final String contextKey;

  private final RequestContext context;

  public ChecksummingContentLocator(final ContentLocator content, final MessageDigest messageDigest,
                                    final String contextKey, final RequestContext context)
  {
    super(content);

    this.messageDigest = messageDigest;

    this.contextKey = contextKey;

    this.context = context;
  }

  @Override
  public InputStream getContent()
      throws IOException
  {
    messageDigest.reset(); // in case someone previously called getContent() but didn't complete the hash

    return new DigestCalculatingInputStream(getTarget().getContent(), messageDigest, contextKey, context);
  }

  private static class DigestCalculatingInputStream
      extends DigestInputStream
  {
    private final String contextKey;

    private final RequestContext context;

    public DigestCalculatingInputStream(final InputStream source, final MessageDigest messageDigest,
                                        final String contextKey, final RequestContext context)
        throws IllegalArgumentException
    {
      super(source, messageDigest);

      this.contextKey = contextKey;

      this.context = context;
    }

    public int read()
        throws IOException
    {
      int result = super.read();

      if (result == -1) {
        setHash();
      }

      return result;
    }

    public int read(byte[] b, int off, int len)
        throws IOException
    {
      int result = super.read(b, off, len);

      if (result == -1) {
        setHash();
      }

      return result;
    }

    // ==

    protected synchronized void setHash()
        throws IOException
    {
      if (!context.containsKey(contextKey, false)) {
        context.put(contextKey, DigesterUtils.getDigestAsString(getMessageDigest().digest()));
      }
    }
  }

}
