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
package org.sonatype.nexus.client.core.condition.internal;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.util.Collection;

/******
 * Copied from org.sonatype.aether:aether-api:1.13.1
 ******/

/**
 * A constraint on versions for a dependency. A constraint can either consist of one or more version ranges or a single
 * version. In the first case, the constraint expresses a hard requirement on a version matching one of its ranges. In
 * the second case, the constraint expresses a soft requirement on a specific version (i.e. a recommendation).
 *
 * @author Benjamin Bentmann
 */
public interface VersionConstraint
{

  /**
   * Gets the version ranges of this constraint.
   *
   * @return The version ranges, may be empty but never {@code null}.
   */
  Collection<VersionRange> getRanges();

  /**
   * Gets the version recommended by this constraint.
   *
   * @return The recommended version or {@code null} if none.
   */
  Version getVersion();

  /**
   * Determines whether the specified version satisfies this constraint. In more detail, a version satisfies this
   * constraint if it matches at least one version range or if this constraint has no version ranges at all and the
   * specified version equals the version recommended by the constraint.
   *
   * @param version The version to test, must not be {@code null}.
   * @return {@code true} if the specified version satisfies this constraint, {@code false} otherwise.
   */
  boolean containsVersion(Version version);

}
