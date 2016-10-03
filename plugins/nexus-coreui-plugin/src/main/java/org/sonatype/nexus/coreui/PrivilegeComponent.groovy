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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.privilege.Privilege
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.google.common.collect.Maps
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static com.google.common.base.Preconditions.checkArgument
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE

/**
 * Privilege {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Privilege')
class PrivilegeComponent
    extends DirectComponentSupport
{
  @Inject
  SecuritySystem securitySystem

  @Inject
  List<PrivilegeDescriptor> privilegeDescriptors;

  /**
   * Retrieves privileges.
   * @return a list of privileges
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:privileges:read')
  PagedResponse<PrivilegeXO> read(StoreLoadParameters parameters) {
    return extractPage(parameters, securitySystem.listPrivileges().collect { input ->
      return convert(input)
    })
  }
  
  /**
   * Retrieves privileges and extracts name and id fields only.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:privileges:read')
  List<ReferenceXO> readReferences() {
    return securitySystem.listPrivileges().collect { Privilege privilege ->
      return new ReferenceXO(id: privilege.id, name: privilege.name)
    }
  }

  /**
   * Return only those records matching the given parameters. Will apply a filter and/or sort to client-exposed 
   * properties of {@link PrivilegeXO}: name, description, permission, type.
   */
  @RequiresPermissions('nexus:privileges:read')
  PagedResponse<PrivilegeXO> extractPage(final StoreLoadParameters parameters, final List<PrivilegeXO> xos) {
    log.trace("requesting page with parameters: $parameters and size of: ${xos.size()}") 
    
    checkArgument(!parameters.start || parameters.start < xos.size(), "Requested to skip more results than available")
    
    List<PrivilegeXO> result = xos.collect() 
    if(parameters.filter) {
      def filter = parameters.filter.first().value
      result = xos.findResults{ PrivilegeXO xo ->
        (xo.name.contains(filter) || xo.description.contains(filter) || xo.permission.contains(filter) ||
            xo.type.contains(filter)) ? xo : null
      }  
    }
    
    if(parameters.sort) {
      //assume one sort, not multiple props
      int order = parameters.sort[0].direction == 'ASC' ? 0 : 1
      String sort = parameters.sort[0].property
      result = result.sort { a, b ->
        def desc = a."$sort" <=> b."$sort"
        order ? -desc : desc
      }
    }
    
    int size = result.size()
    int potentialFinalIndex = parameters.start + parameters.limit
    int finalIndex = (size > potentialFinalIndex) ? potentialFinalIndex : size
    Range indices = (parameters.start..<finalIndex)
    return new PagedResponse(size, result[indices])
  }

  /**
   * Retrieve available privilege types.
   * @return a list of privilege types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:privileges:read')
  List<PrivilegeTypeXO> readTypes() {
    return privilegeDescriptors.collect { descriptor ->
      new PrivilegeTypeXO(
          id: descriptor.type,
          name: descriptor.name,
          formFields: descriptor.formFields?.collect { FormFieldXO.create(it) }
      )
    }
  }

  /**
   * Creates a privilege.
   * @param privilege the privilege info
   * @return the created privilege XO
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:privileges:create')
  @Validate(groups = [Create.class, Default.class])
  PrivilegeXO create(final @NotNull @Valid PrivilegeXO privilege) {
    AuthorizationManager authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
    privilege.id = privilege.name; // Use name as privilege ID (note: eventually IDs should go away in favor of names)
    return convert(authorizationManager.addPrivilege(convert(privilege)));
  }

  /**
   * Updates a privilege.
   * @param privilege the privilege info
   * @return the updated privilege XO
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:privileges:update')
  @Validate(groups = [Update.class, Default.class])
  PrivilegeXO update(final @NotNull @Valid PrivilegeXO privilege) {
    AuthorizationManager authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
    return convert(authorizationManager.updatePrivilege(convert(privilege)));
  }

  /**
   * Deletes a privilege, if is not readonly.
   * @param id of privilege to be deleted
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:privileges:delete')
  @Validate
  void remove(@NotEmpty final String id) {
    AuthorizationManager authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
    if (authorizationManager.getPrivilege(id)?.readOnly) {
      throw new IllegalAccessException("Privilege [${id}] is readonly and cannot be deleted")
    }
    authorizationManager.deletePrivilege(id)
  }

  /**
   * Convert privilege to XO.
   */
  @PackageScope
  PrivilegeXO convert(final Privilege input) {
    return new PrivilegeXO(
        id: input.id,
        version: input.version,
        name: input.name != null ? input.name : input.id,
        description: input.description != null ? input.description : input.id,
        type: input.type,
        readOnly: input.readOnly,
        properties: Maps.newHashMap(input.properties),
        permission: input.permission
    )
  }

  /**
   * Convert XO to privilege.
   */
  @PackageScope
  Privilege convert(final PrivilegeXO input) {
    return new Privilege(
        id: input.id,
        version: input.version,
        name: input.name,
        description: input.description,
        type: input.type,
        properties: Maps.newHashMap(input.properties)
    )
  }
}
