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
package org.sonatype.nexus.proxy.attributes.inspectors;

import java.io.InputStream;
import java.security.MessageDigest;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.attributes.AbstractStorageItemInspector;
import org.sonatype.nexus.proxy.item.ChecksummingContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.util.io.StreamSupport;

import static com.google.common.io.ByteStreams.nullOutputStream;

/**
 * The Class DigestCalculatingInspector calculates MD5 and SHA1 digests of a file and stores them into extended
 * attributes.
 *
 * @author cstamas
 */
@Singleton
@Named
public class DigestCalculatingInspector
    extends AbstractStorageItemInspector
{

  /**
   * The digest md5 key.
   *
   * @deprecated MD5 in general is deprecated for file checksum calculation, is collision prone (see Maven 3.0)
   */
  @Deprecated
  public static String DIGEST_MD5_KEY = StorageFileItem.DIGEST_MD5_KEY;

  /**
   * The digest sha1 key.
   */
  public static String DIGEST_SHA1_KEY = StorageFileItem.DIGEST_SHA1_KEY;

  @Override
  public boolean isHandled(final StorageItem item) {
    if (item instanceof StorageFileItem) {
      if (maybeGetFromContext(item)) {
        // we did our job, we "lifted" the digests from context, so no need to recalculate them
        return false;
      }
      else {
        // we need to recalculate those, processing needed
        return true;
      }
    }
    // not a file item
    return false;
  }

  @Override
  public void processStorageItem(final StorageItem item)
      throws Exception
  {
    if (item instanceof StorageFileItem) {
      final StorageFileItem file = (StorageFileItem) item;
      final ChecksummingContentLocator sha1cl =
          new ChecksummingContentLocator(file.getContentLocator(), MessageDigest.getInstance("SHA1"),
              StorageFileItem.DIGEST_SHA1_KEY, item.getItemContext());
      // md5 is deprecated but still calculated
      ChecksummingContentLocator md5cl =
          new ChecksummingContentLocator(sha1cl, MessageDigest.getInstance("MD5"),
              StorageFileItem.DIGEST_MD5_KEY, item.getItemContext());
      try (final InputStream is = md5cl.getContent()) {
        StreamSupport.copy(is, nullOutputStream(), StreamSupport.BUFFER_SIZE);
      }
      // we made sure that above operations will make values into context
      maybeGetFromContext(item);
    }
  }

  // ==

  protected boolean maybeGetFromContext(final StorageItem item) {
    if (item.getItemContext().containsKey(StorageFileItem.DIGEST_SHA1_KEY)) {
      item.getRepositoryItemAttributes().put(DIGEST_SHA1_KEY,
          String.valueOf(item.getItemContext().get(StorageFileItem.DIGEST_SHA1_KEY)));
      // do this one "blindly"
      item.getRepositoryItemAttributes().put(DIGEST_MD5_KEY,
          String.valueOf(item.getItemContext().get(StorageFileItem.DIGEST_MD5_KEY)));
      // we did our job, those were in context
      return true;
    }
    else {
      // no values found in context
      return false;
    }
  }

}
