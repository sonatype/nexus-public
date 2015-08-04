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
package org.sonatype.nexus.proxy.maven.routing;

import java.io.IOException;

/**
 * A writable {@link PrefixSource} implementation. Writable prefix source has capabilities to write and delete next to
 * existing methods.
 *
 * @author cstamas
 * @since 2.4
 */
public interface WritablePrefixSource
    extends PrefixSource
{
  /**
   * Writes entry instances read from passed in {@link PrefixSource} into this entry source.
   */
  void writeEntries(PrefixSource entrySource)
      throws IOException;

  /**
   * Deletes this entry source. After return from this method (and having no exception), the {@link #exists()} method
   * returns {@code true}.
   */
  void delete()
      throws IOException;
}
