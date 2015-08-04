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
package org.sonatype.nexus.plugins.capabilities.test.helper;

import java.util.Set;

import javax.inject.Named;

import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.plugins.capabilities.Capability;
import org.sonatype.nexus.plugins.capabilities.Tag;
import org.sonatype.nexus.plugins.capabilities.Taggable;

import static org.sonatype.nexus.plugins.capabilities.Tag.repositoryTag;
import static org.sonatype.nexus.plugins.capabilities.Tag.tags;

/**
 * A test/demo capability for using {@link RepositoryCombobox}.
 *
 * @since 2.7
 */
@Named(RepositoryComboCapabilityDescriptor.TYPE_ID)
public class RepositoryComboCapability
    extends TestCapability
    implements Capability, Taggable
{

  @Override
  public String status() {
    return null;
  }

  @Override
  public Set<Tag> getTags() {
    return tags(repositoryTag(context().properties().get("all")));
  }

}
