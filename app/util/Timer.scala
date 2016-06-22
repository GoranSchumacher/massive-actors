package util

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 22/06/2016
 */
object Timer {
  def time[R](task : String)( block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println(s"Elapsed time for task $task: " + (t1 - t0) + "ns")
    result
  }
}
