/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import com.beust.jcommander.Parameter;

final class MainCliArgs {


    static final String HL_AST_DUMP = "--hl-ast-dump";
    @Parameter(names = {HL_AST_DUMP},
               arity = 1,
               description = "Headless XML dump of an AST. "
                   + "The argument to this option is the language to use. "
                   + "The content to parse is received from standard input,"
                   + "and output to standard output.")
    String dumpXml;

    @Parameter(names = {"--verbose", "-v"},
               arity = 1,
               description = "Whether to launch the app in verbose mode. "
                   + "This enables logging of exception stack traces and "
                   + "very verbose tracing in the event log of the app.")
    boolean verbose;

    @Parameter(names = {"--help", "-h"},
               help = true,
               description = "Display this help text")
    boolean help;


}
