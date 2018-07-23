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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class UrlValidatorTest
    extends TestSupport
{
  private final UrlValidator urlValidator = new UrlValidator();

  @Parameter(0)
  public String url;

  @Parameter(1)
  public boolean isValid;

  @Parameters(name = "{index}: URL {0} should be valid: {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"http://example", true},
        {"http://example.example", true},
        {"http://example.s", true},
        {"https://example", true},
        {"https://example:8080", true},
        {"https://example:8080/", true},
        {"http://example.com", true},
        {"http://www.example.com", true},
        {"http://example.com/example", true},
        {"http://example.com/example_example/", true},
        {"http://example.com/example_example_(wikipedia)", true},
        {"http://example.com/example_example_(wikipedia)?q=a", true},
        {"http://example.com/example_example_(wikipedia)_(wikipedia)_blah#cite-1", true},
        {"http://www.example.com/wordpress/?p=364", true},
        {"http://www.example.com/example/?example=one&everything=42&foobar", true},
        {"http://username:password@example.com:8080", true},
        {"http://username@without.slash.example.com", true},
        {"http://username@with.slash.example.com/", true},
        {"http://username@port.without.slash.example.com:8080", true},
        {"http://username@port.with.slash.example.com:8080/", true},
        {"http://username:password@without.slash.example.com", true},
        {"http://username:password@with.slash.example.com/", true},
        {"http://localhost", true},
        {"http://1.1.1.1", true},
        {"http://2.2.2.2/", true},
        {"http://3.3.3.3:80", true},
        {"http://4.4.4.4:8080/", true},
        {"http://127.0.0.1", true},
        {"http://10.0.0.2", true},
        {"http://172.16.1.2", true},
        {"http://example.com/fragments/#&example=hash&amp", true},
        {"http://example.com/?q=URL%20with-encoded%20characters", true},
        {"http://42.net", true},
        {"http://123456789", true},
        // valid IPv6 addresses
        {"http://[2001:0db8:0000:0000:0000:ff00:0042:8329]", true},
        {"http://[2001:db8:0:0:0:ff00:42:8329]", true},
        {"http://[2001:db8::ff00:42:8329]", true},
        {"http://[0000:0000:0000:0000:0000:0000:0000:0001]", true},
        {"http://[::1]", true},

        // missing protocol scheme
        {"www", false},
        {"www.example.com", false},
        {"example.com", false},
        // missing domain
        {"http://", false},
        {"https://", false},
        {"http://.", false},
        {"http://..", false},
        {"http://../", false},
        {"http://?", false},
        {"http://??", false},
        {"http://??/", false},
        {"http://#", false},
        {"http://##", false},
        {"http://##/", false},
        // unsupported protocol scheme
        {"rdar://example", false},
        {"h://example", false},
        {"ftp://example.com/", false},
        {"ftps://example.com/", false},
        {"mailto:example.com/", false},
        {"mailto:example@example.com/", false},
        // other reasons
        {"", false},
        {"http://foo.bar?q=Spaces should be encoded", false},
        {"//", false},
        {"//example", false},
        {"///example", false},
        {"///", false},
        {"http:///example", false},
        {"://", false},
        {"http://-leading-slash-is.invalid/", false},
        {"http://trailing-slash-is-.invalid/", false},
        {"http://1.1.1.1.1", false},
        {"http://whois.nic.삼성", false}, // java.net.URI doesn't support IDN
    });
  }

  @Test
  public void shouldValidateUrl() throws Exception {
    URI uri = null;
    try {
      uri = new URI(url);
    }
    catch (URISyntaxException ignored) {
      if (isValid) {
        Assert.fail(String.format("Failed to construct URI object using input: '%s'", url));
      }
      else {
        return;
      }
    }

    assertThat(
        String.format("URL '%s' should yield to be %s", url, isValid ? "valid" : "invalid"),
        urlValidator.isValid(uri, null), is(isValid));
  }

}
