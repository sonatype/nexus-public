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
package org.sonatype.nexus.repository.content.browse.store;

import org.sonatype.nexus.repository.content.store.ContentStoreModule;

/**
 * Extend this module to add the necessary bindings to support browsing.
 * Declare your DAO and annotate the module with the name of your format:
 *
 * <code><pre>
 * &#64;Named("example")
 * public class ExampleBrowseModule
 *     extends FormatBrowseModule&lt;ExampleBrowseNodeDAO&gt;
 * {
 *   // nothing to add...
 * }
 * </pre></code>
 *
 * @since 3.26
 */
public abstract class FormatBrowseModule<DAO extends BrowseNodeDAO>
    extends ContentStoreModule<BrowseNodeStore<DAO>>
{
  // nothing to add...
}
