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
package org.sonatype.nexus.jsecurity.realms;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.realms.privileges.AbstractPrivilegeDescriptor;
import org.sonatype.security.realms.privileges.PrivilegeDescriptor;
import org.sonatype.security.realms.privileges.PrivilegePropertyDescriptor;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeMethodPropertyDescriptor;
import org.sonatype.security.realms.validator.SecurityValidationContext;

import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named("TargetPrivilegeDescriptor")
public class TargetPrivilegeDescriptor
    extends AbstractPrivilegeDescriptor
    implements PrivilegeDescriptor
{
  public static final String TYPE = "target";

  private final PrivilegePropertyDescriptor methodProperty;

  private final PrivilegePropertyDescriptor targetProperty;

  private final PrivilegePropertyDescriptor repositoryProperty;

  private final PrivilegePropertyDescriptor groupProperty;

  @Inject
  public TargetPrivilegeDescriptor(final @Named("ApplicationPrivilegeMethodPropertyDescriptor") PrivilegePropertyDescriptor methodProperty,
                                   final @Named("TargetPrivilegeRepositoryTargetPropertyDescriptor") PrivilegePropertyDescriptor targetProperty,
                                   final @Named("TargetPrivilegeRepositoryPropertyDescriptor") PrivilegePropertyDescriptor repositoryProperty,
                                   final @Named("TargetPrivilegeGroupPropertyDescriptor") PrivilegePropertyDescriptor groupProperty)
  {
    this.methodProperty = checkNotNull(methodProperty);
    this.targetProperty = checkNotNull(targetProperty);
    this.repositoryProperty = checkNotNull(repositoryProperty);
    this.groupProperty = checkNotNull(groupProperty);
  }

  @Override
  public String getName() {
    return "Repository Target";
  }

  @Override
  public List<PrivilegePropertyDescriptor> getPropertyDescriptors() {
    List<PrivilegePropertyDescriptor> propertyDescriptors = new ArrayList<PrivilegePropertyDescriptor>();

    propertyDescriptors.add(methodProperty);
    propertyDescriptors.add(targetProperty);
    propertyDescriptors.add(repositoryProperty);
    propertyDescriptors.add(groupProperty);

    return propertyDescriptors;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String buildPermission(CPrivilege privilege) {
    if (!TYPE.equals(privilege.getType())) {
      return null;
    }

    String method = getProperty(privilege, ApplicationPrivilegeMethodPropertyDescriptor.ID);
    String repositoryTargetId = getProperty(privilege, TargetPrivilegeRepositoryTargetPropertyDescriptor.ID);
    String repositoryId = getProperty(privilege, TargetPrivilegeRepositoryPropertyDescriptor.ID);
    String groupId = getProperty(privilege, TargetPrivilegeGroupPropertyDescriptor.ID);

    StringBuilder basePermString = new StringBuilder();

    basePermString.append("nexus:target:");
    basePermString.append(repositoryTargetId);
    basePermString.append(":");

    StringBuilder postPermString = new StringBuilder();

    postPermString.append(":");

    if (StringUtils.isEmpty(method)) {
      postPermString.append("*");
    }
    else {
      postPermString.append(method);
    }

    if (!StringUtils.isEmpty(repositoryId)) {
      return basePermString
          + repositoryId
          + postPermString;
    }
    else if (!StringUtils.isEmpty(groupId)) {
      return basePermString
          + groupId
          + postPermString;
    }
    else {
      return basePermString
          + "*"
          + postPermString;
    }
  }

  @Override
  public ValidationResponse validatePrivilege(CPrivilege privilege, SecurityValidationContext ctx, boolean update) {
    ValidationResponse response = super.validatePrivilege(privilege, ctx, update);

    if (!TYPE.equals(privilege.getType())) {
      return response;
    }

    // validate method
    // method is of form ('*' | 'read' | 'create' | 'update' | 'delete' [, method]* )
    // so, 'read' method is correct, but so is also 'create,update,delete'
    // '*' means ALL POSSIBLE value for this "field"
    String method = null;
    String repositoryId = null;
    String repositoryTargetId = null;
    String repositoryGroupId = null;

    for (CProperty property : (List<CProperty>) privilege.getProperties()) {
      if (property.getKey().equals(ApplicationPrivilegeMethodPropertyDescriptor.ID)) {
        method = property.getValue();
      }
      else if (property.getKey().equals(TargetPrivilegeRepositoryPropertyDescriptor.ID)) {
        repositoryId = property.getValue();
      }
      else if (property.getKey().equals(TargetPrivilegeRepositoryTargetPropertyDescriptor.ID)) {
        repositoryTargetId = property.getValue();
      }
      else if (property.getKey().equals(TargetPrivilegeGroupPropertyDescriptor.ID)) {
        repositoryGroupId = property.getValue();
      }
    }

    if (StringUtils.isEmpty(repositoryTargetId)) {
      ValidationMessage message = new ValidationMessage("repositoryTargetId", "Privilege ID '"
          + privilege.getId() + "' requires a repositoryTargetId.", "Repository Target is required.");
      response.addValidationError(message);
    }

    if (!StringUtils.isEmpty(repositoryId) && !StringUtils.isEmpty(repositoryGroupId)) {
      ValidationMessage message = new ValidationMessage(
          "repositoryId",
          "Privilege ID '"
              + privilege.getId()
              + "' cannot be assigned to both a group and repository."
              + "  Either assign a group, a repository or neither (which assigns to ALL repositories).",
          "Cannot select both a Repository and Repository Group.");
      response.addValidationError(message);
    }

    if (StringUtils.isEmpty(method)) {
      response.addValidationError("Method cannot be empty on a privilege!");
    }
    else {
      String[] methods = null;

      if (method.contains(",")) {
        // it is a list of methods
        methods = method.split(",");
      }
      else {
        // it is a single method
        methods = new String[]{method};
      }

      boolean valid = true;

      for (String singlemethod : methods) {
        if (!"create".equals(singlemethod) && !"delete".equals(singlemethod)
            && !"read".equals(singlemethod) && !"update".equals(singlemethod)
            && !"*".equals(singlemethod)) {
          valid = false;

          break;
        }
      }

      if (!valid) {
        ValidationMessage message = new ValidationMessage(
            "method",
            "Privilege ID '" + privilege.getId()
                + "' Method is wrong! (Allowed methods are: create, delete, read and update)",
            "Invalid method selected.");
        response.addValidationError(message);
      }

    }

    return response;
  }
}
