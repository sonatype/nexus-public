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
package org.sonatype.nexus.rest.util;

import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import org.restlet.data.Status;

public class EnumUtil
{

  public static <E extends Enum<E>> E valueOf(String name, Class<E> enumClass)
      throws PlexusResourceException
  {
    if (name == null) {
      throw validationError(name, enumClass);
    }
    try {
      return Enum.valueOf(enumClass, name);
    }
    catch (IllegalArgumentException e) {
      throw validationError(name, enumClass);
    }
  }

  private static <E> PlexusResourceException validationError(String name, Class<E> enumClass) {
    ErrorMessage err = new ErrorMessage();
    err.setId("*");
    err.setMsg("No enum const " + enumClass + "." + name);

    ErrorResponse ner = new ErrorResponse();
    ner.addError(err);

    return new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", ner);
  }

}
