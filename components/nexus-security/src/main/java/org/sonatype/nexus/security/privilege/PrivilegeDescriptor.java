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
package org.sonatype.nexus.security.privilege;

import java.util.List;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.privilege.rest.ApiPrivilege;
import org.sonatype.nexus.security.privilege.rest.ApiPrivilegeRequest;

import org.apache.shiro.authz.Permission;

/**
 * Privilege descriptor.
 */
public interface PrivilegeDescriptor<T extends ApiPrivilege, Y extends ApiPrivilegeRequest>
{
  String getType();

  String getName();

  Permission createPermission(CPrivilege privilege);

  List<FormField> getFormFields();

  T createApiPrivilegeImpl(Privilege privilege);

  void validate(Y apiPrivilege);
}
