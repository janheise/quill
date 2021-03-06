package io.getquill.norm

import io.getquill.ast._

object ApplyIntermediateMap {

  def unapply(q: Query): Option[Query] =
    q match {

      case Map(Map(a: GroupBy, b, c), d, e)     => None
      case FlatMap(Map(a: GroupBy, b, c), d, e) => None
      case Filter(Map(a: GroupBy, b, c), d, e)  => None
      case SortBy(Map(a: GroupBy, b, c), d, e)  => None

      // a.map(b => c).map(d => e) =>
      //    a.map(b => e[d := c])
      case Map(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(a, b, er))

      // a.map(b => c).flatMap(d => e) =>
      //    a.flatMap(b => e[d := c])
      case FlatMap(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(FlatMap(a, b, er))

      // a.map(b => c).filter(d => e) =>
      //    a.filter(b => e[d := c]).map(b => c)
      case Filter(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(Filter(a, b, er), b, c))

      // a.map(b => c).sortBy(d => e) =>
      //    a.sortBy(b => e[d := c]).map(b => c)
      case SortBy(Map(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(SortBy(a, b, er), b, c))

      case other => None
    }
}
