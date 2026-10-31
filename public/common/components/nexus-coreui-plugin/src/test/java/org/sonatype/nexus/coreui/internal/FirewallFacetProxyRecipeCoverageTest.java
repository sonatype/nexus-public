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
package org.sonatype.nexus.coreui.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.capability.firewall.FirewallSupportedFormat;

import org.junit.Test;

import static java.util.Map.entry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Drift guard for FIRE-956.
 * <p>
 * The backend Firewall format restriction is blanket: with the limited-support override enabled, any
 * proxy format may be configured for Firewall. The ExtJS repository settings forms are not — each
 * recipe declares its own facets, so a format whose recipe omits the Firewall facet is configurable
 * over REST but invisible in the UI.
 * <p>
 * That divergence is silent, which is what this test exists to prevent. Two distinct drifts are
 * guarded:
 * <ul>
 * <li>a new {@code *Proxy.js} recipe that forgets the Firewall facet entirely, or mounts it only in a
 * comment, or — for a format <em>inside</em> {@link FirewallSupportedFormat} — wraps an
 * otherwise-correct mount in a conditional, for instance by copy-pasting the FIRE-956 gate pattern
 * onto the wrong recipe; and</li>
 * <li>a recipe for a format <em>outside</em> {@link FirewallSupportedFormat} that mounts the facet
 * <em>unconditionally</em> — offering a control the backend rejects on save whenever the FIRE-956
 * override is off. Mere presence of the xtype is not enough to certify such a recipe as correct, so
 * these are additionally required to mount it exactly once, inside the block gated on the override's
 * {@code NX.State} key.</li>
 * </ul>
 * <p>
 * Only proxy recipes are checked. Firewall is proxy-only, enforced by
 * {@code FirewallConfigurationValidator}, so hosted and group recipes must not carry the facet.
 * <p>
 * This guard can be deleted if and when the facet is mounted centrally in
 * {@code RepositorySettingsForm} instead of per recipe, which would make the drift impossible rather
 * than merely detectable.
 */
public class FirewallFacetProxyRecipeCoverageTest
{
  /**
   * Resolved relative to this test class's own code source rather than the classpath or the process
   * working directory. {@code nexus-cloudui-plugin} depends on this module and ships its own copies of
   * these recipes at the same resource path, so a classloader lookup would return whichever copy came
   * first and the sibling guard would silently check the wrong set. Working-directory resolution
   * ({@code System.getProperty("basedir")}) is correct under surefire but silently environment
   * dependent.
   */
  private static final String RECIPE_DIR = "src/main/resources/static/rapture/NX/coreui/view/repository/recipe";

  private static final String FACET_CLASS = "NX.coreui.view.repository.facet.FirewallFacet";

  private static final String FACET_XTYPE = "nx-coreui-repository-firewall-facet";

  /**
   * NX.State key published by {@code FirewallLimitedSupportFormatsStateContributor}. Hand-duplicated
   * across 10+ recipe files with no shared JS constant, which is exactly why a misspelling must fail
   * the build rather than silently hide the facet.
   */
  private static final String OVERRIDE_STATE_KEY = "firewallLimitedSupportFormatsEnabled";

  /**
   * The one legal mount site for a limited-support format. Matched literally, spacing included: a
   * recipe that writes the condition some other way fails this guard loudly rather than being
   * silently treated as ungated.
   */
  private static final String GATE_CONDITION = "if (NX.State.getValue('" + OVERRIDE_STATE_KEY + "'))";

  /**
   * A floor, not an exact count — it guards against the recipe directory resolving to somewhere empty,
   * which would let every assertion below pass while checking nothing. Deliberately well under the
   * real count so adding or removing a format does not require touching it.
   */
  private static final int MIN_EXPECTED_PROXY_RECIPES = 20;

