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
package org.sonatype.plexus.rest;

import org.sonatype.plexus.rest.resource.RestletResponseCustomizer;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class ResponseCustomizerPlexusResource
    extends SimplePlexusResource
{

  @Override
  public Object get(final Context context, final Request request, final Response response, final Variant variant)
      throws ResourceException
  {
    final Object result = super.get(context, request, response, variant);
    return new CustomStringRepresentation((String) result);
  }

  private static class CustomStringRepresentation
      extends StringRepresentation
      implements RestletResponseCustomizer
  {

    public CustomStringRepresentation(CharSequence text) {
      super(text);
    }

    public void customize(final Response response) {
      addHttpResponseHeader(response, "X-Custom", "foo");
    }

  }

}
