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

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nexus MIME type rule.
 *
 * @since 3.0
 */
public class MimeRule
{
  private final boolean override;

  private final List<String> mimetypes;

  public MimeRule(final boolean override,
                  final String... mimetypes)
  {
    this(override, Arrays.asList(mimetypes));
  }

  public MimeRule(final boolean override,
                  final List<String> mimetypes)
  {
    checkNotNull(mimetypes, "mimetypes");
    checkArgument(!mimetypes.isEmpty(), "mimetypes");
    this.override = override;
    this.mimetypes = ImmutableList.copyOf(mimetypes);
  }

  /**
   * If {@code true}, only this mapping should be considered, otherwise other sources like Tika might be consulted
   * as well.
   */
  public boolean isOverride() {
    return override;
  }

  /**
   * Returns the applicable MIME types for this mapping in priority order (first most applicable). Returned list is
   * immutable.
   */
  @Nonnull
  public List<String> getMimetypes() {
    return mimetypes;
  }
}
