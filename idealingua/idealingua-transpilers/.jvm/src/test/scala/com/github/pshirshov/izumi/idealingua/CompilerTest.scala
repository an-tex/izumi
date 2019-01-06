package com.github.pshirshov.izumi.idealingua

import com.github.pshirshov.izumi.fundamentals.platform.files.IzFiles
import org.scalatest.WordSpec


class CompilerTest extends WordSpec {

  import IDLTestTools._

  "IDL compiler" should {
    "be able to compile into scala" in {
      assume(IzFiles.haveExecutables("scalac"), "scalac not available")
      assert(compilesScala(getClass.getSimpleName, loadDefs()))
      assert(compilesScala(s"${getClass.getSimpleName}-nonportable", loadDefs("/defs/scala")))
    }

    "be able to compile into typescript" in {
      assume(IzFiles.haveExecutables("tsc"), "tsc not available")
      assume(IzFiles.haveExecutables("npm"), "npm not available")
      assume(IzFiles.haveExecutables("yarn"), "yarn not available")
      assert(compilesTypeScript(s"${getClass.getSimpleName}-scoped", loadDefs(), scoped = true))
      assert(compilesTypeScript(s"${getClass.getSimpleName}-plain", loadDefs(), scoped = false))
    }

    "be able to compile into golang" in {
      assume(IzFiles.haveExecutables("go"), "go not available")
      assert(compilesGolang(s"${getClass.getSimpleName}-scoped", loadDefs(), repoLayout = true))
      assert(compilesGolang(s"${getClass.getSimpleName}-plain", loadDefs(), repoLayout = false))
    }

    "be able to compile into csharp" in {
      assume(IzFiles.haveExecutables("csc", "nunit-console"), "csc not available")
      assert(compilesCSharp(getClass.getSimpleName, loadDefs()))
    }
  }
}

