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
package org.sonatype.nexus.security.privilege.rest;

import java.util.Collection;

import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;
import org.sonatype.nexus.security.privilege.ApplicationPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.Privilege;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;

/**
 * @since 3.19
 */
public class ApiPrivilegeApplication
    extends ApiPrivilegeWithActions
{
  public static final String DOMAIN_KEY = "domain";

  @NotBlank
  @ApiModelProperty(NexusSecurityApiConstants.PRIVILEGE_DOMAIN_DESCRIPTION)
  private String domain;

  /**
   * for deserialization
   */
  private ApiPrivilegeApplication() {
    super(ApplicationPrivilegeDescriptor.TYPE);
  }

  public ApiPrivilegeApplication(final String name,
                                 final String description,
                                 final boolean readOnly,
                                 final String domain,
                                 final Collection<PrivilegeAction> actions)
  {
    super(ApplicationPrivilegeDescriptor.TYPE, name, description, readOnly, actions);
    this.domain = domain;
  }

  public ApiPrivilegeApplication(final Privilege privilege) {
    super(privilege);
    domain = privilege.getPrivilegeProperty(DOMAIN_KEY);
  }

  public void setDomain(final String domain) {
    this.domain = domain;
  }

  public String getDomain() {
    return domain;
  }

  @Override
  protected Privilege doAsPrivilege(final Privilege privilege) {
    super.doAsPrivilege(privilege);
    privilege.addProperty(DOMAIN_KEY, domain);
    return privilege;
  }

  @Override
  protected String doAsActionString() {
    return toCrudActionString();
  }
}
