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
package org.sonatype.nexus.configuration;

import java.util.Collection;
import java.util.Collections;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;

/**
 * An event fired on configuration change (upon succesful save). This event is meant for component outside of
 * "configuration framework", for any other component interested in configuration change (like feed generators, mail
 * senders, etc).
 *
 * @author cstamas
 */
public class ConfigurationChangeEvent
    extends ConfigurationEvent
{
  private final Collection<Configurable> changes;

  private final String userId;

  public ConfigurationChangeEvent(ApplicationConfiguration configuration, Collection<Configurable> changes,
                                  String userId)
  {
    super(configuration);

    if (changes == null) {
      changes = Collections.emptyList();
    }

    this.changes = Collections.unmodifiableCollection(changes);

    this.userId = userId;
  }

  public Collection<Configurable> getChanges() {
    return changes;
  }

  public String getUserId() {
    return userId;
  }
}
