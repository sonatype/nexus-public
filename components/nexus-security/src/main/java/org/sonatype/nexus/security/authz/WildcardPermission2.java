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
package org.sonatype.nexus.security.authz;

import java.util.Set;

import com.google.common.base.Joiner;
import org.apache.shiro.authz.permission.WildcardPermission;

/**
 * {@link WildcardPermission} which caches {@link #hashCode} for improved performance.
 *
 * @since 3.0
 */
public class WildcardPermission2
  extends WildcardPermission
{
  private int cachedHash;

  protected WildcardPermission2() {}

  public WildcardPermission2(final String wildcardString) {
    this(wildcardString, DEFAULT_CASE_SENSITIVE);
  }

  public WildcardPermission2(final String wildcardString, final boolean caseSensitive) {
    super(wildcardString, caseSensitive);
  }

  /**
   * Caches {@link #hashCode()} after parts are installed.
   */
  @Override
  protected void setParts(final String wildcardString, final boolean caseSensitive) {
    super.setParts(wildcardString, caseSensitive);
    this.cachedHash = super.hashCode();
  }

  @Override
  public int hashCode() {
    return cachedHash;
  }

  private static final Joiner JOINER = Joiner.on(',');

  /**
   * Customized string representation to avoid {@code []} syntax from sets.
   */
  @Override
  public String toString() {
    StringBuilder buff = new StringBuilder();
    for (Set<String> part : getParts()) {
      if (buff.length() > 0) {
        buff.append(':');
      }
      JOINER.appendTo(buff, part);
    }
    return buff.toString();
  }
}
