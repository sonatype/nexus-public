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

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.constraints.NotNull

import org.sonatype.nexus.coreui.internal.UploadService
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.rapture.StateContributor
import org.sonatype.nexus.repository.upload.UploadConfiguration
import org.sonatype.nexus.repository.upload.UploadDefinition
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectFormPostMethod
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.commons.fileupload.FileItem
import org.apache.shiro.authz.annotation.RequiresPermissions

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

/**
 * @since 3.next
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Upload')
class UploadComponentComponent
    extends DirectComponentSupport
    implements StateContributor
{

  @Inject
  UploadService uploadService

  @Inject
  UploadConfiguration configuration

  @DirectMethod
  @Timed
  @ExceptionMetered
  Collection<UploadDefinition> getUploadDefinitions() {
    return uploadService.getAvailableDefinitions()
  }

  @DirectFormPostMethod
  @Timed
  @ExceptionMetered
  @Validate
  @RequiresPermissions('nexus:component:add')
  String doUpload(final @NotNull Map<String, String> params, final @NotNull Map<String, FileItem> files) {
    if (!configuration.enabled) {
      throw new WebApplicationException(Response.Status.NOT_FOUND)
    }

    return uploadService.upload(params, files)
  }

  @Override
  @Nullable
  Map<String, Object> getState() {
    return ['upload': configuration.enabled]
  }
}
