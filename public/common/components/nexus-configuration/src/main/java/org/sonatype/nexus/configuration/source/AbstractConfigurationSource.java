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
package org.sonatype.nexus.configuration.source;

import java.util.List;

import org.sonatype.nexus.configuration.model.ApplyStatusXO;
import org.sonatype.nexus.configuration.model.InstanceConfigurationXO;
import org.sonatype.nexus.configuration.validation.MissingBlobStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.configuration.ApplyStatus.APPLIED;
import static org.sonatype.nexus.configuration.ApplyStatus.FAILED;
import static org.sonatype.nexus.configuration.ApplyStatus.NOT_APPLIED;

public abstract class AbstractConfigurationSource
    implements ConfigurationSource
{
  private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigurationSource.class);

  @Override
  public final List<ApplyStatusXO> applyToInstance(final InstanceConfigurationXO instanceConfigurationXO) {
    try {
      return doApplyToInstance(instanceConfigurationXO);
    }
    catch (MissingBlobStoreException e) {
      throw e;
    }
    catch (RuntimeException e) {
      LOG.error("Failed to apply configuration for type: {}", getConfigurationTypeId(), e);
      return List.of(getFailedStatus(" " + e.getMessage()));
    }
    catch (Exception e) {
      LOG.error("Failed to apply configuration for type: {}", getConfigurationTypeId(), e);
      return List.of(getFailedStatus(" " + e.getMessage()));
    }
  }

  protected abstract List<ApplyStatusXO> doApplyToInstance(final InstanceConfigurationXO instanceConfigurationXO);

  protected ApplyStatusXO getAppliedStatus(final String message) {
    return ApplyStatusXO.from(getConfigurationTypeId(), "Applied" + message, APPLIED);
  }

  protected ApplyStatusXO getNotAppliedStatus(final String message) {
    return ApplyStatusXO.from(getConfigurationTypeId(), "Not applied" + message, NOT_APPLIED);
  }

  protected ApplyStatusXO getFailedStatus(final String message) {
    return ApplyStatusXO.from(getConfigurationTypeId(), "Failed to apply" + message, FAILED);
  }
}
