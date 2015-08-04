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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/**
 * The Tag class represents a minimal XML tree. It consist of a named element
 * with a hashtable of named attributes. Methods are provided to walk the tree
 * and get its constituents. The content of a Tag is a list that contains String
 * objects or other Tag objects.
 */
public class Tag
{
  Tag parent;

  String name;

  Map attributes = new TreeMap();

  Vector content = new Vector();

  static SimpleDateFormat format = new SimpleDateFormat(
      "yyyyMMddHHmmss.SSS");

  /**
   * Construct a new Tag with a name.
   */
  public Tag(String name) {
    this.name = name;
  }

  /**
   * Construct a new Tag with a name.
   */
  public Tag(String name, Map attributes) {
    this.name = name;
    this.attributes = attributes;
  }

  /**
   * Construct a new Tag with a name and a set of attributes. The attributes
   * are given as ( name, value ) ...
   */
  public Tag(String name, String[] attributes) {
    this.name = name;
    for (int i = 0; i < attributes.length; i += 2) {
      addAttribute(attributes[i], attributes[i + 1]);
    }
  }

  /**
   * Construct a new Tag with a single string as content.
   */
  public Tag(String name, String content) {
    this.name = name;
    addContent(content);
  }

  /**
   * Add a new attribute.
   */
  public void addAttribute(String key, String value) {
    attributes.put(key, value);
  }

  /**
   * Add a new attribute.
   */
  public void addAttribute(String key, Object value) {
    if (value == null) {
      return;
    }
    attributes.put(key, value.toString());
  }

  /**
   * Add a new attribute.
   */
  public void addAttribute(String key, int value) {
    attributes.put(key, Integer.toString(value));
  }

  /**
   * Add a new date attribute. The date is formatted as the SimpleDateFormat
   * describes at the top of this class.
   */
  public void addAttribute(String key, Date value) {
    attributes.put(key, format.format(value));
  }

  /**
   * Add a new content string.
   */
  public void addContent(String string) {
    content.addElement(string);
  }

  /**
   * Add a new content tag.
   */
  public void addContent(Tag tag) {
    content.addElement(tag);
    tag.parent = this;
  }

  /**
   * Return the name of the tag.
   */
  public String getName() {
    return name;
  }

  /**
   * Return the attribute value.
   */
  public String getAttribute(String key) {
    return (String) attributes.get(key);
  }

  /**
   * Return the attribute value or a default if not defined.
   */
  public String getAttribute(String key, String deflt) {
    String answer = getAttribute(key);
    return answer == null ? deflt : answer;
  }

  /**
   * Answer the attributes as a Dictionary object.
   */
  public Map getAttributes() {
    return attributes;
  }

  /**
   * Return the contents.
   */
  public Vector getContents() {
    return content;
  }

  /**
   * Return a string representation of this Tag and all its children
   * recursively.
   */
  public String toString() {
    StringWriter sw = new StringWriter();
    print(0, new PrintWriter(sw));
    return sw.toString();
  }

  /**
   * Return only the tags of the first level of descendants that match the
   * name.
   */
  public Vector getContents(String tag) {
    Vector out = new Vector();
    for (Enumeration e = content.elements(); e.hasMoreElements(); ) {
      Object o = e.nextElement();
      if (o instanceof Tag && ((Tag) o).getName().equals(tag)) {
        out.addElement(o);
      }
    }
    return out;
  }

  /**
   * Return the whole contents as a String (no tag info and attributes).
   */
  public String getContentsAsString() {
    StringBuffer sb = new StringBuffer();
    getContentsAsString(sb);
    return sb.toString();
  }

  /**
   * convenient method to get the contents in a StringBuffer.
   */
  public void getContentsAsString(StringBuffer sb) {
    for (Enumeration e = content.elements(); e.hasMoreElements(); ) {
      Object o = e.nextElement();
      if (o instanceof Tag) {
        ((Tag) o).getContentsAsString(sb);
      }
      else {
        sb.append(o.toString());
      }
    }
  }

  /**
   * Print the tag formatted to a PrintWriter.
   */
  public void print(int indent, PrintWriter pw) {
    pw.print("\n");
    spaces(pw, indent);
    pw.print('<');
    pw.print(name);

    for (Iterator e = attributes.keySet().iterator(); e.hasNext(); ) {
      String key = (String) e.next();
      String value = escape((String) attributes.get(key));
      pw.print(' ');
      pw.print(key);
      pw.print("=");
      String quote = "'";
      if (value.indexOf(quote) >= 0) {
        quote = "\"";
      }
      pw.print(quote);
      pw.print(value);
      pw.print(quote);
    }

    if (content.size() == 0) {
      pw.print('/');
    }
    else {
      pw.print('>');
      for (Enumeration e = content.elements(); e.hasMoreElements(); ) {
        Object content = e.nextElement();
        if (content instanceof String) {
          pw.print("\n");
          spaces(pw, indent + 2);
          pw.print(escape((String) content));
/*[mcculls] line-wrapping causes round-trip problems
                                        formatted(pw, indent + 2, 60, escape((String) content));
*/
        }
        else if (content instanceof Tag) {
          Tag tag = (Tag) content;
          tag.print(indent + 2, pw);
        }
      }
      pw.print("\n");
      spaces(pw, indent);
      pw.print("</");
      pw.print(name);
    }
    pw.print('>');
  }

