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
package org.sonatype.nexus.security.authz;

/**
 * Declares one authentication realm whose principals must never receive the role granted by the Default Role
 * capability.
 *
 * <p>
 * The capability grants its role to every authenticated subject, which is wrong for an identity whose privileges are
 * scoped by the credential itself: an ambient role leaves it holding more permissions than intended. Register one
 * component per realm to exclude.
 */
public interface DefaultRoleExemption
{
  /**
   * The realm name to exclude, exactly as that realm reports it and as it appears in the principals it issues. A name
   * matching no realm exempts nothing and reports no error, so return the realm's own constant, not a literal.
   */
  String realmName();
}
