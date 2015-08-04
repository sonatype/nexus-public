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
import org.sonatype.nexus.proxy.registry.ContentClass;

/**
 * Thrown when invalid grouping is tried: for example grouping of repositories without same content class.
 *
 * @author cstamas
 */
public class InvalidGroupingException
    extends ConfigurationException
{
  private static final long serialVersionUID = -738329028288324297L;

  public InvalidGroupingException(ContentClass c1, ContentClass c2) {
    super("The content classes are not groupable! '" + c1.getId() + "' and '" + c2.getId()
        + "' are not compatible!");
  }

  public InvalidGroupingException(ContentClass c1) {
    super("There is no repository group implementation that supports this content class '" + c1.getId() + "'!");
  }

  public InvalidGroupingException(String id, String path) {
    super("The group '" + id + "' has a cyclic reference! Path to the cyclic reference: '" + path + "'.");
  }

  /**
   * @since 2.6
   */
  public InvalidGroupingException(final String reason) {
    super(reason);
  }

}
