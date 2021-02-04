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
package org.sonatype.nexus.repository.view;

import java.net.URISyntaxException;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.view.ViewUtils.buildUrlWithParameters;

public class ViewUtilsTest
    extends TestSupport
{
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void buildUrlsWithoutParameters() {
    Parameters parameters = new Parameters();

    assertThat(buildUrlWithParameters("http://www.example.com", parameters),
        is("http://www.example.com"));
    assertThat(buildUrlWithParameters("https://www.example.com", parameters),
        is("https://www.example.com"));
  }

  @Test
  public void buildUrlsWithSubpathsWithoutParameters() {
    Parameters parameters = new Parameters();

    assertThat(buildUrlWithParameters("http://www.example.com/foo", parameters),
        is("http://www.example.com/foo"));
    assertThat(buildUrlWithParameters("https://www.example.com/foo", parameters),
        is("https://www.example.com/foo"));
  }

  @Test
  public void buildUrlsWithSingleParameter() {
    Parameters parameters = new Parameters();
    parameters.set("a", "1");

    assertThat(buildUrlWithParameters("http://www.example.com", parameters),
        is("http://www.example.com?a=1"));
    assertThat(buildUrlWithParameters("http://www.example.com/foo", parameters),
        is("http://www.example.com/foo?a=1"));
  }

  @Test
  public void buildUrlsWithMultipleParameters() {
    Parameters parameters = new Parameters();
    parameters.set("a", "1");
    parameters.set("b", "2");

    assertThat(buildUrlWithParameters("http://www.example.com", parameters),
        is("http://www.example.com?a=1&b=2"));
    assertThat(buildUrlWithParameters("http://www.example.com/foo", parameters),
        is("http://www.example.com/foo?a=1&b=2"));
  }

  @Test
  public void buildUrlsWithDuplicateParameters() {
    Parameters parameters = new Parameters();
    parameters.set("a", "1", "2");

    assertThat(buildUrlWithParameters("http://www.example.com", parameters),
        is("http://www.example.com?a=1&a=2"));
    assertThat(buildUrlWithParameters("http://www.example.com/foo", parameters),
        is("http://www.example.com/foo?a=1&a=2"));
  }

  @Test
  public void buildUrlsWithCorrectlyEncodedParameters() {
    Parameters parameters = new Parameters();
    parameters.set("a", "what?");
    parameters.set("b", "F=ma");
    parameters.set("c", "hello world");
    parameters.set("d", "1+2");

    assertThat(buildUrlWithParameters("http://www.example.com", parameters),
        is("http://www.example.com?a=what%3F&b=F%3Dma&c=hello+world&d=1%2B2"));
  }

  @Test
  public void exceptionIsThrownOnInvalidUrl() {
    exception.expect(IllegalArgumentException.class);
    exception.expectCause(any(URISyntaxException.class));
    exception.expectMessage("http://www.test hostname.com");
    exception.expectMessage("test-parameter");
    exception.expectMessage("test-value");

    Parameters parameters = new Parameters();
    parameters.set("test-parameter", "test-value");
    buildUrlWithParameters("http://www.test hostname.com", parameters);
  }
}
