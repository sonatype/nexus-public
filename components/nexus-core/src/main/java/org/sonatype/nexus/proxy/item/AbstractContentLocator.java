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

import static com.google.common.base.Preconditions.checkNotNull;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The support for implementing {@link ContentLocator}s.
 * 
 * @since 2.7.0
 */
public abstract class AbstractContentLocator
    implements ContentLocator
{
  private final String mimeType;
  
  private final long length;

  private final boolean reusable;

  protected AbstractContentLocator(final String mimeType, final boolean reusable, final long length) {
    checkArgument(length > -1 || length == UNKNOWN_LENGTH);
    this.mimeType = checkNotNull(mimeType);
    this.length = length;
    this.reusable = reusable;
  }

  @Override
  public String getMimeType() {
    return mimeType;
  }
  
  @Override
  public long getLength() {
    return length;
  }

  @Override
  public boolean isReusable() {
    return reusable;
  }
}
