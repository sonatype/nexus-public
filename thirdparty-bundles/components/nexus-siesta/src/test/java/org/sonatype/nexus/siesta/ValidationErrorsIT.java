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

import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.sonatype.nexus.rest.ValidationErrorXO;

import org.junit.Test;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.rest.MediaTypes.VND_VALIDATION_ERRORS_V1_JSON_TYPE;
import static org.sonatype.nexus.rest.MediaTypes.VND_VALIDATION_ERRORS_V1_XML_TYPE;

/**
 * Validation error response handling tests.
 */
public class ValidationErrorsIT
    extends SiestaTestSupport
{
  @Test
  public void put_multiple_manual_validations_XML() throws Exception {
    put_multiple_manual_validations(APPLICATION_XML_TYPE, VND_VALIDATION_ERRORS_V1_XML_TYPE);
  }

  @Test
  public void put_multiple_manual_validations_JSON() throws Exception {
    put_multiple_manual_validations(APPLICATION_JSON_TYPE, VND_VALIDATION_ERRORS_V1_JSON_TYPE);
  }

  private void put_multiple_manual_validations(final MediaType... mediaTypes) throws Exception {
    UserXO sent = new UserXO();

    Response response = client().target(url("validationErrors/manual/multiple")).request()
        .accept(mediaTypes)
        .put(Entity.entity(sent, mediaTypes[0]), Response.class);

    assertThat(response.getStatusInfo(), is(equalTo((StatusType)Status.BAD_REQUEST)));
    assertThat(response.getMediaType(), is(equalTo(mediaTypes[1])));

    List<ValidationErrorXO> errors = response.readEntity(new GenericType<List<ValidationErrorXO>>() {});
    assertThat(errors, hasSize(2));
  }

  @Test
  public void put_single_manual_validation_XML() throws Exception {
    put_single_manual_validation(APPLICATION_XML_TYPE, VND_VALIDATION_ERRORS_V1_XML_TYPE);
  }

  @Test
  public void put_single_manual_validation_JSON() throws Exception {
    put_single_manual_validation(APPLICATION_JSON_TYPE, VND_VALIDATION_ERRORS_V1_JSON_TYPE);
  }

  private void put_single_manual_validation(final MediaType... mediaTypes) throws Exception {
    UserXO sent = new UserXO();

    Response response = client().target(url("validationErrors/manual/single")).request()
        .accept(mediaTypes)
        .put(Entity.entity(sent, mediaTypes[0]), Response.class);

    assertThat(response.getStatusInfo(), is(equalTo((StatusType)Status.BAD_REQUEST)));
    assertThat(response.getMediaType(), is(equalTo(mediaTypes[1])));

    List<ValidationErrorXO> errors = response.readEntity(new GenericType<List<ValidationErrorXO>>() {});
    assertThat(errors, hasSize(1));
  }
}
