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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.WritablePrefixSource;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class WritablePrefixSourceModifierTest
{
  protected final String[] entries1 = new String[]{"/org/sonatype", "/org/apache"};

  protected final String[] entries2 = new String[]{"/org/sonatype/nexus", "/org/apache/maven"};

  private WritablePrefixSource writableEntrySource;

  private WritablePrefixSourceModifier wesm;

  @Before
  public void prepare()
      throws IOException
  {
    writableEntrySource = new WritableArrayListPrefixSource(Arrays.asList(entries1));
    wesm = new WritablePrefixSourceModifier(writableEntrySource, 2);
  }

  @Test
  public void smoke()
      throws IOException
  {
    assertThat("No changes added yet", !wesm.hasChanges());
    assertThat("No changes added yet", !wesm.apply());
    assertThat("No changes added yet", !wesm.reset());

  }

  @Test
  public void offeringPathsNotModifyingWL() {
    // offering paths that would not modify WL
    assertThat("WL would not be changed", !wesm.offerEntry("/org/sonatype/nexus"));
    assertThat("No changes added yet", !wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.offerEntry("/org/apache/maven"));
    assertThat("No changes added yet", !wesm.hasChanges());
  }

  @Test
  public void offeringPathsModifyingWLAndReset()
      throws IOException
  {
    // offering paths that modify WL and reset
    assertThat("WL is changed", wesm.offerEntry("/com/sonatype/nexus"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.offerEntry("/com/mycorp"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("Changes were added", wesm.reset());
    assertThat("No changes added yet, wesm was reset", !wesm.hasChanges());
    assertThat("Entry unchanged", writableEntrySource.readEntries(), hasItems(entries1));
  }

  @Test
  public void offeringPathsModifyingWLAndApply()
      throws IOException
  {
    // offering paths that modify WL and apply
    assertThat("WL is changed", wesm.offerEntry("/com/sonatype/nexus"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.offerEntry("/com/mycorp"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("Changes were added", wesm.apply());
    assertThat("No changes added yet, wesm was applied", !wesm.hasChanges());
    assertThat("Entry unchanged", writableEntrySource.readEntries(), hasItems(entries1));
    assertThat("Entry unchanged", writableEntrySource.readEntries(), hasItems(new String[]{"/com/mycorp"}));
  }

  @Test
  public void revokingPathsNotModifyingWL() {
    // revoking paths that would not modify WL
    assertThat("WL would not be changed", !wesm.revokeEntry("/com")); // not in WL
    assertThat("No changes added yet", !wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/com/sonatype")); // not in WL
    assertThat("No changes added yet", !wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/org/sonatype/nexus")); // parent is in list
    assertThat("No changes added yet", !wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/org/apache/maven")); // parent is in list
    assertThat("No changes added yet", !wesm.hasChanges());
  }

  @Test
  public void revokingPathsModifyingWLAndReset()
      throws IOException
  {
    // revoking paths that modify WL and reset
    assertThat("WL is changed", wesm.revokeEntry("/org/sonatype"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.revokeEntry("/org/apache"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/com/sonatype")); // not in WL
    assertThat("No changes added yet", wesm.hasChanges());
    assertThat("Changes were added", wesm.reset());
    assertThat("No changes added yet, wesm was reset", !wesm.hasChanges());
    assertThat("Entry unchanged", writableEntrySource.readEntries(), hasItems(entries1));
  }

  @Test
  public void revokingPathsModifyingWLAndApply()
      throws IOException
  {
    // revoking paths that modify WL and apply
    assertThat("WL is changed", wesm.revokeEntry("/org/sonatype"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.revokeEntry("/org/apache"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/com/sonatype")); // not in WL
    assertThat("No changes added yet", wesm.hasChanges());
    assertThat("Changes were added", wesm.apply());
    assertThat("No changes added yet, wesm was applied", !wesm.hasChanges());
    assertThat("Entry removed", writableEntrySource.readEntries(), not(hasItems(entries1)));
    assertThat("Entry removed", writableEntrySource.readEntries().size(), is(0));
  }

  @Test
  public void modifyingFreely()
      throws IOException
  {
    // Note: using entries2 that has 3 depth entries
    writableEntrySource = new WritableArrayListPrefixSource(Arrays.asList(entries2));
    wesm = new WritablePrefixSourceModifier(writableEntrySource, 3);

    assertThat("WL would be changed", wesm.revokeEntry("/org/sonatype")); // child is in list
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL would be changed", wesm.revokeEntry("/org/apache")); // child is in list
    assertThat("Changes were adde", wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/org/sonatype/nexus")); // already removed by parent above
    assertThat("No changes added yet", wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/org/apache/maven")); // already removed by parent above
    assertThat("No changes added yet", wesm.hasChanges());

    // adding some
    assertThat("WL is changed", wesm.offerEntry("/com/sonatype/nexus"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.offerEntry("/com/mycorp"));
    assertThat("Changes were added", wesm.hasChanges());

    assertThat("Changes were added", wesm.apply());

    assertThat(writableEntrySource.readEntries(), contains("/com/sonatype/nexus", "/com/mycorp"));
  }

  // ==

  @Test
  public void edgeCase1()
      throws IOException
  {
    writableEntrySource = new WritableArrayListPrefixSource(Arrays.asList(entries1));
    wesm = new WritablePrefixSourceModifier(writableEntrySource, 2);

    assertThat("WL would not be changed", !wesm.revokeEntry("/org/sonatype/nexus")); // parent is in list
    assertThat("No changes added yet", !wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/org/apache/maven")); // parent is in list
    assertThat("No changes added yet", !wesm.hasChanges());
    assertThat("WL would not be changed", wesm.revokeEntry("/org/sonatype"));
    assertThat("No changes added yet", wesm.hasChanges());
    assertThat("WL would not be changed", wesm.revokeEntry("/org/apache"));
    assertThat("No changes added yet", wesm.hasChanges());

    // adding some
    assertThat("WL is changed", wesm.offerEntry("/com/sonatype/nexus"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.offerEntry("/com/mycorp"));
    assertThat("Changes were added", wesm.hasChanges());

    assertThat("Changes were added", wesm.apply());

    assertThat(writableEntrySource.readEntries(), contains("/com/sonatype", "/com/mycorp"));
  }

  @Test
  public void edgeCase2()
      throws IOException
  {
    writableEntrySource = new WritableArrayListPrefixSource(Arrays.asList(entries2));
    wesm = new WritablePrefixSourceModifier(writableEntrySource, 2);

    assertThat("WL is changed", wesm.revokeEntry("/org/sonatype")); // child is in list
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.revokeEntry("/org/apache")); // child is in list
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/org/sonatype/nexus")); // already removed by parent above
    assertThat("No changes added yet", wesm.hasChanges());
    assertThat("WL would not be changed", !wesm.revokeEntry("/org/apache/maven")); // already removed by parent above
    assertThat("No changes added yet", wesm.hasChanges());

    // adding some
    assertThat("WL is changed", wesm.offerEntry("/com/sonatype/nexus"));
    assertThat("Changes were added", wesm.hasChanges());
    assertThat("WL is changed", wesm.offerEntry("/com/mycorp"));
    assertThat("Changes were added", wesm.hasChanges());

    assertThat("Changes were added", wesm.apply());

    assertThat(writableEntrySource.readEntries(), contains("/com/sonatype", "/com/mycorp"));
  }

  // ==

  public static class WritableArrayListPrefixSource
      implements WritablePrefixSource
  {
    private List<String> entries;

    private long created;

    /**
     * Constructor with entries. Will have last modified timestamp as "now" (moment of creation).
     *
     * @param entries list of entries, might not be {@code null}
     */
    public WritableArrayListPrefixSource(final List<String> entries) {
      this(entries, System.currentTimeMillis());
    }

    /**
     * Constructor with entries and timestamp.
     *
     * @param entries list of entries, might not be {@code null}.
     * @param created the timestamp this instance should report.
     */
    public WritableArrayListPrefixSource(final List<String> entries, final long created) {
      this.entries = entries;
      this.created = entries != null ? created : -1;
    }

    @Override
    public boolean exists() {
      return entries != null;
    }

    @Override
    public boolean supported() {
      return exists();
    }

    @Override
    public List<String> readEntries()
        throws IOException
    {
      return entries;
    }

    @Override
    public long getLostModifiedTimestamp() {
      return created;
    }

    @Override
    public void writeEntries(PrefixSource entrySource)
        throws IOException
    {
      this.entries = entrySource.readEntries();
      this.created = System.currentTimeMillis();
    }

    @Override
    public void delete()
        throws IOException
    {
      entries = null;
      created = -1;
    }
  }
}
