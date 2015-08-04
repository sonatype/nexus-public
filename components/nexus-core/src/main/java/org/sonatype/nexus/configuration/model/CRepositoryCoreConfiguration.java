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
package org.sonatype.nexus.configuration.model;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.ExternalConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.repository.LocalStatus;

import static com.google.common.base.Preconditions.checkNotNull;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class CRepositoryCoreConfiguration
    extends AbstractCoreConfiguration<CRepository>
{
  private static final String REPOSITORY_ID_PATTERN = "^[a-zA-Z0-9_\\-\\.]+$";

  private final CRepository repositoryModel;

  private final CRepositoryExternalConfigurationHolderFactory<?> externalConfigurationFactory;

  private ExternalConfiguration<?> externalConfiguration;

  public CRepositoryCoreConfiguration(ApplicationConfiguration configuration, CRepository repositoryModel,
      CRepositoryExternalConfigurationHolderFactory<?> extFactory)
  {
    super(configuration);
    setOriginalConfiguration(repositoryModel);
    this.repositoryModel = checkNotNull(repositoryModel);
    this.externalConfigurationFactory = extFactory;
  }

  @Override
  protected void copyTransients(CRepository source, CRepository destination) {
    ((CRepository) destination).setExternalConfiguration(((CRepository) source).getExternalConfiguration());

    ((CRepository) destination).externalConfigurationImple = ((CRepository) source).externalConfigurationImple;

    ((CRepository) destination).defaultLocalStorageUrl = ((CRepository) source).defaultLocalStorageUrl;

    // trick with RemoteStorage, which is an object, and XStream will not "overlap" it properly (ie. destionation !=
    // null but source == null)
    if (((CRepository) source).getRemoteStorage() == null) {
      ((CRepository) destination).setRemoteStorage(null);
    }
  }

  public ExternalConfiguration<?> getExternalConfiguration() {
    if (externalConfiguration == null) {
      externalConfiguration = prepareExternalConfiguration(getOriginalConfiguration());
    }
    return externalConfiguration;
  }

  protected ExternalConfiguration<?> prepareExternalConfiguration(CRepository configuration) {
    if (externalConfigurationFactory == null) {
      return null;
    }

    // prepare the Xpp3Dom root node
    if (repositoryModel.getExternalConfiguration() == null) {
      // just put an elephant in South Africa to find it for sure ;)
      repositoryModel.setExternalConfiguration(new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME));
    }

    // set the holder
    if (repositoryModel.externalConfigurationImple == null) {
      // in 1st round, i intentionally choosed to make our lives bitter, and handle config manually
      // later we will see about it
      repositoryModel.externalConfigurationImple = externalConfigurationFactory
          .createExternalConfigurationHolder(repositoryModel);
    }

    return new DefaultExternalConfiguration<AbstractXpp3DomExternalConfigurationHolder>(getApplicationConfiguration(),
        this, (AbstractXpp3DomExternalConfigurationHolder) repositoryModel.externalConfigurationImple);
  }

  @Override
  protected CRepository extractConfiguration(Configuration configuration) {
    // this is an exceptional situation, the "normal" way will not work, look at the constructor
    return null;
  }

  @Override
  public boolean isDirty() {
    return isThisDirty() || (getExternalConfiguration() != null && getExternalConfiguration().isDirty());
  }

  @Override
  public void validateChanges() throws ConfigurationException {
    super.validateChanges();
    if (getExternalConfiguration() != null) {
      getExternalConfiguration().validateChanges();
    }
  }

  @Override
  public void commitChanges() throws ConfigurationException {
    super.commitChanges();
    if (getExternalConfiguration() != null) {
      getExternalConfiguration().commitChanges();
    }
  }

  @Override
  public void rollbackChanges() {
    super.rollbackChanges();
    if (getExternalConfiguration() != null) {
      getExternalConfiguration().rollbackChanges();
    }
  }

  // ==

  @Override
  public ValidationResponse doValidateChanges(CRepository changedConfiguration) {
    CRepository cfg = (CRepository) changedConfiguration;

    ValidationResponse response = new ApplicationValidationResponse();

    // ID
    if (StringUtils.isBlank(cfg.getId())) {
      response.addValidationError(new ValidationMessage("id", "Repository ID must not be blank!"));
    }
    else if (!cfg.getId().matches(REPOSITORY_ID_PATTERN)) {
      response.addValidationError(new ValidationMessage("id",
          "Only letters, digits, underscores, hyphens, and dots are allowed in Repository ID"));
    }

    // ID not 'all'
    if ("all".equals(cfg.getId())) {
      response.addValidationError(new ValidationMessage("id", "Repository ID can't be 'all', reserved word"));
    }

    // Name
    if (StringUtils.isBlank(cfg.getName())) {
      response.addValidationWarning(new ValidationMessage("id", "Repository with ID='" + cfg.getId()
          + "' has no name, defaulted it's name to it's ID."));

      cfg.setName(cfg.getId());

      response.setModified(true);
    }

    // LocalStatus
    try {
      LocalStatus.valueOf(cfg.getLocalStatus());
    }
    catch (Exception e) {
      response.addValidationError(new ValidationMessage("localStatus", "LocalStatus of repository with ID=\""
          + cfg.getId() + "\" has unacceptable value \"" + cfg.getLocalStatus() + "\"! (Allowed values are: \""
          + LocalStatus.IN_SERVICE + "\" and \"" + LocalStatus.OUT_OF_SERVICE + "\")", e));
    }

    // indexable
    if (cfg.isIndexable() && (!"maven2".equals(cfg.getProviderHint()))) {
      response.addValidationWarning(new ValidationMessage("indexable", "Indexing isn't supported for \""
          + cfg.getProviderHint() + "\" repositories, only Maven2 repositories are indexable!"));

      cfg.setIndexable(false);

      response.setModified(true);
    }

    // proxy repo URL (if set) -- it must end with a slash (true for Maven1/2 reposes!)
    // TODO: This is temporary solution until we cleanup config framework.
    // This check below should happen in _maven specific_ configuration validation, not here in core
    // This breaks other plugins as OBR and any future one. So, as a fix, we "limit" this
    // to "maven2"/"maven1" providers only for now, to keep OBR plugin unaffected.
    // TODO: THIS CHECK SHOULD BE INJECTED BY PROVIDER WHO PROVIDES MAVEN2 Repositories!
    if ("maven2".equals(cfg.getProviderHint()) || "maven1".equals(cfg.getProviderHint())) {
      if (cfg.getRemoteStorage() != null && cfg.getRemoteStorage().getUrl() != null
          && !cfg.getRemoteStorage().getUrl().endsWith(RepositoryItemUid.PATH_SEPARATOR)) {
        cfg.getRemoteStorage().setUrl(cfg.getRemoteStorage().getUrl() + RepositoryItemUid.PATH_SEPARATOR);
      }
    }

    return response;
  }
}
