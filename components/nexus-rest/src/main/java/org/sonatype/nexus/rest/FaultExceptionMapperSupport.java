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
package org.sonatype.nexus.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ExceptionMapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

// TODO: Consider making this part of ExceptionMapperSupport and avoiding a sub-class variant here for this functionality?

/**
 * Support for {@link ExceptionMapper} implementations which support sending back {@link FaultXO}
 * if request accept type includes one of:
 *
 * <ul>
 * <li>{@link MediaTypes#VND_ERROR_V1_JSON_TYPE}</li>
 * <li>{@link MediaTypes#VND_ERROR_V1_XML_TYPE}</li>
 * </ul>
 *
 * @since 3.0
 */
public abstract class FaultExceptionMapperSupport<E extends Throwable>
    extends ExceptionMapperSupport<E>
{
  private final List<Variant> variants;

  public FaultExceptionMapperSupport() {
    this.variants = Variant.mediaTypes(
        MediaTypes.VND_ERROR_V1_JSON_TYPE,
        MediaTypes.VND_ERROR_V1_XML_TYPE
    ).add().build();
  }

  @Override
  protected Response convert(final E exception, final String id) {
    Response.ResponseBuilder builder = Response.status(getStatus(exception));

    Variant variant = getRequest().selectVariant(variants);
    if (variant != null) {
      builder.type(variant.getMediaType())
          .entity(new FaultXO(id, getMessage(exception)));
    }

    return builder.build();
  }

  protected String getMessage(final E exception) {
    return exception.getMessage();
  }

  protected Status getStatus(final E exception) {
    return Status.INTERNAL_SERVER_ERROR;
  }

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