  /**
   * Convenience method to print a string nicely and does character conversion
   * to entities.
   */
/*[mcculls] line-wrapping causes round-trip problems
        void formatted(PrintWriter pw, int left, int width, String s) {
		int pos = width + 1;
		s = s.trim();

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i == 0 || (Character.isWhitespace(c) && pos > width - 3)) {
				pw.print("\n");
				spaces(pw, left);
				pos = 0;
			}
			switch (c) {
				case '<' :
					pw.print("&lt;");
					pos += 4;
					break;
				case '>' :
					pw.print("&gt;");
					pos += 4;
					break;
				case '&' :
					pw.print("&amp;");
					pos += 5;
					break;
				default :
					pw.print(c);
					pos++;
					break;
			}

		}
	}
*/

  /**
   * Escape a string, do entity conversion.
   */
  String escape(String s) {
    if (s == null) {
      return "?null?";
    }

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '&':
          sb.append("&amp;");
          break;
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }

  /**
   * Make spaces.
   */
  void spaces(PrintWriter pw, int n) {
    while (n-- > 0) {
      pw.print(' ');
    }
  }

  /**
   * root/preferences/native/os
   */
  public Tag[] select(String path) {
    return select(path, (Tag) null);
  }

  public Tag[] select(String path, Tag mapping) {
    Vector v = new Vector();
    select(path, v, mapping);
    Tag[] result = new Tag[v.size()];
    v.copyInto(result);
    return result;
  }

  void select(String path, Vector results, Tag mapping) {
    if (path.startsWith("//")) {
      int i = path.indexOf('/', 2);
      String name = path.substring(2, i < 0 ? path.length() : i);

      for (Enumeration e = content.elements(); e.hasMoreElements(); ) {
        Object o = e.nextElement();
        if (o instanceof Tag) {
          Tag child = (Tag) o;
          if (match(name, child, mapping)) {
            results.add(child);
          }
          child.select(path, results, mapping);
        }

      }
      return;
    }

    if (path.length() == 0) {
      results.addElement(this);
      return;
    }

    int i = path.indexOf("/");
    String elementName = path;
    String remainder = "";
    if (i > 0) {
      elementName = path.substring(0, i);
      remainder = path.substring(i + 1);
    }

    for (Enumeration e = content.elements(); e.hasMoreElements(); ) {
      Object o = e.nextElement();
      if (o instanceof Tag) {
        Tag child = (Tag) o;
        if (child.getName().equals(elementName)
            || elementName.equals("*")) {
          child.select(remainder, results, mapping);
        }
      }
    }
  }

  public boolean match(String search, Tag child, Tag mapping) {
    String target = child.getName();
    String sn = null;
    String tn = null;

    if (search.equals("*")) {
      return true;
    }

    int s = search.indexOf(':');
    if (s > 0) {
      sn = search.substring(0, s);
      search = search.substring(s + 1);
    }
    int t = target.indexOf(':');
    if (t > 0) {
      tn = target.substring(0, t);
      target = target.substring(t + 1);
    }

    if (!search.equals(target)) // different tag names
    {
      return false;
    }

    if (mapping == null) {
      return tn == sn || (sn != null && sn.equals(tn));
    }
    else {
      String suri = sn == null ? mapping.getAttribute("xmlns") : mapping
          .getAttribute("xmlns:" + sn);
      String turi = tn == null ? child.findRecursiveAttribute("xmlns")
          : child.findRecursiveAttribute("xmlns:" + tn);
      return turi == suri
          || (turi != null && suri != null && turi.equals(suri));
    }
  }

  public String getString(String path) {
    String attribute = null;
    int index = path.indexOf("@");
    if (index >= 0) {
      // attribute
      attribute = path.substring(index + 1);

      if (index > 0) {
        // prefix path
        path = path.substring(index - 1); // skip -1
      }
      else {
        path = "";
      }
    }
    Tag tags[] = select(path);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tags.length; i++) {
      if (attribute == null) {
        tags[i].getContentsAsString(sb);
      }
      else {
        sb.append(tags[i].getAttribute(attribute));
      }
    }
    return sb.toString();
  }

  public String getStringContent() {
    StringBuffer sb = new StringBuffer();
    for (Enumeration e = content.elements(); e.hasMoreElements(); ) {
      Object c = e.nextElement();
      if (!(c instanceof Tag)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public String getNameSpace() {
    return getNameSpace(name);
  }

  public String getNameSpace(String name) {
    int index = name.indexOf(':');
    if (index > 0) {
      String ns = name.substring(0, index);
      return findRecursiveAttribute("xmlns:" + ns);
    }
    else {
      return findRecursiveAttribute("xmlns");
    }
  }

  public String findRecursiveAttribute(String name) {
    String value = getAttribute(name);
    if (value != null) {
      return value;
    }
    if (parent != null) {
      return parent.findRecursiveAttribute(name);
    }
    return null;
  }

  public String getLocalName() {
    int index = name.indexOf(':');
    if (index <= 0) {
      return name;
    }

    return name.substring(index + 1);
  }

  public void rename(String string) {
    name = string;
  }


  public static void convert(Collection c, String type, Tag parent) {
    for (Iterator i = c.iterator(); i.hasNext(); ) {
      Map map = (Map) i.next();
      parent.addContent(new Tag(type, map));
    }
  }

}
