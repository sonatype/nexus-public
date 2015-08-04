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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.configuration.ConfigurationException;

/**
 * Throws when an incompatible master is assigned to a shadow repository.
 *
 * @author cstamas
 */
public class IncompatibleMasterRepositoryException
    extends ConfigurationException
{
  private static final long serialVersionUID = -5676236705854300582L;

  private final ShadowRepository shadow;

  private final String masterId;

  public IncompatibleMasterRepositoryException(ShadowRepository shadow, String masterId) {
    this("Master repository ID='" + masterId + "' is incompatible with shadow repository ID='" + shadow.getId()
        + "' because of it's ContentClass", shadow, masterId);
  }

  public IncompatibleMasterRepositoryException(String message, ShadowRepository shadow, String masterId) {
    super(message);

    this.shadow = shadow;

    this.masterId = masterId;
  }

  public ShadowRepository getShadow() {
    return shadow;
  }

  public String getMasterId() {
    return masterId;
  }
}
