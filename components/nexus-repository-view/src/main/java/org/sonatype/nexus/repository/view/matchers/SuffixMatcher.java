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
package org.sonatype.nexus.repository.view.matchers;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Suffix string matcher.
 *
 * @since 3.0
 */
public class SuffixMatcher
  extends ComponentSupport
  implements Matcher
{
  private final String suffix;

  private final boolean ignoreCase;

  @VisibleForTesting
  public SuffixMatcher(final String suffix, final boolean ignoreCase) {
    this.suffix = checkNotNull(suffix);
    this.ignoreCase = ignoreCase;
  }

  public SuffixMatcher(final String suffix) {
    this(suffix, false);
  }

  public SuffixMatcher ignoreCase(final boolean ignoreCase) {
    return new SuffixMatcher(suffix, ignoreCase);
  }

  @Override
  public boolean matches(final Context context) {
    checkNotNull(context);
    String path = context.getRequest().getPath();
    log.debug("Matching: {} ends-with={} ignore-case: {}", path, suffix, ignoreCase);
    if (ignoreCase) {
      return Strings2.lower(path).endsWith(Strings2.lower(suffix));
    }
    else {
      return path.endsWith(suffix);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "suffix='" + suffix + '\'' +
        ", ignoreCase=" + ignoreCase +
        '}';
  }
}
