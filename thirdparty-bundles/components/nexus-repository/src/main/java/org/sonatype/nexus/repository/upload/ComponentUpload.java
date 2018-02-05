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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A component being uploaded.
 *
 * @since 3.7
 */
public class ComponentUpload
    implements WithUploadField
{
  private List<AssetUpload> assetUploads = new ArrayList<>();

  private Map<String, String> fields = new HashMap<>();

  public List<AssetUpload> getAssetUploads() {
    return assetUploads;
  }

  @Override
  public Map<String, String> getFields() {
    return fields;
  }

  public void setAssetUploads(final List<AssetUpload> assetUploads) {
    this.assetUploads = assetUploads;
  }

  public void setFields(final Map<String, String> fields) {
    this.fields = fields;
  }
}
