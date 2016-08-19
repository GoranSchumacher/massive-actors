package util

/**
 * @author Gøran Schumacher (GS) / Schumacher Consulting Aps
 * @version $Revision$ 22/06/2016
 */
object Timer {
  def time[R](task : String)( block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    val duration = t1-t0
    println(f"Elapsed time for task: $task - $duration%,15d ms")
    result
  }
}
