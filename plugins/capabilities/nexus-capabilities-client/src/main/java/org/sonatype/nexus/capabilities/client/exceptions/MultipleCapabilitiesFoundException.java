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
package org.sonatype.nexus.capabilities.client.exceptions;

import java.util.Collection;

import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.capabilities.client.Filter;
import org.sonatype.nexus.client.core.exception.NexusClientException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exception thrown when looking for a unique capability based on a filter and the filter matches more then one.
 *
 * @since capabilities 2.2
 */
@SuppressWarnings("serial")
public class MultipleCapabilitiesFoundException
    extends NexusClientException
{

  private final Filter filter;

  private final Collection<Capability> capabilities;

  public MultipleCapabilitiesFoundException(final Filter filter,
                                            final Collection<Capability> capabilities)
  {
    super(String.format(
        "Found multiple capabilities matching filter '%s': %s",
        checkNotNull(filter), checkNotNull(capabilities)
    ));
    this.filter = filter;
    this.capabilities = capabilities;
  }

  public Filter getFilter() {
    return filter;
  }

  public Collection<Capability> getCapabilities() {
    return capabilities;
  }

}
