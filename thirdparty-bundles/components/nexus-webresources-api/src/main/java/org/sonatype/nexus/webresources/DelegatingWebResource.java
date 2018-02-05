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
package org.sonatype.nexus.webresources;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Delegating {@link WebResource}.
 *
 * @since 2.8
 */
public class DelegatingWebResource
  implements WebResource
{
  private final WebResource delegate;

  public DelegatingWebResource(final WebResource delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public String getPath() {
    return delegate.getPath();
  }

  @Override
  @Nullable
  public String getContentType() {
    return delegate.getContentType();
  }

  @Override
  public long getSize() {
    return delegate.getSize();
  }

  @Override
  public long getLastModified() {
    return delegate.getLastModified();
  }

  @Override
  public boolean isCacheable() {
    return delegate.isCacheable();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return delegate.getInputStream();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "delegate=" + delegate +
        '}';
  }
}
