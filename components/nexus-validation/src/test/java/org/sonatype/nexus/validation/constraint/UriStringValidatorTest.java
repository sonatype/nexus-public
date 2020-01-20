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
public class UriStringValidatorTest
    extends TestSupport
{
  private final UriStringValidator underTest = new UriStringValidator();

  @Parameter()
  public String uri;

  @Parameter(1)
  public boolean valid;

  @Parameters(name = "{index}: ''{0}'' should be valid: {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"http://example", true},
        {"www", true},
        {"<>invaliduri", false}
    });
  }

  @Test
  public void shouldValidateUriString() throws Exception {
    TestDatum datum = new TestDatum(uri);
    underTest.initialize(getUriStringAnnotation(datum));
    assertThat(
        String.format("'%s' should be %s", uri, valid ? "valid" : "invalid"),
        underTest.isValid(datum.getUri(), null), is(valid));
  }

  private UriString getUriStringAnnotation(final Object obj) throws NoSuchFieldException {
    return obj.getClass().getDeclaredField("uri").getAnnotation(UriString.class);
  }

  public static class TestDatum
  {
    @UriString
    private final String uri;

    public TestDatum(final String uri) {
      this.uri = uri;
    }

    public String getUri() {
      return uri;
    }
  }
}
