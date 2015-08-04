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
package org.sonatype.nexus.proxy.storage.local;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ChecksummingContentLocator;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.AbstractContextualizedRepositoryStorage;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract Storage class. It have ID and defines logger. Predefines all write methods to be able to "decorate"
 * StorageItems with attributes if supported.
 *
 * @author cstamas
 */
public abstract class AbstractLocalRepositoryStorage
    extends AbstractContextualizedRepositoryStorage<LocalStorageContext>
    implements LocalRepositoryStorage
{
  /**
   * The wastebasket.
   */
  private final Wastebasket wastebasket;

  /**
   * The default Link persister.
   */
  private final LinkPersister linkPersister;

  /**
   * The MIME support.
   */
  private final MimeSupport mimeSupport;

  protected AbstractLocalRepositoryStorage(final Wastebasket wastebasket, final LinkPersister linkPersister,
                                           final MimeSupport mimeSupport)
  {
    this.wastebasket = checkNotNull(wastebasket);
    this.linkPersister = checkNotNull(linkPersister);
    this.mimeSupport = checkNotNull(mimeSupport);
  }

  protected Wastebasket getWastebasket() {
    return wastebasket;
  }

  protected LinkPersister getLinkPersister() {
    return linkPersister;
  }

  protected MimeSupport getMimeSupport() {
    return mimeSupport;
  }

  // ==

  protected LocalStorageContext getLocalStorageContext(final Repository repository)
      throws LocalStorageException
  {
    try {
      return super.getStorageContext(repository, repository.getLocalStorageContext());
    }
    catch (LocalStorageException e) {
      throw e;
    }
    catch (IOException e) {
      throw new LocalStorageException("Could not update context", e);
    }
  }

  @Override
  protected void updateStorageContext(final Repository repository, final LocalStorageContext context,
                                      final ContextOperation contextOperation)
      throws IOException
  {
    updateContext(repository, context);
  }

  /**
   * Local storage specific WRITE operations into context.
   */
  protected void updateContext(final Repository repository, final LocalStorageContext context)
      throws IOException
  {
    // empty, override if needed
  }

  // ==

  /**
   * Gets the absolute url from base.
   */
  @Override
  public URL getAbsoluteUrlFromBase(Repository repository, ResourceStoreRequest request)
      throws LocalStorageException
  {
    StringBuilder urlStr = new StringBuilder(repository.getLocalUrl());

    if (request.getRequestPath().startsWith(RepositoryItemUid.PATH_SEPARATOR)) {
      urlStr.append(request.getRequestPath());
    }
    else {
      urlStr.append(RepositoryItemUid.PATH_SEPARATOR).append(request.getRequestPath());
    }
    try {
      return new URL(urlStr.toString());
    }
    catch (MalformedURLException e) {
      try {
        return new File(urlStr.toString()).toURI().toURL();
      }
      catch (MalformedURLException e1) {
        throw new LocalStorageException("The local storage has a malformed URL as baseUrl!", e);
      }
    }
  }

  @Override
  public final void deleteItem(Repository repository, ResourceStoreRequest request)
      throws ItemNotFoundException, UnsupportedStorageOperationException, LocalStorageException
  {
    getWastebasket().delete(this, repository, request);
  }

  protected void prepareStorageFileItemForStore(final StorageFileItem item)
      throws LocalStorageException
  {
    try {
      // replace content locator
      ChecksummingContentLocator sha1cl =
          new ChecksummingContentLocator(item.getContentLocator(), MessageDigest.getInstance("SHA1"),
              StorageFileItem.DIGEST_SHA1_KEY, item.getItemContext());

      // md5 is deprecated but still calculated
      ChecksummingContentLocator md5cl =
          new ChecksummingContentLocator(sha1cl, MessageDigest.getInstance("MD5"),
              StorageFileItem.DIGEST_MD5_KEY, item.getItemContext());

      item.setContentLocator(md5cl);
    }
    catch (NoSuchAlgorithmException e) {
      throw new LocalStorageException(
          "The JVM does not support SHA1 MessageDigest or MD5 MessageDigest, that is essential for Nexus. We cannot write to local storage! Please run Nexus on JVM that does provide these MessageDigests.",
          e);
    }
  }
}
