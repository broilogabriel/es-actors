package com.broilogabriel

import org.scalatest.FlatSpec

/**
  * Created by init-scala.sh (https://gist.github.com/broilogabriel/6d24932b8e3f77fed91698f8b7b1286b) on 16-11-2016.
  */
class MainTest extends FlatSpec {

  "An empty String" should "return '-'" in {
    assert(Main.minuses("") == "-")
  }

  "A null input" should "return '-'" in {
    assert(Main.minuses(null) == "-")
  }

  "A String input" should "return a String with the same size" in {
    val input = "somebiginput"
    assert(Main.minuses(input).length == input.length)
  }

}
