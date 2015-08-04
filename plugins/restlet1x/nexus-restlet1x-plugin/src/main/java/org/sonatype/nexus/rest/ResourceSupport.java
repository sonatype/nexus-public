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

import javax.ws.rs.Path;

import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.sisu.goodies.common.Loggers;

import org.slf4j.Logger;

/**
 * Support for {@link PlexusResource} implementations.
 *
 * @since 2.2
 */
public abstract class ResourceSupport
    extends AbstractNexusPlexusResource
{

  protected final Logger log = Loggers.getLogger(getClass());

  private String resourceUri;

  protected ResourceSupport() {
    setAvailable(true);
    setReadable(true);
    setModifiable(true);
    setNegotiateContent(true);
  }

  @Override
  public String getResourceUri() {
    if (resourceUri == null) {
      Path path = getClass().getAnnotation(Path.class);
      if (path != null) {
        resourceUri = path.value();
      }
    }
    return resourceUri;
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

}