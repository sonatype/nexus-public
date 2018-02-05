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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

@Named
@Singleton
@Path("/validationErrors")
public class ValidationErrorsResource
    implements Resource
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  @PUT
  @Path("/manual/multiple")
  @Consumes({APPLICATION_XML, APPLICATION_JSON})
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public UserXO putWithMultipleManualValidations(final UserXO user) {
    log.info("PUT user: {}", user);

    final ValidationErrorsException validationErrors = new ValidationErrorsException();
    if (user.getName() == null) {
      validationErrors.withError("name", "Name cannot be null");
    }
    if (user.getDescription() == null) {
      validationErrors.withError("description", "Description cannot be null");
    }

    if (validationErrors.hasValidationErrors()) {
      throw validationErrors;
    }

    return user;
  }

  @PUT
  @Path("/manual/single")
  @Consumes({APPLICATION_XML, APPLICATION_JSON})
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public UserXO putWithSingleManualValidation(final UserXO user) {
    log.info("PUT user: {}", user);

    if (user.getName() == null) {
      throw new ValidationErrorsException("name", "Name cannot be null");
    }
    if (user.getDescription() == null) {
      throw new ValidationErrorsException("description", "Description cannot be null");
    }

    return user;
  }
}
