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
package org.sonatype.nexus.security.anonymous;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

// NOTE: This apparently needs to be non-internal for class-loading accessibility

/**
 * Anonymous user {@link PrincipalCollection}.
 *
 * Used to help identify if a {@link Subject} is anonymous.
 *
 * @since 3.0
 */
public class AnonymousPrincipalCollection
    extends SimplePrincipalCollection
{
  public AnonymousPrincipalCollection(final Object principal, final String realmName) {
    super(principal, realmName);
  }
}
