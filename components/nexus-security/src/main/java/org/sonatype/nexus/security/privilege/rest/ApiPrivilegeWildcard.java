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

import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @since 3.19
 */
public class ApiPrivilegeWildcard
    extends ApiPrivilege
{
  public static final String PATTERN_KEY = "pattern";

  @NotBlank
  @ApiModelProperty(NexusSecurityApiConstants.PRIVILEGE_PATTERN_DESCRIPTION)
  private String pattern;

  /**
   * for deserialization
   */
  private ApiPrivilegeWildcard() {
    super(WildcardPrivilegeDescriptor.TYPE);
  }

  public ApiPrivilegeWildcard(final String name,
                              final String description,
                              final boolean readOnly,
                              final String pattern)
  {
    super(WildcardPrivilegeDescriptor.TYPE, name, description, readOnly);
    this.pattern = pattern;
  }

  public ApiPrivilegeWildcard(final Privilege privilege) {
    super(privilege);
    pattern = privilege.getPrivilegeProperty(PATTERN_KEY);
  }

  public void setPattern(final String pattern) {
    this.pattern = pattern;
  }

  public String getPattern() {
    return pattern;
  }

  @Override
  protected Privilege doAsPrivilege(final Privilege privilege) {
    privilege.addProperty(PATTERN_KEY, pattern);
    return privilege;
  }
}
