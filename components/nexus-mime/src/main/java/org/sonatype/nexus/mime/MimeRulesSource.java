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
package org.sonatype.nexus.mime;

import javax.annotation.Nullable;

/**
 * Interface to provide hints what a given source expects as MIME type based on resource name.
 *
 * @since 2.0
 */
public interface MimeRulesSource
{
  MimeRulesSource NOOP = new MimeRulesSource()
  {
    @Nullable
    @Override
    public MimeRule getRuleForName(final String name) {
      return null;
    }
  };

  /**
   * Returns the forced MIME type that corresponds (should correspond) to given name in the context of given rule
   * source. Returns {@code null} if no rules found.
   */
  @Nullable
  MimeRule getRuleForName(String name);
}
