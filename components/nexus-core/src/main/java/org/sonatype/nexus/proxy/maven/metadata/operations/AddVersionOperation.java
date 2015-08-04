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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

/**
 * adds new version to metadata
 *
 * @author Oleg Gusakov
 * @version $Id: AddVersionOperation.java 743040 2009-02-10 18:20:26Z ogusakov $
 */
public class AddVersionOperation
    implements MetadataOperation
{

  private String version;

  /**
   * @throws MetadataException
   */
  public AddVersionOperation(StringOperand data)
      throws MetadataException
  {
    setOperand(data);
  }

  public void setOperand(AbstractOperand data)
      throws MetadataException
  {
    if (data == null || !(data instanceof StringOperand)) {
      throw new MetadataException("Operand is not correct: expected StringOperand, but got "
          + (data == null ? "null" : data.getClass().getName()));
    }

    version = ((StringOperand) data).getOperand();
  }

  /**
   * add version to the in-memory metadata instance
   */
  public boolean perform(Metadata metadata)
      throws MetadataException
  {
    if (metadata == null) {
      return false;
    }

    Versioning vs = metadata.getVersioning();

    if (vs == null) {
      vs = new Versioning();
      metadata.setVersioning(vs);
    }

    if (vs.getVersions() != null && vs.getVersions().size() > 0) {
      List<String> vl = vs.getVersions();

      if (vl.contains(version)) {
        return false;
      }
    }

    // We have a large hack happening here
    // Originally, the code was:

    // vs.addVersion( version );
    // List<String> versions = vs.getVersions();
    // Collections.sort( versions, new VersionComparator() );
    // vs.setLatest( getLatestVersion( versions ) );
    // vs.setRelease( getReleaseVersion( versions ) );
    // vs.setLastUpdated( TimeUtil.getUTCTimestamp() );
    // return true;

    // This above resulted with "batch" operations (like Nexus metadata merge when group serves up Maven metadata)
    // high CPU usage and high response times. What was happening that Collections.sort() was invoked over and over
    // for every insertion of one new version.

    // Solution: we sort the list once, probably 1st time we got here -- and we "mark" that fact by using a
    // ArrayList2 class. Then, we _keep_ that sorted list _stable_, by using binarySearch on it for insertions.
    // Thus, we kept the semantics of previous solution but at
    // much less CPU and computational expense.

    List<String> versions = vs.getVersions();

    if (!(versions instanceof ArrayList2)) {
      Collections.sort(versions, new VersionComparator());

      vs.setVersions(new ArrayList2(versions));

      versions = vs.getVersions();
    }

    final int index = Collections.binarySearch(versions, version, new VersionComparator());

    // um, this checks seems unnecessary, since we already checked for contains() above,
    // so if we are here, we _know_ the version to be added is NOT in the list
    if (index < 0) {
      // vs.addVersion( version );
      versions.add(-index - 1, version);

      vs.setLatest(getLatestVersion(versions));

      vs.setRelease(getReleaseVersion(versions));

      vs.setLastUpdated(TimeUtil.getUTCTimestamp());

      return true;
    }
    else {
      // we should never arrive here, se above if()
      return false;
    }
  }

  private String getLatestVersion(List<String> orderedVersions) {
    return orderedVersions.get(orderedVersions.size() - 1);
  }

  private String getReleaseVersion(List<String> orderedVersions) {
    for (int i = orderedVersions.size() - 1; i >= 0; i--) {
      if (!orderedVersions.get(i).endsWith("SNAPSHOT")) {
        return orderedVersions.get(i);
      }
    }

    return "";
  }

  // == A HACK

  public static class ArrayList2
      extends ArrayList<String>
  {
    public ArrayList2(Collection<? extends String> c) {
      super(c);
    }
  }
}
