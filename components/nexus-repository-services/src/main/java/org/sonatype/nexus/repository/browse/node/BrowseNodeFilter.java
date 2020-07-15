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
package org.sonatype.nexus.repository.browse.node;

import java.util.function.BiPredicate;

/**
 * This interface is used to filter the list of browse nodes that are returned at a given level of the browse tree
 * and we need to be very careful to ensure this condition is not too expensive. It is also worth noting that this has
 * the ability to hide non-leaf nodes which if implemented incorrectly could hide entire branches.
 *
 * @since 3.11
 */
public interface BrowseNodeFilter
    extends BiPredicate<BrowseNode, Boolean>
{
}
