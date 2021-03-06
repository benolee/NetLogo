// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.api

import org.scalatest.FunSuite

class ColorTests extends FunSuite {
  test("modulate") {
    expectResult(0)(Color.modulateDouble(0))
    expectResult(0)(Color.modulateDouble(140))
    expectResult(139)(Color.modulateDouble(-1))
    expectResult(139.9)(Color.modulateDouble(-0.1))
    expectResult(20)(Color.modulateDouble(1000))
    expectResult(120)(Color.modulateDouble(-1000))
    expectResult(139.9999999999999)(Color.modulateDouble(-0.000000000000001))
  }
  test("rgba") {
    val list = LogoList(0: java.lang.Double,
                        0: java.lang.Double,
                        0: java.lang.Double)
    expectResult((255 << 24))(Color.getARGBIntByRGBAList(list))
  }
  test("translate") {
    val color7 = Color.getARGBbyPremodulatedColorNumber(7 * 10 + 5)
    // testing this line from color-translation.txt: 7 -16711738 -14967177
    expectResult(color7)(Color.translateSavedColor(-14967177))
    expectResult(color7)(Color.translateSavedColor(-16711738))
  }
}
