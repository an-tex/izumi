package izumi.distage.model

import izumi.distage.model.definition.{Activation, ModuleBase}
import izumi.distage.model.plan._
import izumi.distage.model.planning.PlanSplittingOps

/** Transforms [[ModuleBase]] into [[OrderedPlan]] */
trait Planner extends PlanSplittingOps {
  def plan(input: PlannerInput): OrderedPlan

  final def plan(bindings: ModuleBase, activation: Activation, roots: Roots): OrderedPlan = {
    plan(PlannerInput(bindings, activation, roots))
  }

  // plan lifecycle
  def planNoRewrite(input: PlannerInput): OrderedPlan

  def rewrite(bindings: ModuleBase): ModuleBase
}
