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

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

import com.google.common.base.Strings;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link StorageFileItem}.
 */
public class DefaultStorageFileItem
    extends AbstractStorageItem
    implements StorageFileItem
{
  /**
   * The input stream.
   */
  private transient ContentLocator contentLocator;

  /**
   * File content length.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen. The actual value comes from {@link ContentLocator#getLength()}!
   */
  @Deprecated
  private long length;

  /**
   * File content MIME type.
   * 
   * @deprecated Field left in place for legacy upgrades, to have XStream able to read up serialized item and then have
   *             {@link #upgrade()} to happen. The actual value comes from {@link ContentLocator#getMimeType()}!
   */
  @Deprecated
  private String mimeType;

  public DefaultStorageFileItem(Repository repository, ResourceStoreRequest request, boolean canRead,
                                boolean canWrite, ContentLocator contentLocator)
  {
    super(repository, request, canRead, canWrite);
    setContentLocator(contentLocator);
  }

  public DefaultStorageFileItem(RepositoryRouter router, ResourceStoreRequest request, boolean canRead,
                                boolean canWrite, ContentLocator contentLocator)
  {
    super(router, request, canRead, canWrite);
    setContentLocator(contentLocator);
  }

  @Override
  public long getLength() {
    return getContentLocator().getLength();
  }

  @Override
  public String getMimeType() {
    return getContentLocator().getMimeType();
  }

  @Override
  public boolean isReusableStream() {
    return getContentLocator().isReusable();
  }

  @Override
  public InputStream getInputStream()
      throws IOException
  {
    return getContentLocator().getContent();
  }

  @Override
  public long getModified() {
    if (isContentGenerated()) {
      return System.currentTimeMillis();
    }
    else {
      return super.getModified();
    }
  }

  @Override
  public void setContentLocator(ContentLocator locator) {
    this.contentLocator = checkNotNull(locator);
  }

  @Override
  public ContentLocator getContentLocator() {
    return contentLocator;
  }

  @Override
  public String getContentGeneratorId() {
    if (isContentGenerated()) {
      return getRepositoryItemAttributes().get(ContentGenerator.CONTENT_GENERATOR_ID);
    }
    else {
      return null;
    }
  }

  @Override
  public void setContentGeneratorId(String contentGeneratorId) {
    if (Strings.isNullOrEmpty(contentGeneratorId)) {
      getRepositoryItemAttributes().remove(ContentGenerator.CONTENT_GENERATOR_ID);
    }
    else {
      getRepositoryItemAttributes().put(ContentGenerator.CONTENT_GENERATOR_ID, contentGeneratorId);
    }
  }

  @Override
  public boolean isContentGenerated() {
    return getRepositoryItemAttributes().containsKey(ContentGenerator.CONTENT_GENERATOR_ID);
  }

  // ==

  @Override
  public String toString() {
    if (isContentGenerated()) {
      return String.format("%s (file, contentGenerator=%s)", super.toString(), getContentGeneratorId());
    }
    else {
      return String.format("%s (file)", super.toString());
    }
  }
}
