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
package org.osgi.impl.bundle.obr.resource;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class FilterImpl
{
  final char WILDCARD = 65535;

  final int EQ = 0;

  final int LE = 1;

  final int GE = 2;

  final int APPROX = 3;

  final int LESS = 4;

  final int GREATER = 5;

  final int SUBSET = 6;

  final int SUPERSET = 7;

  private String filter;

  abstract class Query
  {
    static final String GARBAGE = "Trailing garbage";

    static final String MALFORMED = "Malformed query";

    static final String EMPTY = "Empty list";

    static final String SUBEXPR = "No subexpression";

    static final String OPERATOR = "Undefined operator";

    static final String TRUNCATED = "Truncated expression";

    static final String EQUALITY = "Only equality supported";

    private String tail;

    boolean match() throws IllegalArgumentException {
      tail = filter;
      boolean val = doQuery();
      if (tail.length() > 0) {
        error(GARBAGE);
      }
      return val;
    }

    private boolean doQuery() throws IllegalArgumentException {
      if (tail.length() < 3 || !prefix("(")) {
        error(MALFORMED);
      }
      boolean val;

      switch (tail.charAt(0)) {
        case '&':
          val = doAnd();
          break;
        case '|':
          val = doOr();
          break;
        case '!':
          val = doNot();
          break;
        default:
          val = doSimple();
          break;
      }

      if (!prefix(")")) {
        error(MALFORMED);
      }
      return val;
    }

    private boolean doAnd() throws IllegalArgumentException {
      tail = tail.substring(1);
      boolean val = true;
      if (!tail.startsWith("(")) {
        error(EMPTY);
      }
      do {
        if (!doQuery()) {
          val = false;
        }
      }
      while (tail.startsWith("("));
      return val;
    }

    private boolean doOr() throws IllegalArgumentException {
      tail = tail.substring(1);
      boolean val = false;
      if (!tail.startsWith("(")) {
        error(EMPTY);
      }
      do {
        if (doQuery()) {
          val = true;
        }
      }
      while (tail.startsWith("("));
      return val;
    }

    private boolean doNot() throws IllegalArgumentException {
      tail = tail.substring(1);
      if (!tail.startsWith("(")) {
        error(SUBEXPR);
      }
      return !doQuery();
    }

    private boolean doSimple() throws IllegalArgumentException {
      int op = 0;
      Object attr = getAttr();

      if (prefix("=")) {
        op = EQ;
      }
      else if (prefix("<=")) {
        op = LE;
      }
      else if (prefix(">=")) {
        op = GE;
      }
      else if (prefix("~=")) {
        op = APPROX;
      }
      else if (prefix("*>")) {
        op = SUPERSET;
      }
      else if (prefix("<*")) {
        op = SUBSET;
      }
      else if (prefix("<")) {
        op = LESS;
      }
      else if (prefix(">")) {
        op = GREATER;
      }
      else {
        error(OPERATOR);
      }

      return compare(attr, op, getValue());
    }

    private boolean prefix(String pre) {
      if (!tail.startsWith(pre)) {
        return false;
      }
      tail = tail.substring(pre.length());
      return true;
    }

    private Object getAttr() {
      int len = tail.length();
      int ix = 0;
      label:
      for (; ix < len; ix++) {
        switch (tail.charAt(ix)) {
          case '(':
          case ')':
          case '<':
          case '>':
          case '=':
          case '~':
          case '*':
          case '}':
          case '{':
          case '\\':
            break label;
        }
      }
      String attr = tail.substring(0, ix).toLowerCase();
      tail = tail.substring(ix);
      return getProp(attr);
    }

    abstract Object getProp(String key);

    private String getValue() {
      StringBuffer sb = new StringBuffer();
      int len = tail.length();
      int ix = 0;
      label:
      for (; ix < len; ix++) {
        char c = tail.charAt(ix);
        switch (c) {
          case '(':
          case ')':
            break label;
          case '*':
            sb.append(WILDCARD);
            break;
          case '\\':
            if (ix == len - 1) {
              break label;
            }
            sb.append(tail.charAt(++ix));
            break;
          default:
            sb.append(c);
            break;
        }
      }
      tail = tail.substring(ix);
      return sb.toString();
    }

    private void error(String m) throws IllegalArgumentException {
      throw new IllegalArgumentException(m + " " + tail);
    }

    private boolean compare(Object obj, int op, String s) {
      if (obj == null) {
        // No value is ok for a subset
        if (op == SUBSET) {
          return true;
        }

        // No value is ok for a superset when the value is
        // empty
        if (op == SUPERSET) {
          return s.trim().length() == 0;
        }

        return false;
      }
      try {
        Class numClass = obj.getClass();
        if (numClass == String.class) {
          return compareString((String) obj, op, s);
        }
        else if (numClass == Character.class) {
          return compareString(obj.toString(), op, s);
        }
        else if (numClass == Long.class) {
          return compareSign(op, Long.valueOf(s)
              .compareTo((Long) obj));
        }
        else if (numClass == Integer.class) {
          return compareSign(op, Integer.valueOf(s).compareTo(
              (Integer) obj));
        }
        else if (numClass == Short.class) {
          return compareSign(op, Short.valueOf(s).compareTo(
              (Short) obj));
        }
        else if (numClass == Byte.class) {
          return compareSign(op, Byte.valueOf(s)
              .compareTo((Byte) obj));
        }
        else if (numClass == Double.class) {
          return compareSign(op, Double.valueOf(s).compareTo(
              (Double) obj));
        }
        else if (numClass == Float.class) {
          return compareSign(op, Float.valueOf(s).compareTo(
              (Float) obj));
        }
        else if (numClass == Boolean.class) {
          if (op != EQ) {
            return false;
          }
          int a = Boolean.valueOf(s).booleanValue() ? 1 : 0;
          int b = ((Boolean) obj).booleanValue() ? 1 : 0;
          return compareSign(op, a - b);
        }
        else if (numClass == BigInteger.class) {
          return compareSign(op, new BigInteger(s)
              .compareTo((BigInteger) obj));
        }
        else if (obj instanceof Collection) {
          if (op == SUBSET || op == SUPERSET) {
            StringSet set = new StringSet(s);
            if (op == SUBSET) {
              return set.containsAll((Collection) obj);
            }
            else {
              return ((Collection) obj).containsAll(set);
            }
          }

          for (Iterator i = ((Collection) obj).iterator(); i
              .hasNext(); ) {
            Object element = i.next();
            if (compare(element, op, s)) {
              return true;
            }
          }
        }
        else if (numClass.isArray()) {
          int len = Array.getLength(obj);
          for (int i = 0; i < len; i++) {
            if (compare(Array.get(obj, i), op, s)) {
              return true;
            }
          }
        }
        else {
          try {
            if (op == SUPERSET || op == SUBSET) {
              StringSet set = new StringSet(s);
              if (op == SUPERSET) {
                return set.contains(obj);
              }
              else {
                return set.size() == 0
                    || (set.size() == 1 && set.iterator()
                    .next().equals(obj));
              }
            }
            else {
              Constructor constructor = numClass
                  .getConstructor(new Class[]{String.class});
              Object instance = constructor
                  .newInstance(new Object[]{s});
              switch (op) {
                case EQ:
                  return obj.equals(instance);
                case LESS:
                  return ((Comparable) obj)
                      .compareTo(instance) < 0;
                case GREATER:
                  return ((Comparable) obj)
                      .compareTo(instance) > 0;
                case LE:
                  return ((Comparable) obj)
                      .compareTo(instance) <= 0;
                case GE:
                  return ((Comparable) obj)
                      .compareTo(instance) >= 0;
              }
            }
          }
          catch (Exception e) {
            e.printStackTrace();
            // Ignore
          }
        }
      }
      catch (Exception e) {
      }
      return false;
    }
  }

  class DictQuery
      extends Query
  {
    private Map dict;

    DictQuery(Map dict) {
      this.dict = dict;
    }

    Object getProp(String key) {
      return dict.get(key);
    }
  }

  public FilterImpl(String filter) throws IllegalArgumentException {
    // NYI: Normalize the filter string?
    this.filter = filter;
    if (filter == null || filter.length() == 0) {
      throw new IllegalArgumentException("Null query");
    }
  }

  public boolean match(Map dict) {
    try {
      return new DictQuery(dict).match();
    }
    catch (IllegalArgumentException e) {
      return false;
    }
  }

  public String toString() {
    return filter;
  }

  public boolean equals(Object obj) {
    return obj != null && obj instanceof FilterImpl
        && filter.equals(((FilterImpl) obj).filter);
  }

  public int hashCode() {
    return filter.hashCode();
  }

  boolean compareString(String s1, int op, String s2) {
    switch (op) {
      case EQ:
        return patSubstr(s1, s2);
      case APPROX:
        return patSubstr(fixupString(s1), fixupString(s2));
      default:
        return compareSign(op, s2.compareTo(s1));
    }
  }

  boolean compareSign(int op, int cmp) {
    switch (op) {
      case LE:
        return cmp >= 0;
      case GE:
        return cmp <= 0;
      case EQ:
        return cmp == 0;
      default: /* APPROX */
        return cmp == 0;
    }
  }

  String fixupString(String s) {
    StringBuffer sb = new StringBuffer();
    int len = s.length();
    boolean isStart = true;
    boolean isWhite = false;
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (Character.isWhitespace(c)) {
        isWhite = true;
      }
      else {
        if (!isStart && isWhite) {
          sb.append(' ');
        }
        if (Character.isUpperCase(c)) {
          c = Character.toLowerCase(c);
        }
        sb.append(c);
        isStart = false;
        isWhite = false;
      }
    }
    return sb.toString();
  }

  boolean patSubstr(String s, String pat) {
    if (s == null) {
      return false;
    }
    if (pat.length() == 0) {
      return s.length() == 0;
    }
    if (pat.charAt(0) == WILDCARD) {
      pat = pat.substring(1);
      for (; ; ) {
        if (patSubstr(s, pat)) {
          return true;
        }
        if (s.length() == 0) {
          return false;
        }
        s = s.substring(1);
      }
    }
    else {
      if (s.length() == 0 || s.charAt(0) != pat.charAt(0)) {
        return false;
      }
      return patSubstr(s.substring(1), pat.substring(1));
    }
  }
}
