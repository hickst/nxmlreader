package ai.lum.nxmlreader.standoff

import ai.lum.common.Interval


sealed trait Tree {

  private[standoff] var _parent: Option[Tree] = None
  def parent: Option[Tree] = _parent

  def label: String
  def text: String
  def characterInterval: Interval
  def children: List[Tree]
  def attributes: Map[String, String]
  def copy(): Tree
  def getTerminals(i: Interval): List[Terminal]

  def getTerminals(i: Int): List[Terminal] = {
    getTerminals(Interval.singleton(i))
  }

  def getTerminals(start: Int, end: Int): List[Terminal] = {
    getTerminals(Interval.open(start, end))
  }

  def getTerminals(): List[Terminal] = {
    getTerminals(characterInterval)
  }

  def root: Tree = parent match {
    case None => this
    case Some(p) => p.root
  }

  def ancestors: List[Tree] = parent match {
    case None => Nil
    case Some(p) => p :: p.ancestors
  }

  def path: String = ancestors.map(_.label).mkString(" ")

}

class NonTerminal(
    val label: String,
    val children: List[Tree],
    val attributes: Map[String, String]
) extends Tree {

  require(children.nonEmpty, "children list is empty")
  require(children.forall(_.parent.isEmpty), "children already have a parent")

  // set children's parent to self
  children.foreach(_._parent = Some(this))

  val characterInterval: Interval = Interval.union(children.map(_.characterInterval))

  def text: String = children.map(_.text).mkString

  def copy(): Tree = new NonTerminal(label, children.map(_.copy()), attributes)

  def getTerminals(i: Interval): List[Terminal] = {
    if (i intersects characterInterval) {
      for {
        c <- children
        t <- c.getTerminals(i)
      } yield t
    } else {
      Nil
    }
  }

}

class Terminal(
    val label: String,
    val text: String,
    val characterInterval: Interval,
    val attributes: Map[String, String] = Map.empty
) extends Tree {

  val children: List[Tree] = Nil

  def copy(): Tree = new Terminal(label, text, characterInterval)

  def getTerminals(i: Interval): List[Terminal] = {
    if (i intersects characterInterval) List(this) else Nil
  }

}
