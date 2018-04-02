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
package org.sonatype.nexus.security.role;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.internal.AuthorizationManagerImpl;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link RoleNotContainSelf} validator.
 *
 * @since 3.10
 */
@Named
public class RoleNotContainSelfValidator
    extends ConstraintValidatorSupport<RoleNotContainSelf, Object> // Collection<String> expected
{
  private final AuthorizationManager authorizationManager;

  private String idField;

  private String roleIdsField;

  private String message;

  @Override
  public void initialize(RoleNotContainSelf constraintAnnotation) {
    this.idField = constraintAnnotation.id();
    this.roleIdsField = constraintAnnotation.roleIds();
    this.message = constraintAnnotation.message();
  }

  @Inject
  public RoleNotContainSelfValidator(final SecuritySystem securitySystem) throws NoSuchAuthorizationManagerException {
    this.authorizationManager = checkNotNull(securitySystem).getAuthorizationManager(AuthorizationManagerImpl.SOURCE);
  }

  @Override
  public boolean isValid(final Object value, final ConstraintValidatorContext context) {
    log.trace("Validating role doesn't contain itself: {}", value);

    String id = getId(value);
    //this must be a create if there is no id
    if (Strings2.isEmpty(id)) {
      return true;
    }

    Set<String> processedRoleIds = new HashSet<>();
    Collection<String> roleIds = getRoleIds(value);
    for (String roleId : roleIds) {
      if (containsRole(id, roleId, processedRoleIds)) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
      }
    }

    return true;
  }

  private String getId(Object obj) {
    try {
      Method m = obj.getClass().getMethod(idField);

      return (String) m.invoke(obj);
    }
    catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
      log.error("Unable to find method {} in object {}", idField, obj);
      throw new RuntimeException(e);
    }
  }

  private Collection<String> getRoleIds(Object obj) {
    try {
      Method m = obj.getClass().getMethod(roleIdsField);

      return (Collection<String>) m.invoke(obj);
    }
    catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
      log.error("Unable to find method {} in object {}", roleIdsField, obj);
      throw new RuntimeException(e);
    }
  }

  private boolean containsRole(String roleId, String childRoleId, Set<String> processedRoleIds) {
    if (processedRoleIds.contains(childRoleId)) {
      return false;
    }
    processedRoleIds.add(childRoleId);

    if (roleId.equals(childRoleId)) {
      return true;
    }

    try {
      Role childRole = authorizationManager.getRole(childRoleId);

      for (String role : childRole.getRoles()) {
        if (containsRole(roleId, role, processedRoleIds)) {
          return true;
        }
      }
    }
    catch (NoSuchRoleException ignored) {
      log.trace("Missing role {}", childRoleId);
      //just ignore, other validator deals with this
    }

    return false;
  }
}
