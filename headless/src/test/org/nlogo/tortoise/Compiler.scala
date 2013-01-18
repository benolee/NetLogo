// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.tortoise

import
  org.nlogo.{ api, nvm, workspace },
  org.nlogo.util.Femto

object Compiler {

  // two main entry points. input is NetLogo, result is JavaScript.

  def compileReporter(logo: String): String =
    compile(logo, commands = false)

  def compileCommands(logo: String): String =
    compile(logo, commands = true)

  ///

  val compiler: nvm.CompilerInterface =
    Femto.scalaSingleton(classOf[nvm.CompilerInterface],
      "org.nlogo.compiler.Compiler")

  // Turning off constant folding in the NetLogo compiler makes it easier to write tests, since with
  // constant folding off, a simple test case like "2 + 2" gets compiled into "_plus(_constdouble:2.0,
  // _constdouble:2.0)" instead of "_constdouble:4.0". - ST 1/16/13

  val flags = nvm.CompilerFlags(foldConstants = false, useGenerator = false)

  // How this works:
  // - the header/footer stuff wraps the code in `to` or `to-report`
  // - the compile returns an nvm.CompilerResults, whose head is a
  //   nvm.Procedure containing an Array[Command] (the procedure body)
  // - in the reporter case, the procedure body starts with the `report` command, so the
  //   actual reporter is the first (and only) argument to that

  def compile(logo: String, commands: Boolean): String = {
    val wrapped =
      workspace.Evaluator.getHeader(api.AgentKind.Observer, commands) +
        logo + workspace.Evaluator.getFooter(commands)
    val results =
      compiler.compileMoreCode(wrapped, None, api.Program.empty(),
        nvm.CompilerInterface.NoProcedures, new api.DummyExtensionManager, flags)
    // results is nvm.CompilerResults, results.head is a Procedure
    if (commands)
      generateCommands(results.head.code)
    else
      generateReporter(results.head.code.head.args(0))
  }

  ///

  def generateCommands(cs: Seq[nvm.Command]): String =
    cs.map(generateCommand)
      .filter(_.nonEmpty)
      .mkString("(function () {\n", "\n", "}).call(this);")

  ///

  def generateCommand(c: nvm.Command): String = {
    def args = c.args.map(generateReporter).mkString(", ")
    c match {
      case Prims.SpecialCommand(op) =>
        op
      case Prims.NormalCommand(op) =>
        s"$op($args);"
    }
  }

  def generateReporter(r: nvm.Reporter): String = {
    def arg(i: Int) =
      generateReporter(r.args(i))
    def args = r.args.map(generateReporter).mkString(", ")
    r match {
      case pure: nvm.Pure if pure.args.isEmpty =>
        compileLiteral(pure.report(null))
      case Prims.InfixReporter(op) =>
        s"(${arg(0)} $op ${arg(1)})"
      case Prims.NormalReporter(op) =>
        s"$op($args)"
    }
  }

  def compileLiteral(x: AnyRef): String =
    x match {
      case ll: api.LogoList =>
        ll.map(compileLiteral).mkString("[", ", ", "]")
      case x =>
        api.Dump.logoObject(x, readable = true, exporting = false)
    }

}
