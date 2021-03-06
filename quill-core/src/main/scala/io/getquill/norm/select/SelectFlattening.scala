package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context

import io.getquill.ast._
import io.getquill.util.Messages.RichContext

trait SelectFlattening extends SelectValues {
  val c: Context

  import c.universe._

  protected def flattenSelect[T](q: Query, inferDecoder: Type => Option[Tree])(implicit t: WeakTypeTag[T]) = {
    val (query, mapAst) = ExtractSelect(q)
    val selectValues = flatten(mapAst, t.tpe, inferDecoder)
    (ReplaceSelect(query, selectAsts(selectValues)), selectValues)
  }

  private def flatten(ast: Ast, typ: Type, inferDecoder: Type => Option[Tree]): SelectValue =
    (inferDecoder(typ), inferDecoder(optionType(c.WeakTypeTag(typ))), ast) match {
      case (_, _, ast) if (typ <:< c.weakTypeOf[Option[Any]]) =>
        OptionSelectValue(flatten(ast, typ.typeArgs.head, inferDecoder))
      case (Some(decoder), Some(optionDecoder), ast) =>
        SimpleSelectValue(ast, decoder, optionDecoder)
      case (None, _, ast) if (typ.typeSymbol.asClass.isCaseClass) =>
        caseClassSelectValue(typ, ast, inferDecoder)
      case other =>
        c.fail(s"Source doesn't know how to decode '$ast: $typ'")
    }

  private def optionType[T](implicit t: WeakTypeTag[T]) =
    c.weakTypeOf[Option[T]]

  private def selectAsts(value: SelectValue): List[Ast] =
    value match {
      case SimpleSelectValue(ast, _, _)    => List(ast)
      case CaseClassSelectValue(_, params) => params.flatten.map(selectAsts).flatten
      case OptionSelectValue(value)        => selectAsts(value)
    }

  private def caseClassSelectValue(typ: Type, ast: Ast, inferDecoder: Type => Option[Tree]) =
    CaseClassSelectValue(typ, selectValuesForCaseClass(typ, ast, inferDecoder))

  private def selectValuesForCaseClass(typ: Type, ast: Ast, inferDecoder: Type => Option[Tree]) =
    selectValuesForConstructor(typ, caseClassConstructor(typ), ast, inferDecoder)

  private def selectValuesForConstructor(typ: Type, constructor: MethodSymbol, ast: Ast, inferDecoder: Type => Option[Tree]) =
    constructor.paramLists.map(_.map {
      param =>
        val paramType = param.typeSignature.asSeenFrom(typ, typ.typeSymbol)
        flatten(Property(ast, param.name.decodedName.toString), paramType, inferDecoder)
    })

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if (m.isPrimaryConstructor) => m
    }.headOption.get // a case class always has a primary constructor
}
