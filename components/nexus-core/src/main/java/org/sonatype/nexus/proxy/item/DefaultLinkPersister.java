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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.util.io.StreamSupport;

import com.google.common.base.Charsets;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class DefaultLinkPersister
    implements LinkPersister
{
  private static final Charset LINK_CHARSET = Charsets.UTF_8;

  private static final String LINK_PREFIX = "LINK to ";

  private static final byte[] LINK_PREFIX_BYTES = LINK_PREFIX.getBytes(LINK_CHARSET);

  private final RepositoryItemUidFactory repositoryItemUidFactory;

  @Inject
  public DefaultLinkPersister(final RepositoryItemUidFactory repositoryItemUidFactory) {
    this.repositoryItemUidFactory = checkNotNull(repositoryItemUidFactory);
  }

  public boolean isLinkContent(final ContentLocator locator)
      throws IOException
  {
    final byte[] buf = getLinkPrefixBytes(locator);
    if (buf != null) {
      return Arrays.equals(buf, LINK_PREFIX_BYTES);
    }
    return false;
  }

  public RepositoryItemUid readLinkContent(final ContentLocator locator)
      throws NoSuchRepositoryException, IOException
  {
    if (locator != null) {
      try (final InputStream is = locator.getContent()) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamSupport.copy(is, baos, StreamSupport.BUFFER_SIZE);
        final String linkBody = new String(baos.toByteArray(), LINK_CHARSET);
        final String uidStr = linkBody.substring(LINK_PREFIX.length(), linkBody.length());
        return repositoryItemUidFactory.createUid(uidStr);
      }
    }
    else {
      return null;
    }
  }

  public void writeLinkContent(final StorageLinkItem link, final OutputStream os)
      throws IOException
  {
    try (OutputStream out = os) {
      final String linkBody = LINK_PREFIX + link.getTarget().toString();
      StreamSupport.copy(new ByteArrayInputStream(linkBody.getBytes(LINK_CHARSET)), out, StreamSupport.BUFFER_SIZE);
      out.flush();
    }
  }

  // ==

  /**
   * Reads up first bytes (exactly as many as many makes the {@link #LINK_PREFIX_BYTES}) from ContentLocator's content.
   * It returns byte array of
   * exact size of count, or null (ie. if file is smaller).
   *
   * @param locator the ContentLocator to read from.
   * @return returns byte array read up, or {@code null} if locator is {@code null}, have not enough length.
   */
  protected byte[] getLinkPrefixBytes(final ContentLocator locator)
      throws IOException
  {
    if (locator != null) {
      try (final DataInputStream dis = new DataInputStream(locator.getContent())) {
        final byte[] buf = new byte[LINK_PREFIX_BYTES.length];
        dis.readFully(buf);
        return buf;
      } catch (EOFException e) {
        // content locator has less bytes, neglect it
      }
    }
    return null;
  }

}