  /**
   * Recipe filename to NXRM format id. Explicit rather than inferred: {@code GolangProxy.js} is format
   * {@code go} and {@code PyPiProxy.js} is {@code pypi}, so any naming convention would need
   * exceptions anyway. Unmapped filenames fail loudly (see
   * {@link #everyProxyRecipeFilenameMapsToAFormat()}) so a newly added format cannot silently escape
   * the gating check.
   */
  private static final Map<String, String> RECIPE_FORMATS = Map.ofEntries(
      entry("AlpineProxy.js", "alpine"),
      entry("AnsiblegalaxyProxy.js", "ansiblegalaxy"),
      entry("AptProxy.js", "apt"),
      entry("CargoProxy.js", "cargo"),
      entry("CocoapodsProxy.js", "cocoapods"),
      entry("ComposerProxy.js", "composer"),
      entry("ConanProxy.js", "conan"),
      entry("CondaProxy.js", "conda"),
      entry("DockerProxy.js", "docker"),
      entry("GolangProxy.js", "go"),
      entry("HelmProxy.js", "helm"),
      entry("HuggingFaceProxy.js", "huggingface"),
      entry("Maven2Proxy.js", "maven2"),
      entry("NpmProxy.js", "npm"),
      entry("NugetProxy.js", "nuget"),
      entry("OciProxy.js", "oci"),
      entry("P2Proxy.js", "p2"),
      entry("PubProxy.js", "pub"),
      entry("PyPiProxy.js", "pypi"),
      entry("RProxy.js", "r"),
      entry("RawProxy.js", "raw"),
      entry("RubygemsProxy.js", "rubygems"),
      entry("SwiftProxy.js", "swift"),
      entry("TerraformProxy.js", "terraform"),
      entry("YumProxy.js", "yum"));

  /**
   * Derived from the backend enum, not hardcoded, so this guard tracks the source of truth.
   */
  private static final Set<String> SUPPORTED_FORMATS = Stream.of(FirewallSupportedFormat.getValues())
      .collect(Collectors.toUnmodifiableSet());

  /**
   * Supported formats whose recipe mounts the facet behind its own rollout flag, so the
   * "exactly one unconditional mount" requirement in
   * {@link #everyProxyRecipeDeclaresTheFirewallFacet()} does not apply to them.
   * <p>
   * {@code pub} gates on {@code firewallPubFormatEnabled} (which also carries a minimum IQ version) and
   * {@code swift} on {@code swiftFirewallEnabled} (NEXUS-53057). Both are inside
   * {@link FirewallSupportedFormat}, so neither is a FIRE-956 limited-support format and neither may be
   * gated on {@link #OVERRIDE_STATE_KEY}; their conditions are unrelated feature rollouts. Mirrors
   * {@code FLAG_GATED_FORMATS} in the React {@code utils/firewallFormats.ts}.
   * <p>
   * Keep this list minimal. Every entry is a recipe whose mount nothing in this class verifies
   * positionally, so adding a format here to make a build pass gives up real coverage.
   */
  private static final Set<String> ROLLOUT_GATED_SUPPORTED_FORMATS = Set.of("pub", "swift");

