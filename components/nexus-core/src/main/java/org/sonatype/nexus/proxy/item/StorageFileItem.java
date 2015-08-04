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

/**
 * A file item, it has content and other properties typical to plain files.
 */
public interface StorageFileItem
    extends StorageItem
{
  /**
   * The digest sha1 key used in item context and attributes.
   */
  public static final String DIGEST_SHA1_KEY = "digest.sha1";

  /**
   * The digest md5 key used in item context and attributes. @deprecated MD5 is deprecated, use SHA1.
   */
  @Deprecated
  public static final String DIGEST_MD5_KEY = "digest.md5";

  /**
   * Returns the file content length in bytes, or {@link ContentLocator#UNKNOWN_LENGTH} if unknown. Shortcut method for
   * {@link ContentLocator#getLength()}.
   */
  long getLength();

  /**
   * Returns the MIME type of the file content, never {@code null}. Shortcut method for
   * {@link ContentLocator#getMimeType()}.
   */
  String getMimeType();

  /**
   * Returns {@code true} if this item's content might be requested multiple times (by calling {@link #getInputStream()}
   * for example). Shortcut method for {@link ContentLocator#isReusable()}.
   */
  boolean isReusableStream();

  /**
   * Returns the content as opened ready to read input stream. It has to be closed by the caller explicitly. Shortcut
   * method for {@link ContentLocator#getContent()}.
   */
  InputStream getInputStream() throws IOException;

  /**
   * Sets the {@link ContentLocator} (that provides the actual content of this file). Passed in value cannot be
   * {@code null}.
   */
  void setContentLocator(ContentLocator locator);

  /**
   * Returns the {@link ContentLocator} of this file item, never {@code null}.
   */
  ContentLocator getContentLocator();

  /**
   * Returns the ID of the {@link ContentGenerator} used to generate content for this item. This method returns
   * {@code null} if method {@link #isContentGenerated()} returns {@code false}, hence, if the item is not generated.
   */
  String getContentGeneratorId();

  /**
   * Sets the {@link ContentGenerator} to be used to generate content for this item. Passed in value might be
   * {@code null}, in which case you "demote", remove an associated generator from this file item. If non null, the
   * {@link ContentGenerator} with given ID will be used to generate content.
   */
  void setContentGeneratorId(String contentGeneratorId);

  /**
   * Returns {@code true} if this file item content is generated content (is not static, coming from storage).
   */
  boolean isContentGenerated();
}
