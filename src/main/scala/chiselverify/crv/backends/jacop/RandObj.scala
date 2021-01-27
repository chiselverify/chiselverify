package chiselverify.crv.backends.jacop
import chiselverify.crv.CRVException
import org.jacop.core.IntDomain
import org.jacop.search._

import scala.collection.mutable
import scala.util.Random

object RandObj {


  private val addLabelFun = new ThreadLocal[mutable.Buffer[DepthFirstSearch[_ <: org.jacop.core.Var]]]

  private def dfs[A <: Rand]: DepthFirstSearch[A] = {
    val label = new DepthFirstSearch[A]
    label.setAssignSolution(true)
    label.setSolutionListener(new PrintOutListener[A]())
    label
  }

  private def satisfySearch[A <: Rand](
    select:   SelectChoicePoint[A],
    listener: SolutionListener[A],
    model:    Model
  ): Boolean = {
    model.imposeAllConstraints()
    val label = dfs[A]

    label.setAssignSolution(true)
    label.setPrintInfo(false)
    addLabel(label)
    label.setSolutionListener(listener)
    listener.searchAll(false)
    label.labeling(model, select)
  }

  private def addLabel(label: DepthFirstSearch[_ <: Rand]): Unit = {
    val b = addLabelFun.get()
    if (b != null) b += label
  }
}

trait RandObj extends chiselverify.crv.RandObj {

  implicit var currentModel: Model = new Model()
  private var nOfCalls = 0
  private val listener = new SimpleSolutionListener[Rand]
  private val domainDatabase = mutable.Map[Rand, IntDomain]()
  private var problemVariables = List[Rand]()
  private var initialize = false

  /** Restore the domain of all [[Rand]] variable declared in the current [[RandObj]] to their initial values
    */
  private def resetDomains(): Unit = {
    domainDatabase.foreach { k =>
      k._1.domain.setDomain(k._2)
      k._1.domain.modelConstraintsToEvaluate = Array.fill[Int](k._1.domain.modelConstraintsToEvaluate.length)(0)
    }
  }

  override def toString: String = {
    val buffer = new StringBuilder()
    for (i <- Range(0, currentModel.n)) {
      buffer ++= currentModel.vars(i).toString + ", "
    }
    buffer + currentModel.randcVars.mkString(", ")
  }

  /** Print all the random variables declared inside the current [[RandObj]]
    */
  def debug(): Unit = {
    problemVariables.foreach(println)
  }

  /** This method is called only the first time we randomize the current [[RandObj]]
    * This is necessary because every time we assign a solution to each of the random variables, their domains are
    * shrink
    */
  private def initializeObject(): Unit = {
    initialize = true
    problemVariables = currentModel.vars.filter(x => x.isInstanceOf[Rand]).map(_.asInstanceOf[Rand]).toList
    problemVariables.foreach(x => domainDatabase += (x -> x.domain.cloneLight()))
  }

  def randomizeWith(constraints: Constraint*): Boolean = {
   val ret = randomize
    constraints.foreach(_.disable())
    ret
  }

  /**
    * Selectivily enable a constraint inside each
    * distribution constraint
    */
  def setDistConstraints(): Unit = {
    currentModel.distConst.foreach(_.disableAll())
    currentModel.distConst.filter(_.isEanble).foreach(_.randomlyEnable())
  }

  /** Randomize the current [[RandObj]]
    *
    * @return Boolean the result of the current randomization
    */
  override def randomize: Boolean = {
    nOfCalls += 1
    if (!initialize) initializeObject()

    if (problemVariables.isEmpty) throw CRVException("Class doesn't have any random field")

    resetDomains()
    preRandomize()
    setDistConstraints()
    // TODO: create a better implementation of Randc in order to add constraint to them
    currentModel.randcVars.foreach(_.next())
    val result = RandObj.satisfySearch(
      new SimpleSelect[Rand](
        problemVariables.toArray,
        null,
        new IndomainRandom[Rand](new Random(currentModel.seed + nOfCalls).nextInt())
      ),
      listener,
      currentModel
    )
    postRandomize()
    result
  }
}
