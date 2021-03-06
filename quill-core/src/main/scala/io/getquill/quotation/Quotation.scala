package io.getquill.quotation

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.ast._
import io.getquill.util.Messages.RichContext

trait Quoted[+T] {
  def ast: Ast
}

case class QuotedAst(ast: Ast) extends StaticAnnotation

trait Quotation extends Parsing with Liftables with Unliftables {

  val c: Context
  import c.universe._

  def quote[T: WeakTypeTag](body: Expr[T]) = {
    val ast = astParser(body.tree)
    verifyFreeVariables(ast)
    q"""
      new ${c.weakTypeOf[Quoted[T]]} {
        @${c.weakTypeOf[QuotedAst]}($ast)
        def quoted = ast
        override def ast = $ast
        override def toString = ast.toString
      }
    """
  }

  protected def unquote[T](tree: Tree)(implicit ct: ClassTag[T]) =
    astTree(tree).flatMap(astUnliftable.unapply).map {
      case ast: T => ast
    }

  private def astTree(tree: Tree) =
    for {
      method <- tree.tpe.decls.find(_.name.decodedName.toString == "quoted")
      annotation <- method.annotations.headOption
      astTree <- annotation.tree.children.lastOption
    } yield (astTree)

  private def verifyFreeVariables(ast: Ast) =
    FreeVariables(ast).toList match {
      case Nil  =>
      case vars => c.fail(s"A quotation must not have references to variables outside its scope. Found: '${vars.mkString(", ")}' in '$ast'.")
    }
}
