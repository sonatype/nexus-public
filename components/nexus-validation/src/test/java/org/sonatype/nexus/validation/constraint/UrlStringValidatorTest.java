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
package org.sonatype.nexus.validation.constraint;

import java.util.Arrays;
import java.util.Collection;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class UrlStringValidatorTest
    extends TestSupport
{
  private final UrlStringValidator underTest = new UrlStringValidator();

  @Parameter()
  public String url;

  @Parameter(1)
  public boolean valid;

  @Parameters(name = "{index}: ''{0}'' should be valid: {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"http://example", true},
        {"www", false}
    });
  }

  @Test
  public void shouldValidateUrlString() throws Exception {
    TestDatum datum = new TestDatum(url);
    underTest.initialize(getUrlStringAnnotation(datum));
    assertThat(
        String.format("'%s' should be %s", url, valid ? "valid" : "invalid"),
        underTest.isValid(datum.getUrl(), null), is(valid));
  }

  private UrlString getUrlStringAnnotation(final Object obj) throws NoSuchFieldException {
    return obj.getClass().getDeclaredField("url").getAnnotation(UrlString.class);
  }

  public static class TestDatum
  {
    @UrlString
    private final String url;

    public TestDatum(final String url) {
      this.url = url;
    }

    public String getUrl() {
      return url;
    }
  }
}
