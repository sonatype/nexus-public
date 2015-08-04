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

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.uid.Attribute;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;

/**
 * Boolean Attribute that returns true if UID represents a path in Maven repository, and path obeys Maven repository
 * layout and is a path pointing to Maven signature file.
 *
 * @author cstamas
 */
public class IsMavenArtifactSignatureAttribute
    implements Attribute<Boolean>
{
  @Override
  public Boolean getValueFor(RepositoryItemUid subject) {
    return subject.getRepository().getRepositoryKind().isFacetAvailable(MavenRepository.class)
        && pathIsValidGav(subject.getRepository().adaptToFacet(MavenRepository.class), subject.getPath());
  }

  protected boolean pathIsValidGav(MavenRepository repository, String path) {
    final Gav gav = repository.getGavCalculator().pathToGav(path);

    return gav != null && !gav.isHash() && gav.isSignature();
  }
}
