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
package org.sonatype.nexus.repository.upload;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.repository.view.PartPayload;

/**
 * An asset being uploaded.
 *
 * @since 3.7
 */
public class AssetUpload
    implements WithUploadField
{
  private Map<String, String> fields = new HashMap<>();

  private PartPayload payload;

  @Override
  public Map<String, String> getFields() {
    return fields;
  }

  public PartPayload getPayload() {
    return payload;
  }

  public void setFields(final Map<String, String> fields) {
    this.fields = fields;
  }

  public void setPayload(final PartPayload payload) {
    this.payload = payload;
  }
}
