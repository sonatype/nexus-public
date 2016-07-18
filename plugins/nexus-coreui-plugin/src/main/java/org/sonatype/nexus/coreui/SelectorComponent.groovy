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
import javax.validation.ConstraintViolationException
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.repository.selector.SelectorPreview
import org.sonatype.nexus.selector.JexlSelector
import org.sonatype.nexus.selector.SelectorConfiguration
import org.sonatype.nexus.selector.SelectorConfigurationStore
import org.sonatype.nexus.validation.ConstraintViolationFactory
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.commons.jexl3.JexlException
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static org.sonatype.nexus.selector.JexlSelector.prettyExceptionMsg

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
  SelectorConfigurationStore store

  @Inject
  ConstraintViolationFactory constraintViolationFactory

  /**
   * @return a list of selectors
   */
  @DirectMethod
  @RequiresPermissions('nexus:selectors:read')
  List<SelectorXO> read() {
    return store.browse().collect { asSelector(it) }
  }

  /**
   * Creates a selector.
   */
  @DirectMethod
  @RequiresAuthentication
  @Validate(groups = [Create.class, Default.class])
  SelectorXO create(final @NotNull @Valid SelectorXO selectorXO) {
    validateExpressionOrThrow(selectorXO.expression)
    def configuration = new SelectorConfiguration(
        name: selectorXO.name,
        type: selectorXO.type,
        description: selectorXO.description,
        attributes: ['expression': selectorXO.expression]
    )
    store.create(configuration)
    return asSelector(configuration)
  }

  /**
   * Updates a selector.
   */
  @DirectMethod
  @RequiresAuthentication
  @Validate(groups = [Update.class, Default.class])
  SelectorXO update(final @NotNull @Valid SelectorXO selectorXO) {
    validateExpressionOrThrow(selectorXO.expression)
    store.update(store.read(new DetachedEntityId(selectorXO.id)).with {
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
  @RequiresAuthentication
  @Validate
  void remove(final @NotEmpty String id) {
    store.delete(store.read(new DetachedEntityId(id)))
  }

  @DirectMethod
  List<ReferenceXO> readContentTypes() {
    return SelectorPreview.ContentType.values().collect { c -> new ReferenceXO(id: c, name: c) }
  }

  /**
   * Retrieve a list of available selector references.
   */
  @DirectMethod
  List<ReferenceXO> readReferences() {
    return store.browse().collect { new ReferenceXO(id: it.name, name: it.name) }
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

  /**
   * Convenience method to validate a JEXL expression or throw an exception on error.
   */
  void validateExpressionOrThrow(String expression) {
    try {
      new JexlSelector(expression)
    }
    catch (Exception e) {
      String msg = e instanceof JexlException ? prettyExceptionMsg(e) : e.getMessage()
      throw new ConstraintViolationException(e.getMessage(),
          Collections.singleton(constraintViolationFactory.createViolation("expression", msg)))
    }
  }
}