  /**
   * Every proxy recipe requires the facet class and mounts its xtype.
   * <p>
   * For a format inside {@link FirewallSupportedFormat} this additionally asserts, positionally and on
   * comment-blanked source, that there is <em>exactly one</em> mount and that it is
   * <em>unconditional</em>. Those recipes mount by design, so the raw {@code source.contains(xtype)}
   * check was the only thing covering them, and it is weak in both directions: it passes on a stale
   * comment that merely names the xtype, and it cannot notice a mount that was wrapped in a conditional
   * — for example by copy-pasting the FIRE-956 gate onto a supported-format recipe, which would hide
   * Firewall configuration for a format the backend accepts unconditionally. This mirrors the
   * positional logic in {@link #limitedSupportProxyRecipesGateTheFirewallFacet()}, inverted.
   * <p>
   * {@link #ROLLOUT_GATED_SUPPORTED_FORMATS} is exempt: {@code pub} and {@code swift} are inside the
   * enum but mount behind their own, unrelated feature-rollout flags.
   */
  @Test
  public void everyProxyRecipeDeclaresTheFirewallFacet() {
    List<Path> proxyRecipes = proxyRecipes();

    // Guard the guard: if the resource lookup silently found nothing this test would pass while
    // checking zero files, which is the failure mode that matters most here.
    assertTrue("found no *Proxy.js recipes under " + RECIPE_DIR
        + " — the path is probably wrong, not the recipes",
        proxyRecipes.size() >= MIN_EXPECTED_PROXY_RECIPES);

    List<String> missingRequires = new ArrayList<>();
    List<String> missingXtype = new ArrayList<>();
    List<String> checkedUnconditional = new ArrayList<>();
    List<String> notUnconditional = new ArrayList<>();

    for (Path recipe : proxyRecipes) {
      String raw = read(recipe);
      String name = recipe.getFileName().toString();
      if (!raw.contains(FACET_CLASS)) {
        missingRequires.add(name);
      }
      if (!raw.contains(FACET_XTYPE)) {
        missingXtype.add(name);
        continue;
      }

      String format = formatOf(name);
      if (!SUPPORTED_FORMATS.contains(format) || ROLLOUT_GATED_SUPPORTED_FORMATS.contains(format)) {
        continue;
      }
      checkedUnconditional.add(name);

      // Comments blanked in place, so reported line numbers still match the file on disk.
      String source = withCommentsBlanked(name, raw);
      List<Integer> mounts = occurrencesOf(source, FACET_XTYPE);
      if (mounts.size() != 1) {
        notUnconditional.add(name + " mounts it " + mounts.size() + " time(s) outside comments"
            + (mounts.isEmpty() ? "" : " (line " + lineNumbers(source, mounts) + ")") + ", expected once");
        continue;
      }

      int mount = mounts.get(0);
      List<int[]> enclosing = conditionalBlocks(name, source).stream()
          .filter(block -> mount > block[0] && mount < block[1])
          .toList();
      if (!enclosing.isEmpty()) {
        notUnconditional.add(name + " mounts it at line " + lineNumber(source, mount)
            + ", inside conditional block(s) spanning line(s) " + blockLines(source, enclosing));
      }
    }

    assertTrue("proxy recipes missing '" + FACET_CLASS + "' in requires: " + missingRequires
        + ". Add it unconditionally — requires is resolved by the ExtJS class loader, not initComponent.",
        missingRequires.isEmpty());

    assertTrue("proxy recipes never mounting xtype '" + FACET_XTYPE + "': " + missingXtype
        + ". Push it into me.items, gated on NX.State '" + OVERRIDE_STATE_KEY + "' for formats "
        + "outside FirewallSupportedFormat. See PubProxy.js.", missingXtype.isEmpty());

    // Guard the guard again: the supported-format branch must actually run. If RECIPE_FORMATS, the enum
    // lookup or the exemption set drifted so that every recipe was skipped, the assertion below would
    // pass while checking nothing.
    assertFalse("no supported-format proxy recipe was checked for an unconditional mount — every "
        + "recipe filename mapped to a format outside FirewallSupportedFormat or into "
        + ROLLOUT_GATED_SUPPORTED_FORMATS + ", which cannot be right while maven2/npm/docker recipes "
        + "exist. RECIPE_FORMATS, the enum lookup or the exemption set is wrong.",
        checkedUnconditional.isEmpty());

    assertTrue("these proxy recipes are for formats inside FirewallSupportedFormat, so they must mount "
        + "xtype '" + FACET_XTYPE + "' exactly once and unconditionally: " + notUnconditional
        + ". The backend accepts Firewall configuration for these formats whatever the FIRE-956 override "
        + "is set to, so a conditional mount hides a control that works — do not copy the "
        + GATE_CONDITION + " pattern onto them. Mentioning the xtype only in a comment does not count "
        + "either. Mount it plainly in the me.items literal (NpmProxy.js is the shape); "
        + ROLLOUT_GATED_SUPPORTED_FORMATS + " are exempt because they carry their own rollout flags.",
        notUnconditional.isEmpty());
  }

