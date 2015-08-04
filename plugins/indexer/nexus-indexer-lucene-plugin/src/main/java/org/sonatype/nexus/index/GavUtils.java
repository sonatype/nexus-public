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
package org.sonatype.nexus.index;

import org.sonatype.nexus.proxy.maven.gav.Gav;

/**
 * Simple "bridge" utility class that converts Nexus Gav classes into Maven Indexer Gav classes.
 *
 * @author cstamas
 */
public class GavUtils
{
  public static org.apache.maven.index.artifact.Gav convert(final Gav gav) {
    final org.apache.maven.index.artifact.Gav.HashType ht =
        gav.getHashType() != null ? org.apache.maven.index.artifact.Gav.HashType.valueOf(gav.getHashType().name())
            : null;
    final org.apache.maven.index.artifact.Gav.SignatureType st =
        gav.getSignatureType() != null ? org.apache.maven.index.artifact.Gav.SignatureType
            .valueOf(gav.getSignatureType().name())
            : null;

    return new org.apache.maven.index.artifact.Gav(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(),
        gav.getClassifier(), gav.getExtension(), gav.getSnapshotBuildNumber(), gav.getSnapshotTimeStamp(),
        gav.getName(), gav.isHash(), ht, gav.isSignature(), st);
  }

}
