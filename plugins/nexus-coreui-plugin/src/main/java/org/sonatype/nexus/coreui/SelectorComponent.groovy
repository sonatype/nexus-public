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

import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.selector.SelectorConfiguration
import org.sonatype.nexus.selector.SelectorFactory
import org.sonatype.nexus.selector.SelectorManager
import org.sonatype.nexus.validation.ConstraintViolationFactory
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

/**
 * Selector {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Selector')
class SelectorComponent
    extends DirectComponentSupport
{

  @Inject
  SelectorManager selectorManager

  @Inject
  ConstraintViolationFactory constraintViolationFactory

  @Inject
  SelectorFactory selectorFactory

  /**
   * @return a list of selectors
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:selectors:read')
  List<SelectorXO> read() {
    return selectorManager.browse().collect { asSelector(it) }
  }

  /**
   * Creates a selector.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:selectors:create')
  @Validate(groups = [Create.class, Default.class])
  SelectorXO create(final @NotNull @Valid SelectorXO selectorXO) {
    selectorFactory.validateSelector(selectorXO.type, selectorXO.expression)
    def configuration = new SelectorConfiguration(
        name: selectorXO.name,
        type: selectorXO.type,
        description: selectorXO.description,
        attributes: ['expression': selectorXO.expression]
    )
    selectorManager.create(configuration)
    return asSelector(configuration)
  }

  /**
   * Updates a selector.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:selectors:update')
  @Validate(groups = [Update.class, Default.class])
  SelectorXO update(final @NotNull @Valid SelectorXO selectorXO) {
    selectorFactory.validateSelector(selectorXO.type, selectorXO.expression)
    selectorManager.update(selectorManager.read(new DetachedEntityId(selectorXO.id)).with {
      description = selectorXO.description
      attributes = ['expression': selectorXO.expression]
      return it
    })
    return selectorXO
  }

  /**
   * Deletes a selector.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:selectors:delete')
  @Validate
  void remove(final @NotEmpty String id) {
    selectorManager.delete(selectorManager.read(new DetachedEntityId(id)))
  }

  /**
   * Retrieve a list of available selector references.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:selectors:read')
  List<ReferenceXO> readReferences() {
    return selectorManager.browse().collect { new ReferenceXO(id: it.name, name: it.name) }
  }

  static SelectorXO asSelector(final SelectorConfiguration configuration) {
    return new SelectorXO(
        id: configuration.entityMetadata.id.value,
        name: configuration.name,
        type: configuration.type,
        description: configuration.description,
        expression: configuration.attributes['expression']
    )
  }
}
