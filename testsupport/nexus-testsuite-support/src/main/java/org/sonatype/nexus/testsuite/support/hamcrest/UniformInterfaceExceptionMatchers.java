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
package org.sonatype.nexus.testsuite.support.hamcrest;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * {@link UniformInterfaceException} related hamcrest matchers.
 *
 * @since 2.7
 */
public abstract class UniformInterfaceExceptionMatchers
{

  /**
   * Matches {@link UniformInterfaceException} with a specified status.
   */
  @Factory
  public static Matcher<UniformInterfaceException> exceptionWithStatus(final Status status) {
    return new TypeSafeMatcher<UniformInterfaceException>()
    {

      @Override
      protected boolean matchesSafely(final UniformInterfaceException e) {
        return status.equals(e.getResponse().getClientResponseStatus());
      }

      @Override
      public void describeTo(final Description description) {
        description
            .appendText("a ")
            .appendText(UniformInterfaceException.class.getName())
            .appendText(" with status ")
            .appendValue(status);
      }

      @Override
      protected void describeMismatchSafely(final UniformInterfaceException item,
                                            final Description mismatchDescription)
      {
        mismatchDescription
            .appendText("had status ")
            .appendValue(item.getResponse().getClientResponseStatus());
      }
    };
  }

}
