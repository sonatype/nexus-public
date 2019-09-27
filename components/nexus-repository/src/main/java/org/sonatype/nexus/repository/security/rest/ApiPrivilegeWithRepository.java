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
package org.sonatype.nexus.repository.security.rest;

import java.util.Collection;

import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWithActions;
import org.sonatype.nexus.security.privilege.rest.PrivilegeAction;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @since 3.19
 */
public abstract class ApiPrivilegeWithRepository
    extends ApiPrivilegeWithActions
{
  public static final String FORMAT_KEY = "format";

  public static final String REPOSITORY_KEY = "repository";

  @NotBlank
  @ApiModelProperty(NexusSecurityApiConstants.PRIVILEGE_REPOSITORY_FORMAT_DESCRIPTION)
  private String format;

  @NotBlank
  @ApiModelProperty(NexusSecurityApiConstants.PRIVILEGE_REPOSITORY_DESCRIPTION)
  private String repository;

  public ApiPrivilegeWithRepository(final String privilegeType) {
    super(privilegeType);
  }

  public ApiPrivilegeWithRepository(final String type,
                                    final String name,
                                    final String description,
                                    final boolean readOnly,
                                    final String format,
                                    final String repository,
                                    final Collection<PrivilegeAction> actions)
  {
    super(type, name, description, readOnly, actions);
    this.format = format;
    this.repository = repository;
  }

  public ApiPrivilegeWithRepository(final Privilege privilege) {
    super(privilege);
    format = privilege.getPrivilegeProperty(FORMAT_KEY);
    repository = privilege.getPrivilegeProperty(REPOSITORY_KEY);
  }

  public void setRepository(final String repository) {
    this.repository = repository;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public String getRepository() {
    return repository;
  }

  public String getFormat() {
    return format;
  }

  @Override
  protected Privilege doAsPrivilege(final Privilege privilege) {
    super.doAsPrivilege(privilege);
    privilege.addProperty(FORMAT_KEY, getFormat());
    privilege.addProperty(REPOSITORY_KEY, getRepository());

    return privilege;
  }

  @Override
  protected String doAsActionString() {
    return toBreadActionString();
  }
}
