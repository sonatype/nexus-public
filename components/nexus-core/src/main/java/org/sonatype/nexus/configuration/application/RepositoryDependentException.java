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
package org.sonatype.nexus.configuration.application;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.repository.Repository;

import static java.lang.String.format;
import static org.sonatype.nexus.proxy.utils.RepositoryStringUtils.getHumanizedNameString;

public class RepositoryDependentException
    extends ConfigurationException
{

  private static final long serialVersionUID = -2037859093869479166L;

  private final Repository dependant;

  private final Repository repository;

  public RepositoryDependentException(Repository repository, Repository dependant) {
    super(format("Repository %s cannot be deleted due to dependant repository %s.",
        getHumanizedNameString(repository), getHumanizedNameString(dependant)));
    this.repository = repository;
    this.dependant = dependant;
  }

  public Repository getDependant() {
    return dependant;
  }

  public Repository getRepository() {
    return repository;
  }

  public String getUIMessage() {
    return format(
        "Repository '%s' cannot be deleted due to dependant repository '%s'.\nDependencies must be removed in order to complete this operation.",
        getHumanizedNameString(repository), getHumanizedNameString(dependant));
  }

}
