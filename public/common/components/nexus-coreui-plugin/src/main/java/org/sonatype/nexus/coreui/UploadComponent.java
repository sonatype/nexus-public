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
package org.sonatype.nexus.coreui;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.coreui.internal.UploadService;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.repository.upload.UploadDefinition;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Upload {@link DirectComponent}.
 */
@Component
@DirectAction(action = "coreui_Upload")
public class UploadComponent
    extends DirectComponentSupport
{
  private final UploadService uploadService;

  @Autowired
  public UploadComponent(final UploadService uploadService) {
    this.uploadService = checkNotNull(uploadService);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public Collection<UploadDefinition> getUploadDefinitions() {
    return uploadService.getAvailableDefinitions()
        .stream()
        .filter(UploadDefinition::isUiUpload)
        .collect(Collectors.toList()); // NOSONAR
  }
}
