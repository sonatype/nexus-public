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
package org.sonatype.plexus.rest.resource;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class PlexusResourceException
    extends ResourceException
{

  /**
   * Generated serial version UID.
   */
  private static final long serialVersionUID = -7465134306020613153L;

  /**
   * The object that will be returned to the client.
   */
  private Object resultObject;

  public PlexusResourceException(int code, String name, String description, String uri, Throwable cause,
                                 Object resultObject)
  {
    super(code, name, description, uri, cause);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(int code, String name, String description, String uri, Object resultObject) {
    super(code, name, description, uri);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(int code, Throwable cause, Object resultObject) {
    super(code, cause);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(int code, Object resultObject) {
    super(code);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(Status status, String description, Throwable cause, Object resultObject) {
    super(status, description, cause);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(Status status, String description, Object resultObject) {
    super(status, description);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(Status status, Throwable cause, Object resultObject) {
    super(status, cause);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(Status status, Object resultObject) {
    super(status);
    this.resultObject = resultObject;
  }

  public PlexusResourceException(Throwable cause, Object resultObject) {
    super(cause);
    this.resultObject = resultObject;
  }

  public Object getResultObject() {
    return resultObject;
  }

}
