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

import org.sonatype.nexus.web.BaseUrlHolder;

import org.restlet.data.Reference;
import org.restlet.data.Request;

public interface ReferenceFactory
{

  Reference createChildReference(Request request, String childPath);

  /**
   * @deprecated Use {@link BaseUrlHolder#get()} instead to reference to base URL.
   *             Trying to normalize this so we can cleanup how baseUrl handling is done generally.
   */
  @Deprecated
  Reference getContextRoot(Request request);

  Reference createReference(Reference base, String relPart);

  Reference createReference(Request base, String relPart);

  Reference createThisReference(Request request);

}
