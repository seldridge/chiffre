// Copyright 2017 IBM
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package chiffre

import chisel3._
import chisel3.util._
import chisel3.core.BaseModule
import chisel3.internal.InstanceId
import chisel3.experimental.{ChiselAnnotation, RunFirrtlTransform}
import chiffre.passes.{ScanChainAnnotation, FaultInjectionAnnotation,
  ScanChainTransform, FaultInstrumentationTransform}
import chiffre.scan._

trait ChiffreController extends BaseModule {
  self: BaseModule =>

  /** Scan Chain Identifier used to differentiate scan chains. This must
    * be a `lazy val`. */
  def scanId: String

  private def scanMaster(scan: Data, name: String): Unit = {
    if (scanId == null) { // scalastyle:off
      throw new Exception(
        "Chiffre Controller 'scanId' should be a 'lazy val'") }
    annotate(
      new ChiselAnnotation with RunFirrtlTransform {
        def toFirrtl = ScanChainAnnotation(
          scan.toNamed,
          "master", "scan", name, None)
        def transformClass = classOf[ScanChainTransform]
      }
    )
  }

  val scan = Wire(new ScanIo)
  scan.in := false.B

  scanMaster(scan, scanId)
}

trait ChiffreInjectee extends BaseModule {
  self: BaseModule =>

  def isFaulty[T <: inject.Injector](component: InstanceId, id: String,
                                     tpe: Class[T]): Unit = {
    component match {
      case c: Bits =>
        annotate(
          new ChiselAnnotation with RunFirrtlTransform {
            def toFirrtl = FaultInjectionAnnotation(c.toNamed, id, tpe.getName)
            def transformClass = classOf[FaultInstrumentationTransform]
          })
      case c => throw new Exception(s"Type not implemented for: $c")
    }
  }
}