  /**
   * Presence of the facet xtype is necessary but not sufficient. For a format outside
   * {@link FirewallSupportedFormat} the backend rejects Firewall configuration unless the FIRE-956
   * override is on, so the mount must be conditional.
   * <p>
   * What is asserted is positional, and deliberately so: the facet xtype must occur <em>exactly
   * once</em> in the recipe, and that one occurrence must sit inside the braces of an
   * {@code if (NX.State.getValue('firewallLimitedSupportFormatsEnabled'))} block. Checking "mentions
   * the state key somewhere" and "is absent from the {@code me.items} literal" separately does not tie
   * the two to the same call site: a recipe naming the key only in a comment while mounting the facet
   * from an unconditional {@code me.items.push(...)} after the literal satisfies both, which is
   * precisely the ungated mount this guard exists to catch. Under the single positional assertion an
   * ungated mount fails wherever it is — in the items literal, pushed after it, or from another method
   * — as does a misspelled state key (no gate is then found at all) and a mount duplicated across a
   * gated and an ungated site.
   * <p>
   * Two details make the check honest. Comments are blanked before scanning, so neither the gate nor
   * the mount can be satisfied by prose. And the gated region is delimited by brace matching rather
   * than by the following {@code me.callParent()}: a mount sitting between the end of the gate block
   * and {@code me.callParent()} is ungated and must fail.
   * <p>
   * Recipes for the supported formats are exempt: they mount unconditionally by design. That includes
   * {@code PubProxy.js}, which is inside the enum and gates on its own, unrelated
   * {@code firewallPubFormatEnabled} flag.
   */
  @Test
  public void limitedSupportProxyRecipesGateTheFirewallFacet() {
    List<String> checked = new ArrayList<>();
    List<String> ungated = new ArrayList<>();

    for (Path recipe : proxyRecipes()) {
      String name = recipe.getFileName().toString();
      if (SUPPORTED_FORMATS.contains(formatOf(name))) {
        continue;
      }
      checked.add(name);

      // Comments blanked in place, so reported line numbers still match the file on disk.
      String source = withCommentsBlanked(name, read(recipe));
      List<Integer> mounts = occurrencesOf(source, FACET_XTYPE);

      if (mounts.size() != 1) {
        ungated.add(name + " mounts it " + mounts.size() + " time(s)"
            + (mounts.isEmpty() ? "" : " (line " + lineNumbers(source, mounts) + ")") + ", expected once");
        continue;
      }

      int mount = mounts.get(0);
      List<int[]> gates = gateBlocks(name, source);
      if (gates.isEmpty()) {
        ungated.add(name + " mounts it at line " + lineNumber(source, mount)
            + " but has no \"" + GATE_CONDITION + "\" block outside comments");
        continue;
      }
      if (gates.stream().noneMatch(gate -> mount > gate[0] && mount < gate[1])) {
        ungated.add(name + " mounts it at line " + lineNumber(source, mount)
            + ", outside the gate block(s) spanning line(s) " + blockLines(source, gates));
      }
    }

    assertFalse("no proxy recipe was checked — every recipe filename mapped to a format inside "
        + "FirewallSupportedFormat, which cannot be right while alpine/apt/helm/terraform "
        + "recipes exist. RECIPE_FORMATS or the enum lookup is wrong.", checked.isEmpty());

    assertTrue("these proxy recipes are for formats outside FirewallSupportedFormat, so their only "
        + "mount of xtype '" + FACET_XTYPE + "' must sit inside " + GATE_CONDITION + " { ... }: "
        + ungated
        + ". With the FIRE-956 override off, a mount reachable outside that block shows a repository "
        + "admin a Firewall dropdown the backend rejects on save; a mount reachable only under a "
        + "misspelled key never shows at all. Mentioning the state key in a comment does not count, "
        + "and neither does a second me.items.push(...) outside the gate. See PubProxy.js for the "
        + "shape.", ungated.isEmpty());
  }

  /**
   * Without this, a proxy recipe for a brand new format would be skipped by
   * {@link #limitedSupportProxyRecipesGateTheFirewallFacet()} rather than checked by it.
   */
  @Test
  public void everyProxyRecipeFilenameMapsToAFormat() {
    List<String> unmapped = new ArrayList<>();
    for (Path recipe : proxyRecipes()) {
      String name = recipe.getFileName().toString();
      if (!RECIPE_FORMATS.containsKey(name)) {
        unmapped.add(name);
      }
    }

    assertTrue("these *Proxy.js recipes have no RECIPE_FORMATS entry: " + unmapped
        + ". Map each to its NXRM format id (the value in the recipe's Format class, e.g. "
        + "GolangProxy.js -> 'go'), otherwise the gating check silently skips it.", unmapped.isEmpty());
  }

