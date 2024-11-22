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
package org.sonatype.nexus.extjs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mozilla.javascript.Token.CALL;
import static org.mozilla.javascript.Token.NEW;

/**
 * ExtJS 4+ class definition scanner.
 *
 * @since 3.0
 */
public class ClassDefScanner
{
  private final Map<String, ClassDef> classes = new HashMap<>();

  private final Log log;

  private boolean warnings = false;

  private String namespace;

  public ClassDefScanner(final Log log) {
    this.log = checkNotNull(log);
  }

  public boolean isWarnings() {
    return warnings;
  }

  public void setWarnings(final boolean warnings) {
    this.warnings = warnings;
  }

  @Nullable
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(@Nullable final String namespace) {
    this.namespace = namespace;
  }

  /**
   * Scan a set of files and return a list of class definitions in dependency order.
   * ie. Foo extends Bar, Bar will be before Foo.
   */
  public List<ClassDef> scan(final Scanner files) throws Exception {
    checkNotNull(files);

    final boolean debug = log.isDebugEnabled();

    // reset and scan for files
    classes.clear();

    log.debug("Finding files to scan...");
    files.scan();

    // scan each file
    String[] included = files.getIncludedFiles();
    log.debug("Scanning " + included.length + " files...");
    for (String path : included) {
      File file = new File(files.getBasedir(), path);
      scan(file);
    }

    // TODO: Sort out how/if we want to resolve aliases, ATM this data is collected by not used

    // Resolve all references and build map with all class defs (including aliases/alts)
    Map<String, ClassDef> classNames = new HashMap<>();
    List<ClassDef> allClasses = new ArrayList<>();
    for (ClassDef def : classes.values()) {
      log.debug("Processing: " + def);

      classNames.put(def.getName(), def);
      for (String alt : def.getAlternateClassName()) {
        classNames.put(alt, def);
      }

      resolve(def);

      allClasses.add(def);
    }

    // Sort all classes by priority, so that higher-priority value is closer to the start and processed sooner
    // ie. priority=10 is closer to the start than priority=1
    Collections.sort(allClasses, new Comparator<ClassDef>()
    {
      @Override
      public int compare(final ClassDef a, final ClassDef b) {
        return Double.compare(b.getPriority(), a.getPriority());
      }
    });

    if (debug) {
      log.debug("All classes:");
      for (ClassDef def : allClasses) {
        log.debug("  " + def.getName());
      }
    }

    // build the graph
    DAG graph = new DAG();
    for (ClassDef def : allClasses) {
      graph.addVertex(def.getName());
      for (String name : def.getDependencies()) {
        // resolve dependencies which have class defs to primary class name
        ClassDef dep = classNames.get(name);
        if (dep != null) {
          name = dep.getName();
        }
        graph.addEdge(def.getName(), name);
      }
    }

    // display some debug information about the graph
    if (debug) {
      log.debug("Vertices:");
      for (Vertex v : graph.getVerticies()) {
        log.debug("  " + v.getLabel());
        if (!v.getParents().isEmpty()) {
          log.debug("    parents:");
          for (Vertex parent : v.getParents()) {
            log.debug("      " + parent.getLabel());
          }
        }
        if (!v.getChildren().isEmpty()) {
          log.debug("    children:");
          for (Vertex child : v.getChildren()) {
            log.debug("      " + child.getLabel());
          }
        }
      }
    }

    // result sorted list of class definitions
    Set<ClassDef> results = new LinkedHashSet<>();
    log.debug("Ordered classes:");
    // the graph contains many references, only include those which are class defs
    for (String className : TopologicalSorter.sort(graph)) {
      ClassDef def = classNames.get(className);
      // skip duplicates (due to alt/aliases)
      if (def != null && !results.contains(def)) {
        log.debug("  " + def.getName());
        results.add(def);
      }
    }

    log.debug("Found " + results.size() + " classes");
    return new ArrayList<>(results);
  }

  /**
   * Resolve all references.
   */
  private void resolve(final ClassDef def) {
    // resolve mvc references if namespace is set
    if (namespace != null) {
      if (def.getMvcControllers() != null) {
        for (String name : def.getMvcControllers()) {
          appendMvcDependency(def, "controller", name);
        }
      }
      if (def.getMvcModels() != null) {
        for (String name : def.getMvcModels()) {
          appendMvcDependency(def, "model", name);
        }
      }
      if (def.getMvcStores() != null) {
        for (String name : def.getMvcStores()) {
          appendMvcDependency(def, "store", name);
        }
      }
      if (def.getMvcViews() != null) {
        for (String name : def.getMvcViews()) {
          appendMvcDependency(def, "view", name);
        }
      }
    }
  }

  private void appendMvcDependency(final ClassDef def, final String type, final String name) {
    if (classes.containsKey(name)) {
      // already qualified name
      def.getDependencies().add(name);
    }
    else {
      if (name.contains("@")) {
        // namespace is embedded: <name>@<namespace>
        String[] parts = name.split("@", 2);
        def.getDependencies().add(String.format("%s.%s", parts[1], parts[0]));
      }
      else {
        def.getDependencies().add(String.format("%s.%s.%s", namespace, type, name));
      }
    }
  }

