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
package org.sonatype.nexus.templates;

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;

/**
 * A template for creation of various objects.
 *
 * @author cstamas
 */
public interface Template
{
  /**
   * Returns the originating template provider for this template.
   */
  TemplateProvider getTemplateProvider();

  /**
   * The ID of this template.
   */
  String getId();

  /**
   * The human description of this template.
   */
  String getDescription();

  /**
   * Returns true if the supplied object does "fit" the target that this template creates (a la
   * class.isAssignableFrom(target)). The actual meaning of "fit" is left to given template and it's implementation,
   * how to "narrow" the selection.
   */
  boolean targetFits(Object target);

  /**
   * Instantianates this template, creates resulting object (needs cast).
   */
  Object create()
      throws ConfigurationException, IOException;
}
