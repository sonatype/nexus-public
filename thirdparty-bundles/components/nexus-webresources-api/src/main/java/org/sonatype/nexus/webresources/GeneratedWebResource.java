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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.webresources.WebResource.Prepareable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

/**
 * Support for generated {@link WebResource} implementations.
 *
 * @since 2.8
 */
public abstract class GeneratedWebResource
    implements WebResource, Prepareable
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public boolean isCacheable() {
    return false;
  }

  @Override
  public long getLastModified() {
    return System.currentTimeMillis();
  }

  @Override
  public long getSize() {
    throw new UnsupportedOperationException("Preparation required");
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException("Preparation required");
  }

  @Override
  public WebResource prepare() throws IOException {
    return new DelegatingWebResource(this)
    {
      private final byte[] content;

      {
        content = generate();
        checkState(content != null);
        log.trace("Generated: {}, {} bytes", getPath(), content.length);
      }

      @Override
      public long getSize() {
        return content.length;
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
      }
    };
  }

  protected abstract byte[] generate() throws IOException;
}
