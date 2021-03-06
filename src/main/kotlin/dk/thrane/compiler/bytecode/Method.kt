package dk.thrane.compiler.bytecode

import java.io.DataOutputStream

class Method(val accessFlags: List<MethodAccessFlag>, val name: ConstantUtf8Info, val descriptor: ConstantUtf8Info,
             val attributes: List<Attribute>) {
    fun write(out: DataOutputStream) {
        out.writeShort(MethodAccessFlag.combine(accessFlags))
        out.writeShort(name.index)
        out.writeShort(descriptor.index)
        out.writeShort(attributes.size)
        attributes.forEach { it.write(out) }
    }
}
