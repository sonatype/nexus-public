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

import org.codehaus.plexus.util.StringUtils;
import org.restlet.data.Reference;
import org.restlet.data.Request;

/**
 * NOTE: this should NOT be a PLEXUS component. If someone wants to load it by creating a config thats great. Providing
 * a default implementation my cause other issues when trying to load an actual implementation. Classes extending this
 * should be able to just override <code>getContextRoot</code>.
 */
public class DefaultReferenceFactory
    implements ReferenceFactory
{
  /**
   * Centralized, since this is the only "dependent" stuff that relies on knowledge where restlet.Application is
   * mounted (we had a /service => / move).
   *
   * This implementation is still used by tests.
   */
  public Reference getContextRoot(Request request) {
    Reference result = request.getRootRef();

    // fix for when restlet is at webapp root
    if (StringUtils.isEmpty(result.getPath())) {
      result.setPath("/");
    }

    return result;
  }

  protected Reference updateBaseRefPath(Reference reference) {
    if (reference.getBaseRef().getPath() == null) {
      reference.getBaseRef().setPath("/");
    }
    else if (!reference.getBaseRef().getPath().endsWith("/")) {
      reference.getBaseRef().setPath(reference.getBaseRef().getPath() + "/");
    }

    return reference;
  }

  /**
   * See NexusReferenceFactory impl which provides real behavior for this at runtime
   * this impl here mainly for legacy test support.
   */
  public Reference createThisReference(Request request) {
    String uriPart =
        request.getResourceRef().getTargetRef().toString().substring(
            request.getRootRef().getTargetRef().toString().length());

    // trim leading slash
    if (uriPart.startsWith("/")) {
      uriPart = uriPart.substring(1);
    }

    return updateBaseRefPath(new Reference(getContextRoot(request), uriPart));
  }

  public Reference createChildReference(Request request, String childPath) {
    Reference result = createThisReference(request).addSegment(childPath);

    if (result.hasQuery()) {
      result.setQuery(null);
    }

    return result.getTargetRef();
  }

  public Reference createReference(Reference base, String relPart) {
    Reference ref = new Reference(base, relPart);

    return updateBaseRefPath(ref).getTargetRef();
  }

  public Reference createReference(Request base, String relPart) {
    return createReference(getContextRoot(base), relPart);
  }

}
