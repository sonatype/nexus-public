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
package org.sonatype.nexus.test.utils;

import org.sonatype.nexus.test.utils.NexusRequestMatchers.InError;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsRedirecting;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsSuccessful;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.IsSuccessfulCode;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.RedirectLocationMatches;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.RespondsWithStatusCode;
import org.sonatype.nexus.test.utils.NexusRequestMatchers.ResponseTextMatches;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class ResponseMatchers
{

  @Factory
  public static RespondsWithStatusCode respondsWithStatusCode(final int expectedStatusCode) {
    return new RespondsWithStatusCode(expectedStatusCode);
  }

  @Factory
  public static InError inError() {
    return new InError();
  }

  @Factory
  public static IsSuccessful isSuccessful() {
    return new IsSuccessful();
  }

  @Factory
  public static ResponseTextMatches responseText(Matcher<String> matcher) {
    return new ResponseTextMatches(matcher);
  }

  @Factory
  public static IsRedirecting isRedirecting() {
    return new IsRedirecting();
  }

  @Factory
  public static RedirectLocationMatches redirectLocation(Matcher<String> matcher) {
    return new RedirectLocationMatches(matcher);
  }

  @Factory
  public static IsSuccessfulCode isSuccessfulCode() {
    return new IsSuccessfulCode();
  }

}
