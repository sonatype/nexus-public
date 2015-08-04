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
package org.sonatype.nexus.proxy.maven.uid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.item.uid.Attribute;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeSource;

/**
 * The attributes implemented in Nexus Maven plugin contributing Maven specific UID attributes.
 *
 * @author cstamas
 */
@Named("maven")
@Singleton
public class MavenRepositoryItemUidAttributeSource
    implements RepositoryItemUidAttributeSource
{
  private final Map<Class<?>, Attribute<?>> attributes;

  public MavenRepositoryItemUidAttributeSource() {
    Map<Class<?>, Attribute<?>> attrs = new HashMap<Class<?>, Attribute<?>>(6);

    attrs.put(IsMavenArtifactAttribute.class, new IsMavenArtifactAttribute());
    attrs.put(IsMavenSnapshotArtifactAttribute.class, new IsMavenSnapshotArtifactAttribute());
    attrs.put(IsMavenChecksumAttribute.class, new IsMavenChecksumAttribute());
    attrs.put(IsMavenPomAttribute.class, new IsMavenPomAttribute());
    attrs.put(IsMavenRepositoryMetadataAttribute.class, new IsMavenRepositoryMetadataAttribute());
    attrs.put(IsMavenArtifactSignatureAttribute.class, new IsMavenArtifactSignatureAttribute());

    this.attributes = Collections.unmodifiableMap(attrs);
  }

  @Override
  public Map<Class<?>, Attribute<?>> getAttributes() {
    return attributes;
  }
}
