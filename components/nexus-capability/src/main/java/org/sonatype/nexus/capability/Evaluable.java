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
package org.sonatype.nexus.capability;

/**
 * A logical condition.
 *
 * @since capabilities 2.2
 */
public interface Evaluable
{

  /**
   * Whether or not the condition is satisfied.
   *
   * @return true, if condition is satisfied
   */
  boolean isSatisfied();

  /**
   * Describe condition in case that it is satisfied.
   *
   * @return description
   */
  String explainSatisfied();

  /**
   * Describe condition in case that it is not satisfied.
   *
   * @return description
   */
  String explainUnsatisfied();

}
