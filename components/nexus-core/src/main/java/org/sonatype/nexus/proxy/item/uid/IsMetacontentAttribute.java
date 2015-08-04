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
package org.sonatype.nexus.proxy.item.uid;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;

/**
 * Attribute yielding "false" for real repository content, and "true" for all the "meta content", that is actually not
 * holding data serving the basic purpose of this given repository.
 *
 * @author cstamas
 */
public class IsMetacontentAttribute
    implements Attribute<Boolean>
{
  public Boolean getValueFor(RepositoryItemUid subject) {
    // stuff not having metadata:
    // /.nexus/attributes
    // /.nexus/trash
    // we are specific about these for a good reason (see future)
    return subject.getBooleanAttributeValue(IsItemAttributeMetacontentAttribute.class)
        || subject.getBooleanAttributeValue(IsTrashMetacontentAttribute.class);
  }
}
