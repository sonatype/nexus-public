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
package org.sonatype.nexus.proxy.repository;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractProxyRepositoryConfiguration
    extends AbstractRepositoryConfiguration
{
  private static final String PROXY_MODE = "proxyMode";

  private static final String REMOTE_STATUS_CHECK_MODE = "remoteStatusCheckMode";

  private static final String ITEM_MAX_AGE = "itemMaxAge";

  public static final String ARTIFACT_MAX_AGE = "artifactMaxAge";

  public static final String METADATA_MAX_AGE = "metadataMaxAge";

  private static final String ITEM_AGING_ACTIVE = "itemAgingActive";

  private static final String AUTO_BLOCK_ACTIVE = "autoBlockActive";

  public static final String FILE_TYPE_VALIDATION = "fileTypeValidation";

  public AbstractProxyRepositoryConfiguration(Xpp3Dom configuration) {
    super(configuration);
  }

  public ProxyMode getProxyMode() {
    return ProxyMode.valueOf(getNodeValue(getRootNode(), PROXY_MODE, ProxyMode.ALLOW.toString()));
  }

  public void setProxyMode(ProxyMode mode) {
    setNodeValue(getRootNode(), PROXY_MODE, mode.toString());
  }


  public boolean isFileTypeValidation() {
    return Boolean.valueOf(getNodeValue(getRootNode(), FILE_TYPE_VALIDATION, "true"));
  }

  public void setFileTypeValidation(boolean doValidate) {
    setNodeValue(getRootNode(), FILE_TYPE_VALIDATION, Boolean.toString(doValidate));
  }

  public RepositoryStatusCheckMode getRepositoryStatusCheckMode() {
    return RepositoryStatusCheckMode.valueOf(getNodeValue(getRootNode(), REMOTE_STATUS_CHECK_MODE,
        RepositoryStatusCheckMode.AUTO_BLOCKED_ONLY.toString()));
  }

  public void setRepositoryStatusCheckMode(RepositoryStatusCheckMode mode) {
    setNodeValue(getRootNode(), REMOTE_STATUS_CHECK_MODE, mode.toString());
  }

  public int getItemMaxAge() {
    return Integer.parseInt(getNodeValue(getRootNode(), ITEM_MAX_AGE, "1440"));
  }

  public void setItemMaxAge(int age) {
    setNodeValue(getRootNode(), ITEM_MAX_AGE, String.valueOf(age));
  }

  public int getArtifactMaxAge() {
    return Integer.parseInt(getNodeValue(getRootNode(), ARTIFACT_MAX_AGE, "1440"));
  }

  public void setArtifactMaxAge(int age) {
    setNodeValue(getRootNode(), ARTIFACT_MAX_AGE, String.valueOf(age));
  }

  public int getMetadataMaxAge() {
    return Integer.parseInt(getNodeValue(getRootNode(), METADATA_MAX_AGE, "1440"));
  }

  public void setMetadataMaxAge(int age) {
    setNodeValue(getRootNode(), METADATA_MAX_AGE, String.valueOf(age));
  }

  public boolean isItemAgingActive() {
    return Boolean.parseBoolean(getNodeValue(getRootNode(), ITEM_AGING_ACTIVE, Boolean.TRUE.toString()));
  }

  public void setItemAgingActive(boolean value) {
    setNodeValue(getRootNode(), ITEM_AGING_ACTIVE, Boolean.toString(value));
  }

  public boolean isAutoBlockActive() {
    return Boolean.parseBoolean(getNodeValue(getRootNode(), AUTO_BLOCK_ACTIVE, Boolean.TRUE.toString()));
  }

  public void setAutoBlockActive(boolean value) {
    setNodeValue(getRootNode(), AUTO_BLOCK_ACTIVE, Boolean.toString(value));
  }
}
