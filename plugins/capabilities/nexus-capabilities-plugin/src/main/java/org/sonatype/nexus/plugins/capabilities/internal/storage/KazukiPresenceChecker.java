package org.sonatype.nexus.plugins.capabilities.internal.storage;

/**
 * Helper class that determines is optional Kazuki plugin present or not.
 */
public class KazukiPresenceChecker
{
  public static final boolean PRESENT;

  static {
    boolean present = false;
    try {
      Class.forName("io.kazuki.v0.store.Key", false, KazukiPresenceChecker.class.getClassLoader());
      present = true;
    } catch (ClassNotFoundException e) {
      // ignore
    }
    PRESENT = present;
  }
}
