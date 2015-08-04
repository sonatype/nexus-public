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

import org.restlet.Context;
import org.restlet.Filter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * A simple restlet that is returned as root, while allowing to recreate roots in applications per application request.
 *
 * @author cstamas
 */
public class RetargetableRestlet
    extends Filter
{
  public RetargetableRestlet(Context context) {
    super(context);
  }

  @Override
  protected int doHandle(Request request, Response response) {
    if (getNext() != null) {
      return super.doHandle(request, response);
    }
    else {
      response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);

      return CONTINUE;
    }

  }
}
