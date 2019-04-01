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

import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import org.sonatype.nexus.rest.ExceptionMapperSupport;
import org.sonatype.nexus.rest.ValidationErrorXO;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.sonatype.nexus.rest.MediaTypes.VND_VALIDATION_ERRORS_V1_JSON_TYPE;
import static org.sonatype.nexus.rest.MediaTypes.VND_VALIDATION_ERRORS_V1_XML_TYPE;

/**
 * Support class for exception mappers returning {@link Status#BAD_REQUEST} with {@link ValidationErrorXO}s in body.
 *
 * @since 3.0
 */
public abstract class ValidationExceptionMapperSupport<E extends Throwable>
    extends ExceptionMapperSupport<E>
{
  private final List<Variant> variants;

  public ValidationExceptionMapperSupport() {
    this.variants = Variant.mediaTypes(
        VND_VALIDATION_ERRORS_V1_JSON_TYPE,
        VND_VALIDATION_ERRORS_V1_XML_TYPE,
        APPLICATION_JSON_TYPE
        ).add().build();
  }

  @Override
  protected Response convert(final E exception, final String id) {
    final Response.ResponseBuilder builder = Response.status(getStatus(exception));

    final List<ValidationErrorXO> errors = getValidationErrors(exception);
    if (errors != null && !errors.isEmpty()) {
      final Variant variant = getRequest().selectVariant(variants);
      if (variant != null) {
        builder.type(variant.getMediaType())
            .entity(
                new GenericEntity<List<ValidationErrorXO>>(errors)
                {
                  @Override
                  public String toString() {
                    return getEntity().toString();
                  }
                }
            );
      }
    }

    return builder.build();
  }

  protected Status getStatus(final E exception) {
    return Status.BAD_REQUEST;
  }

  protected abstract List<ValidationErrorXO> getValidationErrors(final E exception);

  //
  // Helpers
  //

  private Request request;

  @Context
  public void setRequest(final Request request) {
    this.request = checkNotNull(request);
  }

  protected Request getRequest() {
    checkState(request != null);
    return request;
  }
}
