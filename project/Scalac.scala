object Scalac {
  lazy val options: Seq[String] = Seq(warnAsError, warnUnused, lint)

  lazy val warnAsError = "-Werror"
  lazy val warnUnused  = "-Wunused:imports,privates,locals"
  lazy val lint: String = {
    val warnings = Seq(
      "adapted-args",
      "nullary-unit",
      "inaccessible",
      "infer-any",
      "missing-interpolator",
      "doc-detached",
      "private-shadow",
      "type-parameter-shadow",
      "poly-implicit-overload",
      "option-implicit",
      "delayedinit-select",
      "package-object-classes",
      "stars-align",
      "constant",
      "nonlocal-return",
      "implicit-not-found",
      "serial",
      "valpattern",
      "eta-zero",
      "eta-sam",
      "deprecation",
      "recurse-with-default",
      "unit-special",
      "multiarg-infix",
      "implicit-recursion"
    )
    s"-Xlint:${warnings.mkString(",")}"
  }
}
