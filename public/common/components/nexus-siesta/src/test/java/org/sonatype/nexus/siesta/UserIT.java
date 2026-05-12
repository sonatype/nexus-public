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
package org.sonatype.nexus.siesta;

import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests related to happy paths for a resource.
 */
class UserIT
    extends SiestaTestSupport
{
  private static final Logger log = LoggerFactory.getLogger(UserIT.class);

  private void put_happyPath(final MediaType mediaType) {
    UserXO sent = new UserXO().withName(UUID.randomUUID().toString());

    WebTarget target = client().target(url("user"));
    Response response = target.request()
        .accept(mediaType)
        .put(Entity.entity(sent, mediaType), Response.class);
    log.info("Status: {}", response.getStatusInfo());

    assertThat(response.getStatusInfo().getFamily(), equalTo(Family.SUCCESSFUL));

    UserXO received = response.readEntity(UserXO.class);
    assertThat(received, is(notNullValue()));
    assertThat(received.getName(), is(equalTo(sent.getName())));
  }

  @Test
  void put_happyPath_XML() {
    put_happyPath(APPLICATION_XML_TYPE);
  }

  @Test
  void put_happyPath_JSON() {
    put_happyPath(APPLICATION_JSON_TYPE);
  }
}
