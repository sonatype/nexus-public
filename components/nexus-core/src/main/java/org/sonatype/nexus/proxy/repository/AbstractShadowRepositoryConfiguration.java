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

import java.util.List;

import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepository;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class AbstractShadowRepositoryConfiguration
    extends AbstractRepositoryConfiguration
{
  private static final String MASTER_REPOSITORY_ID = "masterRepositoryId";

  private static final String SYNCHRONIZE_AT_STARTUP = "synchronizeAtStartup";

  public AbstractShadowRepositoryConfiguration(Xpp3Dom configuration) {
    super(configuration);
  }

  public String getMasterRepositoryId() {
    return getNodeValue(getRootNode(), MASTER_REPOSITORY_ID, null);
  }

  public void setMasterRepositoryId(String id) {
    setNodeValue(getRootNode(), MASTER_REPOSITORY_ID, id);
  }

  public boolean isSynchronizeAtStartup() {
    return Boolean.parseBoolean(getNodeValue(getRootNode(), SYNCHRONIZE_AT_STARTUP, Boolean.FALSE.toString()));
  }

  public void setSynchronizeAtStartup(boolean val) {
    setNodeValue(getRootNode(), SYNCHRONIZE_AT_STARTUP, Boolean.toString(val));
  }

  @Override
  public ValidationResponse doValidateChanges(ApplicationConfiguration applicationConfiguration,
                                              CoreConfiguration owner, Xpp3Dom config)
  {
    ValidationResponse response = super.doValidateChanges(applicationConfiguration, owner, config);

    // validate master

    List<CRepository> allReposes = applicationConfiguration.getConfigurationModel().getRepositories();

    boolean masterFound = false;

    for (CRepository repository : allReposes) {
      masterFound = masterFound || StringUtils.equals(repository.getId(), getMasterRepositoryId());
    }

    if (!masterFound) {
      String id = ((CRepository) owner.getConfiguration(false)).getId();
      ValidationMessage message =
          new ValidationMessage("shadowOf", "Master repository id=\"" + getMasterRepositoryId()
              + "\" not found for ShadowRepository with id=\"" + id + "\"!",
              "The source nexus repository is not existing.");

      response.addValidationError(message);
    }

    return response;
  }
}
