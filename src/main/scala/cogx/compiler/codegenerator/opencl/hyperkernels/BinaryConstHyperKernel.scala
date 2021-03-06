/*
 * (c) Copyright 2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cogx.compiler.codegenerator.opencl.hyperkernels

import cogx.compiler.codegenerator.opencl.fragments.{AddressingMode, HyperKernel, SmallTensorAddressing, TensorElementAddressing}
import cogx.compiler.parser.op.{BinaryConstOpcode, NeedsVectorLength}
import cogx.compiler.codegenerator.opencl.OpcodeToFunction
import cogx.platform.types.{FieldType, VirtualFieldRegister}

/**
  * A hyperkernel that combines a field with a real constant.
  *
  * @author Greg Snider
  *
  * @param in The input virtual field register driving this kernel.
  * @param operation The binary opcode for this operation.
  * @param resultType The FieldType of the result of this kernel.
  * @param addressMode The addressing mode of this kernel.
  */
private[cogx]
class BinaryConstHyperKernel private (in: Array[VirtualFieldRegister],
                                               operation: BinaryConstOpcode,
                                               resultType: FieldType,
                                               addressMode: AddressingMode)
        extends HyperKernel(operation, in, resultType, addressMode)
{
  opcode match {
    case x: NeedsVectorLength =>
      val vectorElemsPerThread =
        addressMode match {
          case SmallTensorAddressing => resultType.tensorShape.points
          case TensorElementAddressing => 1
          case _ => throw new RuntimeException(s"Internal error: unexpected addressing mode $addressMode")
        }
      addCode(s"    @out0 = ${OpcodeToFunction(operation)}(read(@in0), ${operation.const}f, $vectorElemsPerThread);")
    case _ =>
      addCode(s"    @out0 = ${OpcodeToFunction(operation)}(read(@in0), ${operation.const}f);")
  }
  //debugCompile
}

/** Factory object for creating kernels of this type.
  */
private[cogx]
object BinaryConstHyperKernel extends HyperHelper {

  /**
   * Create a hyperkernel that combines a field with a constant.
   *
   * @param in The input virtual field register driving this kernel.
   * @param operation The binary opcode for this operation.
   * @param resultType The FieldType of the result of this kernel.
   * @return The synthesized hyperkernel.
   */
  def apply(in: Array[VirtualFieldRegister], operation: BinaryConstOpcode,
            resultType: FieldType): HyperKernel = {
    require(in.length == 1)
    require(resultType == in(0).fieldType)
    val addressing = bestAddressMode(in, resultType)
    new BinaryConstHyperKernel(in, operation, resultType, addressing)
  }
}

