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
package org.sonatype.nexus.obr.metadata;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.osgi.service.obr.Resource;

/**
 * An {@link Appendable} writer that appends OBR resources to a metadata stream.
 */
public interface ObrResourceWriter
    extends Appendable, Closeable, Flushable
{
  /**
   * Appends the given OBR resource to the underlying metadata stream.
   *
   * @param resource the resource
   */
  public void append(Resource resource)
      throws IOException;

  /**
   * Appends the OBR metadata footer and marks the stream as complete.
   */
  public void complete();
}
