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
 * An evaluable condition.
 */
public interface Condition
    extends Evaluable
{

  /**
   * Binds (eventual) resources used by condition. Before binding, condition should not be used.
   * <p/>
   * Calling this method multiple times should not fail, eventually should log a warning.
   *
   * @return itself, for fluent api usage
   */
  Condition bind();

  /**
   * Releases (eventual) resources used by condition. After releasing, condition should not be used until not binding
   * it again.
   * <p/>
   * Calling this method multiple times should not fail, eventually should log a warning.
   *
   * @return itself, for fluent api usage
   */
  Condition release();

}
