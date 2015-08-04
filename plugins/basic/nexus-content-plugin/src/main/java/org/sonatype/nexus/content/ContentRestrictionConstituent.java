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
package org.sonatype.nexus.content;

import javax.servlet.ServletRequest;

import org.sonatype.nexus.content.internal.ContentAuthenticationFilter;

/**
 * Allows components to configure {code}/content{code} restriction to be enabled or not.
 *
 * If one or more components has content restriction enabled, then the content authentication is restricted.
 *
 * @see ContentAuthenticationFilter
 * @since 2.1
 */
public interface ContentRestrictionConstituent
{
  /**
   * Determine if content restriction is required for the given request.
   */
  boolean isContentRestricted(final ServletRequest request);
}
