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
package org.sonatype.nexus.repository.site.plugin;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.registry.AbstractIdContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;

/**
 * The Site content class. It is not compatible with anything, hence it is not groupable.
 *
 * @author cstamas
 */
@Named(SiteRepository.ID)
@Singleton
public class SiteContentClass
    extends AbstractIdContentClass
{

  public static final String NAME = "Site";

  public String getId() {
    return SiteRepository.ID;
  }

  @Override
  public String getName() {
    return NAME;
  }

  public boolean isCompatible(ContentClass contentClass) {
    return false;
  }

  public boolean isGroupable() {
    return false;
  }
}
