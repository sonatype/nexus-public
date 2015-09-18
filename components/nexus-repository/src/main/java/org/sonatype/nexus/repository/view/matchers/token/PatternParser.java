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
package org.sonatype.nexus.repository.view.matchers.token;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Parses a template pattern (see {@link TokenParser} for syntax) into a series of {@link Token}s.
 *
 * @since 3.0
 */
public class PatternParser
{
  public static final String DEFAULT_VARIABLE_REGEXP = "[^/]+";

  private static final List<Character> ESCAPE_CHARS = asList('\\');

  private static final List<Character> VARNAME_DISALLOWED_CHARS = asList(' ', '}');

  private static final List<Character> VARIABLE_NAME_TERMINATORS = asList(':', '}');

  private static final List<Character> END_OF_LITERAL = asList('{');

  private static final List<Character> END_OF_VARIABLE_DECLARATION = asList('}');

  private List<Token> tokens = new ArrayList<>();

  private static final List<Character> NONE = Collections.<Character>emptyList();

  public PatternParser(final String tokenPattern) {
    checkNotNull(tokenPattern);

    parseTemplate(new StringCharacterIterator(tokenPattern));
  }

  public List<Token> getTokens() {
    return tokens;
  }

  private void parseTemplate(final CharacterIterator iterator) {
    while (true) {
      final char ch = iterator.current();
      switch (ch) {
        case CharacterIterator.DONE:
          return;
        case '{':
          parseVariable(iterator);
          break;
        default:
          parseLiteral(iterator);
      }
    }
  }

  private void parseVariable(final CharacterIterator iterator) {
    // Consume the starting '{' character
    iterator.next();

    final String varName = readFragment(iterator, VARIABLE_NAME_TERMINATORS, ESCAPE_CHARS, VARNAME_DISALLOWED_CHARS);

    // The iterator is currently pointing to the end character.
    if (iterator.current() == ':') {
      // Skip the ':' character
      iterator.next();
      final String regexp = readFragment(iterator, END_OF_VARIABLE_DECLARATION, ESCAPE_CHARS, NONE);
      tokens.add(new VariableToken(varName, regexp));
    }
    else {
      tokens.add(new VariableToken(varName, DEFAULT_VARIABLE_REGEXP));
    }

    // The iterator should now be pointing to the varname end delimiter.
    checkArgument(iterator.current() == '}', "Variable does not end with '}' at position %s", iterator.getIndex());
    // Consume it.
    iterator.next();
  }

  private void parseLiteral(final CharacterIterator iterator) {
    final String literal = readFragment(iterator, END_OF_LITERAL, ESCAPE_CHARS, NONE);
    tokens.add(new LiteralToken(literal));
  }

  /**
   * Reads from the character iterator until either a stop character is reached or the iterator is {@link
   * CharacterIterator#DONE done}. The stop character is not consumed. Escape characters indicate that the
   * following character should not be considered as a stop character.
   *
   * @throws IllegalArgumentException if the fragment isn't at least one character
   */
  static String readFragment(final CharacterIterator iterator,
                             final List<Character> stopChars,
                             final List<Character> escapeChars,
                             final List<Character> disallowed)
  {
    StringBuilder b = new StringBuilder();
    boolean escaping = false;
    while (true) {
      final char ch = iterator.current();

      if (ch == CharacterIterator.DONE) {
        checkArgument(!escaping,
            format("Unexpected end after escape character '%s' at position %s.", ch, iterator.getIndex()));

        checkArgument(b.length() >= 1, format("Zero-length fragment at position %s.", iterator.getIndex()));

        return b.toString();
      }

      if (escaping) {
        checkAllowed(iterator, disallowed);
        b.append(ch);
        escaping = false;
      }
      else if (escapeChars.contains(ch)) {
        escaping = true;
      }
      else if (stopChars.contains(ch)) {
        return b.toString();
      }
      else {
        checkAllowed(iterator, disallowed);
        b.append(ch);
      }

      iterator.next();
    }
  }

  private static void checkAllowed(final CharacterIterator iterator, final List<Character> disallowed) {
    checkArgument(!disallowed.contains(iterator.current()), "Disallowed character %s at position %s",
        iterator.current(), iterator.getIndex());
  }
}