  /**
   * Firewall is proxy-only, so a hosted or group recipe carrying the facet would offer a control the
   * validator always rejects.
   */
  @Test
  public void noHostedOrGroupRecipeDeclaresTheFirewallFacet() {
    List<String> unexpected = new ArrayList<>();
    for (Path recipe : recipesMatching("Hosted.js", "Group.js")) {
      if (read(recipe).contains(FACET_XTYPE)) {
        unexpected.add(recipe.getFileName().toString());
      }
    }
    assertTrue("Firewall is proxy-only but these non-proxy recipes mount the facet: " + unexpected,
        unexpected.isEmpty());
  }

  private static String formatOf(final String recipeFileName) {
    String format = RECIPE_FORMATS.get(recipeFileName);
    if (format == null) {
      throw new IllegalStateException("no RECIPE_FORMATS entry for " + recipeFileName
          + " — add one so the FIRE-956 gating check covers it instead of skipping it");
    }
    return format;
  }

  private static List<Integer> occurrencesOf(final String source, final String needle) {
    List<Integer> indexes = new ArrayList<>();
    for (int i = source.indexOf(needle); i >= 0; i = source.indexOf(needle, i + needle.length())) {
      indexes.add(i);
    }
    return indexes;
  }

  /**
   * Start and end offsets of the braces of every {@link #GATE_CONDITION} block in the source, outermost
   * brace matched to its partner. Brace matching rather than "up to the next {@code me.callParent()}":
   * the latter would accept a mount placed after the gate block closes but before
   * {@code me.callParent()}, which is ungated.
   */
  private static List<int[]> gateBlocks(final String name, final String source) {
    List<int[]> blocks = new ArrayList<>();
    for (int gate = source.indexOf(GATE_CONDITION); gate >= 0; gate =
        source.indexOf(GATE_CONDITION, gate + GATE_CONDITION.length())) {
      blocks.add(braceBlock(name, source, gate + GATE_CONDITION.length()));
    }
    return blocks;
  }

  private static int[] braceBlock(final String name, final String source, final int from) {
    int open = source.indexOf('{', from);
    if (open < 0) {
      throw new IllegalStateException(name + " has a \"" + GATE_CONDITION + "\" with no following '{', so "
          + "this guard cannot tell what the gate encloses. Use a braced block");
    }
    // indexOf finds the next '{' anywhere later in the file, so the check above only catches a gate with
    // no brace after it *at all*. An unbraced gate — if (NX.State.getValue('...')) me.items.unshift(...);
    // — would sail past it and latch onto some later object-literal brace, brace-matching from there and
    // reporting a gate span unrelated to the gate. Require the brace to follow the condition immediately
    // so that shape fails loudly, as documented, instead of silently producing a wrong span.
    if (!source.substring(from, open).isBlank()) {
      throw new IllegalStateException(name + " has a \"" + GATE_CONDITION + "\" at line "
          + lineNumber(source, from) + " whose body is not a braced block: the next '{' is at line "
          + lineNumber(source, open) + ", with code in between. Use a braced block so this guard can "
          + "tell what the gate encloses");
    }
    return matchBraces(name, source, open);
  }

