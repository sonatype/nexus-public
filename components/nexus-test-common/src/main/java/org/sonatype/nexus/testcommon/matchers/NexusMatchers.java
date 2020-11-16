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
package org.sonatype.nexus.testcommon.matchers;

import java.time.OffsetDateTime;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;

public class NexusMatchers
{
  /**
   * Matches the DateTime specified ignoring chronology
   */
  public static Matcher<DateTime> time(final DateTime dateTime) {
    return new TypeSafeMatcher<DateTime>()
    {
      @Override
      public void describeTo(final Description description) {
        description.appendText("a DateTime ").appendText(" ").appendValue(dateTime.toString());
      }

      @Override
      protected boolean matchesSafely(final DateTime item) {
        return dateTime.isEqual(item);
      }
    };
  }

  /**
   * Matches the OffsetDateTime specified ignoring chronology
   */
  public static Matcher<OffsetDateTime> time(final OffsetDateTime dateTime) {
    return new TypeSafeMatcher<OffsetDateTime>()
    {
      @Override
      public void describeTo(final Description description) {
        description.appendText("an OffsetDateTime ").appendText(" ").appendValue(dateTime.toString());
      }

      @Override
      protected boolean matchesSafely(final OffsetDateTime item) {
        return dateTime.isEqual(item);
      }
    };
  }
}
