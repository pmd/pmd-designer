/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import com.beust.jcommander.Parameter;

@Deprecated
final class MainCliArgs {


    @Parameter(names = {"--verbose", "-v", "-D", "--debug"},
               arity = 0,
               description = "Whether to launch the app in verbose mode. "
                   + "This enables logging of exception stack traces and "
                   + "very verbose tracing in the event log of the app.")
    boolean verbose;

    @Parameter(names = {"--help", "-h"},
               help = true,
               description = "Display this help text")
    boolean help;


}
