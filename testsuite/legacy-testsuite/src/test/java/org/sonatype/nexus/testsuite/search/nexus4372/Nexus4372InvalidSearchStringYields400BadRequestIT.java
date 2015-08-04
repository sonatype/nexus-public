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
package org.sonatype.nexus.testsuite.search.nexus4372;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;

public class Nexus4372InvalidSearchStringYields400BadRequestIT
    extends AbstractNexusIntegrationTest
{

  private static String[][] toTest = {
      {"!", "!*"},
      {"]]", "]]*"},
      {"<!", "&lt;!*"},
      // html special chars will be rendered by javascript, payload is expected to be represented as 'plain text', HTML escaped
  };

  @Test
  public void testInvalidSearch()
      throws IOException
  {
    for (int i = 0; i < toTest.length; i++) {
      String[] strings = toTest[i];
      String query = strings[0];
      String expected = strings[1];

      test(query, expected);
    }
  }

  private void test(String query, String expected)
      throws IOException
  {

    String serviceURIpart = "service/local/lucene/search?q=" + URLEncoder.encode(query, "UTF-8");
    log.debug("Testing query {}: {}", query, serviceURIpart);
    String errorPayload = RequestFacade.doGetForText(serviceURIpart,
        respondsWithStatusCode(400));
    log.debug("Received 'Bad Request' error: " + errorPayload);
    MediaType type = MediaType.APPLICATION_XML;
    XStreamRepresentation representation = new XStreamRepresentation(getXMLXStream(), errorPayload, type);

    ErrorResponse payload = (ErrorResponse) representation.getPayload(new ErrorResponse());

    List errors = payload.getErrors();
    assertThat((Collection<?>) errors, hasSize(1));
    ErrorMessage error = (ErrorMessage) errors.get(0);
    String msg = error.getMsg();

    msg = msg.replaceAll(
        "Cannot parse '([^']*)':.*",
        "$1");

    log.debug("msg: " + msg);

    assertThat(msg, equalTo(expected));
  }
}
