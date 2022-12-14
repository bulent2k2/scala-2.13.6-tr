package scala.collection.convert

import java.util

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnit4])
class MapWrapperTest {

  /* Test for scala/bug#7883 */
  @deprecated("Uses deprecated extension", since="2.13")
  @Test
  def testContains(): Unit = {
    import scala.language.reflectiveCalls  // for accessing containsCounter

    // A HashMap which throws an exception when the iterator method is called.
    // Before the fix for scala/bug#7883, calling MapWrapper.containsKey() used to
    // iterate through every element of the wrapped Map, and thus would crash
    // in this case.
    val scalaMap = new scala.collection.mutable.HashMap[String, String] {
      var containsCounter = 0  // keep track of how often contains() has been called.
      override def iterator = throw new UnsupportedOperationException

      override def contains(key: String): Boolean = {
        containsCounter += 1
        super.contains(key)
      }
    }

    val javaMap = scalaMap.asJava

    scalaMap("hello") = "world"
    scalaMap(null) = "null's value"

    assertEquals(0, scalaMap.containsCounter)
    assertTrue(javaMap.containsKey("hello"))     // positive test
    assertTrue(javaMap.containsKey(null))        // positive test, null key

    assertFalse(javaMap.containsKey("goodbye"))  // negative test
    // Note: this case does NOT make it to scalaMap's contains() method because the runtime
    // cast fails in MapWrapper, so the containsCounter is not incremented in this case.
    assertFalse(javaMap.containsKey(42))         // negative test, wrong key type

    assertEquals(Some("null's value"), scalaMap.remove(null))
    assertFalse(javaMap.containsKey(null))       // negative test, null key
    assertEquals(4, scalaMap.containsCounter)
  }

  // test for scala/bug#8504
  @Test
  def testHashCodeNulls(): Unit = {
    val javaMap = Map(1 -> null).asJava

    // Before the fix for scala/bug#8504, this throws a NPE
    javaMap.hashCode
  }

  // regression test for https://github.com/scala/bug/issues/10663
  @Test
  def testHashCodeEqualsMatchesJavaMap(): Unit = {
    val jmap = new util.HashMap[String, String]()
    jmap.put("scala", "rocks")
    jmap.put("java interop is fun!", "ya!")
    jmap.put("?????????? ????\\'?? ???????????????", "whyyyy")
    jmap.put("nulls nooo", null)
    jmap.put(null, "null keys are you serious??")

    // manually convert to scala map
    val scalaMap = jmap.entrySet().iterator.asScala.map { e => e.getKey -> e.getValue}.toMap

    val mapWrapper = scalaMap.asJava

    assertEquals(jmap.hashCode(), mapWrapper.hashCode())
    assertTrue(jmap == mapWrapper)
    assertTrue(mapWrapper == jmap)
  }
}
