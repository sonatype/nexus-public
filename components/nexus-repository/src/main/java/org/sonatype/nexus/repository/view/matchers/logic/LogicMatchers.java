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
package org.sonatype.nexus.repository.view.matchers.logic;

import java.util.Arrays;

import org.sonatype.nexus.repository.view.Matcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Logic matcher factory.
 *
 * @since 3.0
 */
public class LogicMatchers
{
  private LogicMatchers() {}

  public static Matcher and(final Matcher... matchers) {
    checkNotNull(matchers);
    checkArgument(matchers.length > 1, "AND requires 2 or more matchers");
    return new AndMatcher(Arrays.asList(matchers));
  }

  public static Matcher or(final Matcher... matchers) {
    checkNotNull(matchers);
    checkArgument(matchers.length > 1, "OR requires 2 or more matchers");
    return new OrMatcher(Arrays.asList(matchers));
  }

  public static Matcher not(final Matcher matcher) {
    return new NotMatcher(matcher);
  }
}
