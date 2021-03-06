package izumi.distage.plugins.load

import io.github.classgraph.ClassGraph
import izumi.distage.plugins.{PluginBase, PluginConfig, PluginDef}
import izumi.functional.Value
import izumi.fundamentals.platform.cache.SyncCache

import scala.jdk.CollectionConverters._

class PluginLoaderDefaultImpl extends PluginLoader {
  override def load(config: PluginConfig): Seq[PluginBase] = {
    /** Disable scanning if no packages are specified (enable `_root_` package if you really want to scan everything) */
    val loadedPlugins = if (config.packagesEnabled.isEmpty && config.packagesDisabled.isEmpty) {
      Seq.empty
    } else {
      scanClasspath(config)
    }

    applyOverrides(loadedPlugins, config)
  }

  protected[this] def scanClasspath(config: PluginConfig): Seq[PluginBase] = {
    val enabledPackages = config.packagesEnabled.filterNot(p => config.packagesDisabled.contains(p) || p == "_root_")
    val disabledPackages = config.packagesDisabled

    val pluginBase = classOf[PluginBase]
    val pluginDef = classOf[PluginDef[_]]
    val whitelistedClasses = Seq(pluginDef.getName)

    def loadPkgs(pkgs: Seq[String]): Seq[PluginBase] = {
      PluginLoaderDefaultImpl.doLoad[PluginBase](pluginBase.getName, whitelistedClasses, pkgs, disabledPackages, config.debug)
    }

    if (!config.cachePackages) {
      loadPkgs(enabledPackages)
    } else {
      val h1 = scala.util.hashing.MurmurHash3.seqHash(whitelistedClasses)
      val h2 = scala.util.hashing.MurmurHash3.seqHash(disabledPackages)
      enabledPackages.flatMap {
        pkg =>
          val key = s"$pkg;$h1;$h2"
          PluginLoaderDefaultImpl.cache.getOrCompute(key, loadPkgs(Seq(pkg)))
      }
    }
  }

  protected[this] def applyOverrides(loadedPlugins: Seq[PluginBase], config: PluginConfig): Seq[PluginBase] = {
    val merged = loadedPlugins ++ config.merges
    if (config.overrides.nonEmpty) {
      Seq((merged.merge +: config.overrides).overrideLeft)
    } else merged
  }
}

object PluginLoaderDefaultImpl {
  def apply(): PluginLoaderDefaultImpl = new PluginLoaderDefaultImpl()

  private lazy val cache = new SyncCache[String, Seq[PluginBase]]()

  def doLoad[T](base: String, whitelistClasses: Seq[String], enabledPackages: Seq[String], disabledPackages: Seq[String], debug: Boolean): Seq[T] = {
    val scanResult = Value(new ClassGraph())
      .map(_.acceptPackages(enabledPackages: _*))
      .map(_.acceptClasses(whitelistClasses :+ base: _*))
      .map(_.rejectPackages(disabledPackages: _*))
      .map(_.enableExternalClasses())
      .map(if (debug) _.verbose() else identity)
      .map(_.scan())
      .get

    try {
      val implementors = scanResult.getClassesImplementing(base)
      implementors
        .asScala
        .filterNot(_.isAbstract)
        .flatMap {
          classInfo =>
            val clz = classInfo.loadClass()

            if (Option(clz.getSimpleName).exists(_.endsWith("$"))) {
              Seq(clz.getField("MODULE$").get(null).asInstanceOf[T])
            } else {
              clz.getDeclaredConstructors.find(_.getParameterCount == 0).map(_.newInstance().asInstanceOf[T]).toSeq
            }
        }
        .toSeq // 2.13 compat
    } finally {
      scanResult.close()
    }
  }
}
