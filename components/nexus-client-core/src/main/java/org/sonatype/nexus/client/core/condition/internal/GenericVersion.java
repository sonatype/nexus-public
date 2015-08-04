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
package org.sonatype.nexus.client.core.condition.internal;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/******
 * Copied from org.sonatype.aether:aether-util:1.13.1
 ******/

/**
 * A generic version, that is a version that accepts any input string and tries to apply common sense sorting. See
 * {@link GenericVersionScheme} for details.
 */
final class GenericVersion
    implements Version
{

  private final String version;

  private final Item[] items;

  private final int hash;

  /**
   * Creates a generic version from the specified string.
   *
   * @param version The version string, must not be {@code null}.
   */
  public GenericVersion(String version) {
    this.version = version;
    items = parse(version);
    hash = Arrays.hashCode(items);
  }

  private static Item[] parse(String version) {
    List<Item> items = new ArrayList<Item>();

    for (Tokenizer tokenizer = new Tokenizer(version); tokenizer.next(); ) {
      Item item = new Item(tokenizer);
      items.add(item);
    }

    trimPadding(items);

    return items.toArray(new Item[items.size()]);
  }

  private static void trimPadding(List<Item> items) {
    Boolean number = null;
    int end = items.size() - 1;
    for (int i = end; i > 0; i--) {
      Item item = items.get(i);
      if (!Boolean.valueOf(item.isNumber()).equals(number)) {
        end = i;
        number = Boolean.valueOf(item.isNumber());
      }
      if (end == i && (i == items.size() - 1 || items.get(i - 1).isNumber() == item.isNumber())
          && item.compareTo(null) == 0) {
        items.remove(i);
        end--;
      }
    }
  }

  public int compareTo(Version obj) {
    final Item[] these = items;
    final Item[] those = ((GenericVersion) obj).items;

    boolean number = true;

    for (int index = 0; ; index++) {
      if (index >= these.length && index >= those.length) {
        return 0;
      }
      else if (index >= these.length) {
        return -comparePadding(those, index, null);
      }
      else if (index >= those.length) {
        return comparePadding(these, index, null);
      }

      Item thisItem = these[index];
      Item thatItem = those[index];

      if (thisItem.isNumber() != thatItem.isNumber()) {
        if (number == thisItem.isNumber()) {
          return comparePadding(these, index, Boolean.valueOf(number));
        }
        else {
          return -comparePadding(those, index, Boolean.valueOf(number));
        }
      }
      else {
        int rel = thisItem.compareTo(thatItem);
        if (rel != 0) {
          return rel;
        }
        number = thisItem.isNumber();
      }
    }
  }

  private static int comparePadding(Item[] items, int index, Boolean number) {
    int rel = 0;
    for (int i = index; i < items.length; i++) {
      Item item = items[i];
      if (number != null && number.booleanValue() != item.isNumber()) {
        break;
      }
      rel = item.compareTo(null);
      if (rel != 0) {
        break;
      }
    }
    return rel;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof GenericVersion) && compareTo((GenericVersion) obj) == 0;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return version;
  }

  static final class Tokenizer
  {

    private final String version;

    private int index;

    private String token;

    private boolean number;

    private boolean terminatedByNumber;

    public Tokenizer(String version) {
      this.version = (version.length() > 0) ? version : "0";
    }

    public String getToken() {
      return token;
    }

    public boolean isNumber() {
      return number;
    }

    public boolean isTerminatedByNumber() {
      return terminatedByNumber;
    }

    public boolean next() {
      final int n = version.length();
      if (index >= n) {
        return false;
      }

      int state = -2;

      int start = index;
      int end = n;
      terminatedByNumber = false;

      for (; index < n; index++) {
        char c = version.charAt(index);

        if (c == '.' || c == '-') {
          end = index;
          index++;
          break;
        }
        else {
          int digit = Character.digit(c, 10);
          if (digit >= 0) {
            if (state == -1) {
              end = index;
              terminatedByNumber = true;
              break;
            }
            if (state == 0) {
              // normalize numbers and strip leading zeros (prereq for Integer/BigInteger handling)
              start++;
            }
            state = (state > 0 || digit > 0) ? 1 : 0;
          }
          else {
            if (state >= 0) {
              end = index;
              break;
            }
            state = -1;
          }
        }

      }

      if (end - start > 0) {
        token = version.substring(start, end);
        number = state >= 0;
      }
      else {
        token = "0";
        number = true;
      }

      return true;
    }

    @Override
    public String toString() {
      return String.valueOf(token);
    }

  }

  static final class Item
  {

    private static final int KIND_BIGINT = 3;

    private static final int KIND_INT = 2;

    private static final int KIND_STRING = 1;

    private static final int KIND_QUALIFIER = 0;

    private static final Map<String, Integer> QUALIFIERS;

    static {
      QUALIFIERS = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
      QUALIFIERS.put("alpha", Integer.valueOf(-5));
      QUALIFIERS.put("beta", Integer.valueOf(-4));
      QUALIFIERS.put("milestone", Integer.valueOf(-3));
      QUALIFIERS.put("cr", Integer.valueOf(-2));
      QUALIFIERS.put("rc", Integer.valueOf(-2));
      QUALIFIERS.put("snapshot", Integer.valueOf(-1));
      QUALIFIERS.put("ga", Integer.valueOf(0));
      QUALIFIERS.put("final", Integer.valueOf(0));
      QUALIFIERS.put("", Integer.valueOf(0));
      QUALIFIERS.put("sp", Integer.valueOf(1));
    }

    private final int kind;

    private final Object value;

    public Item(Tokenizer tokenizer) {
      String token = tokenizer.getToken();
      if (tokenizer.isNumber()) {
        try {
          if (token.length() < 10) {
            kind = KIND_INT;
            value = Integer.valueOf(Integer.parseInt(token));
          }
          else {
            kind = KIND_BIGINT;
            value = new BigInteger(token);
          }
        }
        catch (NumberFormatException e) {
          throw new IllegalStateException(e);
        }
      }
      else {
        if (tokenizer.isTerminatedByNumber() && token.length() == 1) {
          switch (token.charAt(0)) {
            case 'a':
            case 'A':
              token = "alpha";
              break;
            case 'b':
            case 'B':
              token = "beta";
              break;
            case 'm':
            case 'M':
              token = "milestone";
              break;
          }
        }
        Integer qualifier = QUALIFIERS.get(token);
        if (qualifier != null) {
          kind = KIND_QUALIFIER;
          value = qualifier;
        }
        else {
          kind = KIND_STRING;
          value = token.toLowerCase(Locale.ENGLISH);
        }
      }
    }

    public boolean isNumber() {
      return kind >= KIND_INT;
    }

    public int compareTo(Item that) {
      int rel;
      if (that == null) {
        // null in this context denotes the pad item (0 or "ga")
        switch (kind) {
          case KIND_BIGINT:
          case KIND_STRING:
            rel = 1;
            break;
          case KIND_INT:
          case KIND_QUALIFIER:
            rel = ((Integer) value).intValue();
            break;
          default:
            throw new IllegalStateException("unknown version item kind " + kind);
        }
      }
      else {
        rel = kind - that.kind;
        if (rel == 0) {
          switch (kind) {
            case KIND_BIGINT:
              rel = ((BigInteger) value).compareTo((BigInteger) that.value);
              break;
            case KIND_INT:
            case KIND_QUALIFIER:
              rel = ((Integer) value).compareTo((Integer) that.value);
              break;
            case KIND_STRING:
              rel = ((String) value).compareToIgnoreCase((String) that.value);
              break;
            default:
              throw new IllegalStateException("unknown version item kind " + kind);
          }
        }
      }
      return rel;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Item) && compareTo((Item) obj) == 0;
    }

    @Override
    public int hashCode() {
      return value.hashCode() + kind * 31;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

  }

}
