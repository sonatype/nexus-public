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

import groovy.io.FileType
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import jdk.nashorn.internal.ir.CallNode
import jdk.nashorn.internal.ir.FunctionNode
import jdk.nashorn.internal.ir.LexicalContext
import jdk.nashorn.internal.ir.Node
import jdk.nashorn.internal.ir.ObjectNode
import jdk.nashorn.internal.ir.PropertyNode
import jdk.nashorn.internal.ir.visitor.NodeVisitor
import jdk.nashorn.internal.parser.Parser
import jdk.nashorn.internal.runtime.Context
import jdk.nashorn.internal.runtime.ErrorManager
import jdk.nashorn.internal.runtime.Source
import jdk.nashorn.internal.runtime.options.Options

@Slf4j
@Grab('ch.qos.logback:logback-classic:1.1.2')
/**
 * Parse Javascript files named 'PluginStrings.js' for i18n definitions and compare those to all calls against the
 * NX.I18n service to find calls which would result in error.
 */
class NashornExtI18nParser
{
  private final List<File> folders

  private final Options options

  NashornExtI18nParser(final List<File> folders) {
    this.folders = folders
    folders.each { assert it.exists() && it.isDirectory(), "File doesn't exist or isn't a folder: $it" }
    this.options = new Options('nashorn')
    options.set('anon.functions', true)
    options.set('parse.only', true)
    options.set('scripting', true)
  }

  Map<String, Object> parse() {
    def candidates = []
    def pluginStringsFiles = []

    folders.each { target ->
      target.eachFileRecurse(FileType.FILES) { File file ->
        if (file.name.endsWith('.js')) {
          if (file.name == 'PluginStrings.js') {
            pluginStringsFiles << file
          }
          else {
            if(!shouldIgnore(file)) {
              candidates << file
            }
          }
        }
      }
    }

    def keys = [] as TreeSet
    pluginStringsFiles.each {
      log.debug "Processing PluginStrings.js: $it"
      def pluginStrings = parsePluginStrings(it)
      keys.addAll(pluginStrings)
      log.debug "Added ${pluginStrings.size()} keys"
    }

    LexicalContext lexicalContext = new LexicalContext()
    NodeVisitor i18nVisitor = new I18NVisitor(lexicalContext)
    NodeVisitor labelHelpVisitor = new LabelHelpVisitor(lexicalContext)

    candidates.each { File file ->
      log.debug "Processing candidate: $file"
      parseJS(file).accept(lexicalContext, i18nVisitor)
      parseJS(file).accept(lexicalContext, labelHelpVisitor)
    }

    Set<String> usedKeys = i18nVisitor.keys
    def unusedKeys = keys - usedKeys
    def undefinedKeys = usedKeys - keys
    return [
        'Available key size': keys.size(),
        'Used key size'     : usedKeys.size(),
        'Unused keys'       : unusedKeys,
        'Undefined keys'    : undefinedKeys,
        'Number of fields with help text' : labelHelpVisitor.fieldsWithHelp.size(),
        'Number of fields without help text' : labelHelpVisitor.fieldsWithoutHelp.size(),
        'Fields without help' : labelHelpVisitor.fieldsWithoutHelp
    ]
  }

  private List<String> parsePluginStrings(File pluginStrings) {
    FunctionNode functionNode = parseJS(pluginStrings)

    def extDefine = functionNode.body.statements[0]
    def defineArgs = extDefine.expression.args[1]
    def keysField = defineArgs.elements.find { it.keyName == 'keys' }
    return keysField.value.elements.collect { it.keyName }
  }

  private FunctionNode parseJS(File jsFile) {
    ErrorManager errors = new ErrorManager()
    Context context = new Context(options, errors, Thread.currentThread().getContextClassLoader())
    return new Parser(context.getEnv(), Source.sourceFor('temp', jsFile.text), errors).parse()
  }

  private boolean shouldIgnore(File file) {
    def isDev = file.absolutePath.split('/').contains('dev')
    if(isDev) {
      log.trace("Ignoring $file")
    }
    return isDev
  }
}

@Slf4j
class I18NVisitor
    extends NodeVisitor
{

  final Set<String> keys = [] as TreeSet

  I18NVisitor(final LexicalContext lc) {
    super(lc)
  }

  @Override
  protected boolean enterDefault(final Node node) {
    if (node.getClass() == CallNode && node.toString().contains('NX.I18n') &&
        (node.function.property == 'get' || node.function.property == 'format')) {
      log.trace('Found key: {}',node.args[0].value)
      keys << node.args[0].value
    }
    return true
  }
}

@Slf4j
class LabelHelpVisitor
    extends NodeVisitor
{
  static final Closure KEY_FINDER = { String keyName, Node n ->
    n.getClass() == PropertyNode && n.keyName == keyName
  }
  static final Closure FIELD_LABEL = KEY_FINDER.curry('fieldLabel')
  static final Closure HELP_TEXT = KEY_FINDER.curry('helpText')

  final Set<String> fieldsWithHelp = [] as TreeSet
  final Set<String> fieldsWithoutHelp = [] as TreeSet

  LabelHelpVisitor(final LexicalContext lc) {
    super(lc)
  }

  @Override
  protected boolean enterDefault(final Node node) {
    if (node.getClass() == ObjectNode && node.elements.find(FIELD_LABEL)) {
      PropertyNode fieldLabel = node.elements.find(FIELD_LABEL)
      PropertyNode helpText = node.elements.find(HELP_TEXT)
      log.trace("   Found fieldLabel: ${fieldLabel.keyName} with value: ${fieldLabel.value} and helpText: ${helpText?.value}")
      if(helpText) {
        fieldsWithHelp << fieldLabel.value.toString()
      }
      else {
        fieldsWithoutHelp << fieldLabel.value.toString()
      }
    }
    return true
  }
}

/**
 * Default if run from parent of nexus-oss/nexus-pro folders, can be overridden by space separated command line
 * parameters
 */
def targetFolders = args ?: [
    'plugins/nexus-coreui-plugin/src/main/resources/static/rapture/NX/coreui',
    'private/plugins/nexus-proui-plugin/src/main/resources/static/rapture/NX/proui',
    'components/nexus-rapture/src/main/resources/static/rapture/NX'
]
println JsonOutput.
    prettyPrint(JsonOutput.
        toJson(new NashornExtI18nParser(targetFolders.collect { String folder -> new File(folder) }).parse()))
