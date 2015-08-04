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
package org.sonatype.security.rest.configuration;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.realms.tools.AbstractStaticSecurityResource;
import org.sonatype.security.realms.tools.StaticSecurityResource;

/**
 * A StaticSecurityResource that contributes static privileges and roles to the XML Realms.
 *
 * @author bdemers
 */
@Singleton
@Typed(StaticSecurityResource.class)
@Named("SecurityRestStaticSecurityResource")
public class SecurityRestStaticSecurityResource
    extends AbstractStaticSecurityResource
    implements StaticSecurityResource
{
  /*
   * (non-Javadoc)
   * @see org.sonatype.security.realms.tools.AbstractStaticSecurityResource#getResourcePath()
   */
  protected String getResourcePath() {
    return "/META-INF/security/static-security-rest.xml";
  }
}