  /**
   * Start and end offsets of the brace pair opening at {@code open}, nested braces and quoted strings
   * skipped. Shared by {@link #braceBlock} and {@link #conditionalBlocks}, which differ only in how they
   * locate the opening brace.
   */
  private static int[] matchBraces(final String name, final String source, final int open) {
    int depth = 0;
    for (int i = open; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\'' || c == '"') {
        i = endOfStringLiteral(name, source, i);
      }
      else if (c == '{') {
        depth++;
      }
      else if (c == '}') {
        depth--;
        if (depth == 0) {
          return new int[]{open, i};
        }
      }
    }
    throw new IllegalStateException(name + " has an unbalanced block starting at line "
        + lineNumber(source, open));
  }

  /**
   * Start and end offsets of the body of every {@code if (...)} in the source, whatever its condition.
   * Used by {@link #everyProxyRecipeDeclaresTheFirewallFacet()} to assert that a supported-format mount
   * is <em>not</em> enclosed by one, which is the inverse of what
   * {@link #limitedSupportProxyRecipesGateTheFirewallFacet()} asserts with {@link #gateBlocks}.
   * <p>
   * Both braced and unbraced bodies are recognised. An unbraced body is spanned to its terminating
   * {@code ;} rather than rejected: {@code if (flag) me.items.unshift(...);} is a conditional mount and
   * must be caught, while an unbraced {@code if} anywhere else in a recipe — DockerProxy.js and
   * HuggingFaceProxy.js carry several helper methods full of them — must not fail this guard. That is
   * the opposite trade-off from {@link #braceBlock}, where the FIRE-956 gate shape is mandated and any
   * deviation should fail loudly.
   * <p>
   * {@code else if} needs no special handling: its {@code if} is matched like any other, and the body it
   * yields is the same one.
   */
  private static List<int[]> conditionalBlocks(final String name, final String source) {
    List<int[]> blocks = new ArrayList<>();
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\'' || c == '"') {
        i = endOfStringLiteral(name, source, i);
        continue;
      }
      int conditionStart = ifConditionStart(source, i);
      if (conditionStart < 0) {
        continue;
      }
      int conditionEnd = matchingParen(name, source, conditionStart);
      blocks.add(conditionalBody(name, source, conditionEnd + 1));
      i = conditionEnd;
    }
    return blocks;
  }

  /**
   * Offset of the {@code (} opening an {@code if} condition starting at {@code index}, or -1 if there is
   * no {@code if} keyword there. The preceding character is checked so identifiers ending in "if" are
   * not mistaken for the keyword.
   */
  private static int ifConditionStart(final String source, final int index) {
    if (!source.startsWith("if", index)) {
      return -1;
    }
    if (index > 0 && isIdentifierChar(source.charAt(index - 1))) {
      return -1;
    }
    int i = index + 2;
    while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
      i++;
    }
    return i < source.length() && source.charAt(i) == '(' ? i : -1;
  }

  private static boolean isIdentifierChar(final char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }

  private static int matchingParen(final String name, final String source, final int open) {
    int depth = 0;
    for (int i = open; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\'' || c == '"') {
        i = endOfStringLiteral(name, source, i);
      }
      else if (c == '(') {
        depth++;
      }
      else if (c == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    throw new IllegalStateException(name + " has an unbalanced \"if (\" condition at line "
        + lineNumber(source, open));
  }

  /**
   * Start and end offsets of an {@code if} body beginning at or after {@code from}, which is the offset
   * just past the condition's closing paren. See {@link #conditionalBlocks} for why an unbraced body is
   * spanned rather than rejected.
   */
  private static int[] conditionalBody(final String name, final String source, final int from) {
    int start = from;
    while (start < source.length() && Character.isWhitespace(source.charAt(start))) {
      start++;
    }
    if (start < source.length() && source.charAt(start) == '{') {
      return matchBraces(name, source, start);
    }
    for (int i = start; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\'' || c == '"') {
        i = endOfStringLiteral(name, source, i);
      }
      else if (c == ';') {
        // start - 1 so that an offset at start itself counts as enclosed, matching the exclusive
        // containment test callers use for brace spans.
        return new int[]{start - 1, i};
      }
    }
    throw new IllegalStateException(name + " has an unbraced \"if (...)\" at line "
        + lineNumber(source, from) + " with no terminating ';'");
  }

  /**
   * The source with every comment overwritten by spaces, newlines preserved so offsets and line numbers
   * still line up with the file on disk. Without this a comment could satisfy either half of the gating
   * check: naming the state key in prose, or appearing to enclose a mount that is really outside the
   * gate.
   * <p>
   * Assumes the plain ExtJS recipe dialect actually used here — single and double quoted strings, no
   * template literals and no regex literals. A recipe using either will fail this guard loudly rather
   * than silently, which is the right direction to fail in.
   */
  private static String withCommentsBlanked(final String name, final String source) {
    char[] blanked = source.toCharArray();
    int i = 0;
    while (i < blanked.length) {
      char c = source.charAt(i);
      if (c == '\'' || c == '"') {
        i = endOfStringLiteral(name, source, i) + 1;
      }
      else if (c == '/' && i + 1 < blanked.length && source.charAt(i + 1) == '/') {
        while (i < blanked.length && source.charAt(i) != '\n') {
          blanked[i++] = ' ';
        }
      }
      else if (c == '/' && i + 1 < blanked.length && source.charAt(i + 1) == '*') {
        int end = source.indexOf("*/", i + 2);
        if (end < 0) {
          throw new IllegalStateException(name + " has an unterminated block comment at line "
              + lineNumber(source, i));
        }
        for (; i < end + 2; i++) {
          if (blanked[i] != '\n') {
            blanked[i] = ' ';
          }
        }
      }
      else {
        i++;
      }
    }
    return new String(blanked);
  }

  /**
   * Offset of the closing quote of the string literal opening at {@code openQuote}.
   */
  private static int endOfStringLiteral(final String name, final String source, final int openQuote) {
    char quote = source.charAt(openQuote);
    for (int i = openQuote + 1; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\\') {
        i++;
      }
      else if (c == quote) {
        return i;
      }
      else if (c == '\n') {
        break;
      }
    }
    throw new IllegalStateException(name + " has an unterminated " + quote + " string literal at line "
        + lineNumber(source, openQuote) + ". This guard's scanner assumes plain ExtJS recipes: single and "
        + "double quoted strings, no template literals, no regex literals");
  }

  private static String lineNumbers(final String source, final List<Integer> indexes) {
    return indexes.stream()
        .map(index -> String.valueOf(lineNumber(source, index)))
        .collect(Collectors.joining(", "));
  }

  private static String blockLines(final String source, final List<int[]> blocks) {
    return blocks.stream()
        .map(block -> lineNumber(source, block[0]) + "-" + lineNumber(source, block[1]))
        .collect(Collectors.joining(", "));
  }

  private static int lineNumber(final String source, final int index) {
    int line = 1;
    for (int i = 0; i < index; i++) {
      if (source.charAt(i) == '\n') {
        line++;
      }
    }
    return line;
  }

  private static List<Path> proxyRecipes() {
    return recipesMatching("Proxy.js");
  }

  private static List<Path> recipesMatching(final String... suffixes) {
    Path dir = recipeDir();
    try (Stream<Path> files = Files.list(dir)) {
      return files
          .filter(Files::isRegularFile)
          .filter(path -> {
            String name = path.getFileName().toString();
            return Stream.of(suffixes).anyMatch(name::endsWith);
          })
          .sorted()
          .toList();
    }
    catch (IOException e) {
      throw new UncheckedIOException("failed to list " + dir, e);
    }
  }

  /**
   * Walks up from this test class's own code source (normally {@code target/test-classes}) to the
   * module root. Unlike the process working directory this is unambiguously <em>this</em> module's
   * tree regardless of how the test is launched.
   */
  private static Path recipeDir() {
    Path codeSource = codeSourceLocation();
    Path candidate = codeSource;
    for (int i = 0; i < 4 && candidate != null; i++) {
      Path dir = candidate.resolve(RECIPE_DIR);
      if (Files.isDirectory(dir)) {
        return dir;
      }
      candidate = candidate.getParent();
    }
    throw new IllegalStateException("recipe directory '" + RECIPE_DIR + "' not found within 4 parents of "
        + codeSource + " — the module layout changed, or this test is running from an unexpected "
        + "artifact");
  }

  private static Path codeSourceLocation() {
    CodeSource codeSource = FirewallFacetProxyRecipeCoverageTest.class.getProtectionDomain().getCodeSource();
    if (codeSource == null || codeSource.getLocation() == null) {
      throw new IllegalStateException("could not resolve this test class's code source, so the recipe "
          + "directory cannot be located");
    }
    try {
      return Paths.get(codeSource.getLocation().toURI());
    }
    catch (URISyntaxException e) {
      throw new IllegalStateException("this test class's code source location " + codeSource.getLocation()
          + " is not a usable path, so the recipe directory cannot be located", e);
    }
  }

  private static String read(final Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException("failed to read " + path, e);
    }
  }
}