  /**
   * Scan the given file for class definitions and accumulate dependencies.
   */
  private void scan(final File source) throws IOException {
    log.debug("Scanning: " + source);

    ErrorReporter errorReporter = new LogErrorReporter(log);

    CompilerEnvirons env = new CompilerEnvirons();
    env.setErrorReporter(errorReporter);

    Parser parser = new Parser(env, errorReporter);
    Reader reader = new BufferedReader(new FileReader(source));
    try {
      AstRoot root = parser.parse(reader, source.getAbsolutePath(), 0);
      DependencyAccumulator visitor = new DependencyAccumulator(source);
      root.visit(visitor);

      // complain if no def was found in this source
      if (visitor.current == null) {
        log.warn("No class definition was found while processing: " + source);
      }
    }
    finally {
      reader.close();
    }
  }

  private class DependencyAccumulator
      implements NodeVisitor
  {
    private final File source;

    private ClassDef current;

    private DependencyAccumulator(final File source) {
      this.source = source;
    }

    /**
     * Helper to report error for given node position in ast tree.
     */
    private RuntimeException reportError(final AstNode node, final String message, Object... params) {
      throw Context.reportRuntimeError(String.format(message, params),
          source.getAbsolutePath(),
          node.getLineno(), // seems to always be the line before?
          node.debugPrint(),
          0 // line-offset is unknown?
      );
    }

    /**
     * Helper to check state and report error for given node position in ast tree.
     */
    private void checkState(boolean expression, final AstNode node, final String message, Object... params) {
      if (!expression) {
        throw reportError(node, message, params);
      }
    }

    @Override
    public boolean visit(final AstNode node) {
      int type = node.getType();

      switch (type) {
        case CALL: {
          FunctionCall call = (FunctionCall) node;
          String name = nameOf(call.getTarget());

          // if we can not determine the function name, skip
          if (name == null) {
            break;
          }

          if (name.equals("Ext.define")) {
            // complain if we found more than one class
            if (current != null) {
              log.warn("Found duplicate class definition in source: " + source);
            }
            processClassDef(call);
            return true; // process children
          }
          else if (name.equals("Ext.create")) {
            // complain if we have references to classes with Ext.create() and missing requires
            if (!call.getArguments().isEmpty() && call.getArguments().get(0) instanceof StringLiteral) {
              String className = nameOf(call.getArguments().get(0));
              maybeWarnMissingRequires(className, "Ext.create");
            }
            return false; // ignore children
          }
          break;
        }

        case NEW: {
          // complain if we have references to classes with 'new' and missing requires
          NewExpression expr = (NewExpression) node;
          String className = nameOf(expr.getTarget());
          maybeWarnMissingRequires(className, "new");
          break;
        }
      }

      return true; // process children
    }

    /**
     * Find the textual name of the given node.
     */
    @Nullable
    private String nameOf(final AstNode node) {
      if (node instanceof Name) {
        return ((Name) node).getIdentifier();
      }
      else if (node instanceof PropertyGet) {
        PropertyGet prop = (PropertyGet) node;
        return String.format("%s.%s", nameOf(prop.getTarget()), nameOf(prop.getProperty()));
      }
      else if (node instanceof StringLiteral) {
        return ((StringLiteral) node).getValue();
      }
      return null;
    }

    /**
     * Return string literal value.
     */
    private String stringLiteral(final AstNode node) {
      checkState(node instanceof StringLiteral, node, "Expected string literal only");
      // noinspection ConstantConditions
      StringLiteral string = (StringLiteral) node;
      return string.getValue();
    }

    /**
     * Return number literal value.
     */
    private double numberLiteral(final AstNode node) {
      checkState(node instanceof NumberLiteral, node, "Expected number literal only");
      // noinspection ConstantConditions
      NumberLiteral number = (NumberLiteral) node;
      return number.getNumber();
    }

    /**
     * Returns string array literal values.
     */
    private List<String> arrayStringLiteral(final AstNode node) {
      checkState(node instanceof ArrayLiteral, node, "Expected array literal only");
      List<String> result = new ArrayList<>();
      // noinspection ConstantConditions
      ArrayLiteral array = (ArrayLiteral) node;
      for (AstNode element : array.getElements()) {
        result.add(stringLiteral(element));
      }
      return result;
    }

    /**
     * Returns string literal or array of string literals.
     *
     * @see #stringLiteral(AstNode)
     * @see #arrayStringLiteral(AstNode)
     */
    private List<String> stringLiterals(final AstNode node) {
      // string literal or array of string literals
      if (node instanceof StringLiteral) {
        return Collections.singletonList(stringLiteral(node));
      }
      else if (node instanceof ArrayLiteral) {
        return arrayStringLiteral(node);
      }
      else {
        throw reportError(node, "Expected string literal or array of string literal only");
      }
    }

    /**
     * Maybe warn if a needed class has not been required.
     */
    private void maybeWarnMissingRequires(final String className, final String usage) {
      if (warnings) {
        if (!current.getDependencies().contains(className)) {
          log.warn(String.format("Class '%s' missing requires for '%s' usage of: %s",
              current.getName(), usage, className));
        }
      }
    }

    /**
     * Process an {@code Ext.define} class definition.
     */
    private void processClassDef(final FunctionCall node) {
      List<AstNode> args = node.getArguments();
      checkState(args.size() >= 2, node,
          "Invalid number of arguments for Ext.define"); // simple def or def with callback

      // class-name
      checkState(args.get(0) instanceof StringLiteral, node, "Ext.define arg[0] must be string");
      String className = nameOf(args.get(0));

      // try and avoid file-name/class-name mismatches early
      sanityCheckClassName(className);

      current = new ClassDef(className, source);
      classes.put(className, current);

      // class def object
      checkState(args.get(1) instanceof ObjectLiteral, node, "Ext.define arg[1] must be object");
      ObjectLiteral obj = (ObjectLiteral) args.get(1);
      for (ObjectProperty prop : obj.getElements()) {
        String name = nameOf(prop.getLeft());
        switch (name) {

          // ExtJS core class def

          case "extend":
            // string literal only
            current.setExtend(stringLiteral(prop.getRight()));
            break;

          case "override":
            // string literal only
            current.setOverride(stringLiteral(prop.getRight()));
            break;

          case "@aggregate_priority":
            // number only
            current.setPriority(numberLiteral(prop.getRight()));
            break;

          case "requires":
            // array of string literals only
            current.setRequires(arrayStringLiteral(prop.getRight()));
            break;

          case "require":
            // complain if we found 'require' this almost certainly should be 'requires'
            log.warn(
                String.format("Found 'require' and probably should be 'requires' in: %s#%s", source, prop.getLineno()));
            break;

          case "uses":
            // array of string literals only
            current.setUses(arrayStringLiteral(prop.getRight()));
            break;

          case "alternateClassName": {
            // string literal or array of string literals
            current.getAlternateClassName().addAll(stringLiterals(prop.getRight()));
            break;
          }

          case "alias": {
            // string literal or array of string literals
            current.getAlias().addAll(stringLiterals(prop.getRight()));
            break;
          }

          case "xtype": {
            // string literal only
            current.getAlias().add("widget." + stringLiteral(prop.getRight()));
            break;
          }

          case "mixins": {
            // array of strings, or object
            List<String> mixins = new ArrayList<>();
            if (prop.getRight() instanceof ArrayLiteral) {
              mixins.addAll(arrayStringLiteral(prop.getRight()));
            }
            else if (prop.getRight() instanceof ObjectLiteral) {
              ObjectLiteral child = (ObjectLiteral) prop.getRight();
              for (ObjectProperty element : child.getElements()) {
                mixins.add(stringLiteral(element.getRight()));
              }
            }
            else {
              throw reportError(prop.getRight(), "Expected array or object literal only");
            }
            current.setMixins(mixins);
            break;
          }
        }

        // Additional stuff we have to detect for ExtJS MVC support

        if (isExtends("Ext.app.Application")) {
          // class looks like an application, process more fields
          switch (name) {
            case "controllers":
              // array of string literals only
              current.setMvcControllers(arrayStringLiteral(prop.getRight()));
              break;

            case "models":
              // array of string literals only
              current.setMvcModels(arrayStringLiteral(prop.getRight()));
              break;

            case "stores":
              // array of string literals only
              current.setMvcStores(arrayStringLiteral(prop.getRight()));
              break;

            case "views":
              // array of string literals only
              current.setMvcViews(arrayStringLiteral(prop.getRight()));
              break;
          }
        }
        else if (isMvcClass("controller")) {
          // class looks like a controller, process more fields
          switch (name) {
            case "models":
              // array of string literals only
              current.setMvcModels(arrayStringLiteral(prop.getRight()));
              break;

            case "stores":
              // array of string literals only
              current.setMvcStores(arrayStringLiteral(prop.getRight()));
              break;

            case "views":
              // array of string literals only
              current.setMvcViews(arrayStringLiteral(prop.getRight()));
              break;
          }
        }
        else if (isMvcClass("store")) {
          // class looks like a store, process more fields
          switch (name) {
            case "model":
              // string literal only
              current.getDependencies().add(stringLiteral(prop.getRight()));
              break;
          }
        }
      }
    }

    /**
     * Complain if classes are defined with wrong names, or in wrong files.
     */
    private void sanityCheckClassName(final String className) {
      String found = source.toURI().getPath();
      String expected = className.replace('.', '/') + ".js";
      if (!found.endsWith(expected)) {
        log.warn(String.format("Expected class '%s' to be defined in filename '%s', but was defined in: %s",
            className, expected, found));
      }
    }

    /**
     * Highly hackish means to determine given class-name is a MVC type.
     * Requires namespace to be configured.
     */
    private boolean isMvcClass(final String type) {
      return namespace != null && current.getName().startsWith(String.format("%s.%s.", namespace, type));
    }

    /**
     * Check if current class extends (directly) given class-name.
     */
    private boolean isExtends(final String className) {
      String superClass = current.getExtend();
      return superClass != null && superClass.equals(className);
    }
  }
}
