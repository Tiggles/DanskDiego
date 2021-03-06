package dk.thrane.compiler.bytecode

import java.io.DataOutputStream
import java.util.*

class ConstantPool {
    private val entries: MutableList<ConstantPoolEntry> = ArrayList()
    /**
     * The value of the constant_pool_count item is equal to the <b>number of entries
     * in the constant_pool table plus one</b>. A constant_pool index is considered
     * valid if it is greater than zero and less than constant_pool_count, with the
     * exception for constants of type long and double noted in JVMSE8 §4.4.5.
     */
    private var nextIndex: Int = 1
    private val constants: MutableMap<Pair<String, Class<*>>, ConstantPoolEntry> = HashMap()

    fun write(out: DataOutputStream) {
        out.writeShort(nextIndex)
        entries.forEach { it.write(out) }
    }

    fun insertEntry(entry: ConstantPoolEntry) {
        entry.index = nextIndex
        entry.pool = this
        entries.add(entry)
        nextIndex += entry.entries
    }

    private fun insertIntoCache(name: String, value: ConstantPoolEntry): ConstantPoolEntry {
        constants[Pair(name, value.javaClass)] = value
        insertEntry(value)
        return value
    }

    fun utf8(string: String): ConstantUtf8Info {
        return (constants[Pair(string, ConstantUtf8Info::class.java)] ?:
                insertIntoCache(string, ConstantUtf8Info(string))) as ConstantUtf8Info
    }

    fun string(string: String): ConstantStringInfo {
        return (constants[Pair(string, ConstantStringInfo::class.java)] ?:
                insertIntoCache(string, ConstantStringInfo(utf8(string)))) as ConstantStringInfo
    }

    fun classRef(string: String): ConstantClassInfo {
        return (constants[Pair(string, ConstantClassInfo::class.java)] ?:
                insertIntoCache(string, ConstantClassInfo(utf8(string)))) as ConstantClassInfo
    }

    fun classRef(vararg strings: String) = classRef(strings.joinToString("/"))

    fun nameAndType(name: String, type: Descriptor): ConstantNameAndInfoType {
        return (constants[Pair("$name$type", ConstantNameAndInfoType::class.java)] ?:
                insertIntoCache("$name$type", ConstantNameAndInfoType(
                        utf8(name),
                        utf8(type.toString()))
                )) as ConstantNameAndInfoType
    }

    fun descriptor(type: Descriptor): ConstantUtf8Info {
        return utf8(type.toString())
    }

    fun fieldRef(classInfo: ConstantClassInfo, nameAndInfoType: ConstantNameAndInfoType): ConstantFieldRefInfo {
        return (constants[Pair("$classInfo$nameAndInfoType", ConstantFieldRefInfo::class.java)] ?:
                insertIntoCache("$classInfo$nameAndInfoType",
                        ConstantFieldRefInfo(classInfo, nameAndInfoType))) as ConstantFieldRefInfo
    }

    fun methodRef(classInfo: ConstantClassInfo, nameAndInfoType: ConstantNameAndInfoType): ConstantMethodRefInfo{
        return (constants[Pair("$classInfo$nameAndInfoType", ConstantMethodRefInfo::class.java)] ?:
                insertIntoCache("$classInfo$nameAndInfoType",
                        ConstantMethodRefInfo(classInfo, nameAndInfoType))) as ConstantMethodRefInfo
    }
}

/**
 * Represents a single entry in the constant pool
 */
abstract class ConstantPoolEntry(val tag: Int, val entries: Int = 1) {
    var index: Int = -1
    var pool: ConstantPool? = null

    fun write(out: DataOutputStream) {
        out.writeByte(tag)
        writeBytes(out)
    }

    open fun writeBytes(out: DataOutputStream) {
    }
}

/**
 * Represents a class or an interface. The name index must be a valid index into the constant pool table, pointing to a
 * [ConstantUtf8Info] structure, which should represent a valid binary class or interface encoded in the internal form
 * (§4.2.1)
 */
class ConstantClassInfo(val name: ConstantUtf8Info) : ConstantPoolEntry(7) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeShort(name.index)
    }
}

/**
 * Represents a reference to a field, method, or a reference.
 *
 * The value of the [classEntry] item must be a valid index into the constant_pool table. The  constant_pool entry at
 * that index must be a [ConstantClassInfo] structure (§4.4.1) representing a class or interface type that has the
 * field or method as a member.
 *
 * The value of the [nameAndType] item must be a valid index into the constant pool table. The [ConstantPoolEntry]
 * entry at that index must be a [ConstantNameAndInfoType] structure (§4.4.6). This  constant_pool entry indicates the
 * name and descriptor of the field or method.
 *
 * In a [ConstantFieldRefInfo], the indicated descriptor must be a field descriptor (§4.3.2). Otherwise, the
 * indicated descriptor must be a method descriptor (§4.3.3).
 *
 * If the name of the method of a [ConstantMethodRefInfo] structure begins with a ' < ' (' \u003c '), then the name
 * must be the special name  <init> , representing an instance initialization method (§2.9). The return type of such a
 * method must be void.
 */
abstract class ConstantRefInfo(tag: Int, val classEntry: ConstantClassInfo,
                               val nameAndType: ConstantNameAndInfoType) : ConstantPoolEntry(tag) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeShort(classEntry.index)
        out.writeShort(nameAndType.index)
    }
}

class ConstantFieldRefInfo(classIndex: ConstantClassInfo, nameAndTypeIndex: ConstantNameAndInfoType) :
        ConstantRefInfo(9, classIndex, nameAndTypeIndex)

class ConstantMethodRefInfo(classIndex: ConstantClassInfo, nameAndTypeIndex: ConstantNameAndInfoType) :
        ConstantRefInfo(10, classIndex, nameAndTypeIndex)

class ConstantInterfaceMethodRefInfo(classIndex: ConstantClassInfo, nameAndTypeIndex: ConstantNameAndInfoType) :
        ConstantRefInfo(11, classIndex, nameAndTypeIndex)

/**
 * Used to represent constant objects of the type String
 *
 * The value of the [string] item must be a valid index into the constant pool table. The [ConstantPoolEntry] at
 * that index must be a [ConstantUtf8Info] structure (§4.4.7) representing the sequence of Unicode code points to
 * which the String object is to be initialized.
 */
class ConstantStringInfo(val string: ConstantUtf8Info) : ConstantPoolEntry(8) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeShort(string.index)
    }
}

abstract class Constant4ByteNumericInfo(tag: Int) : ConstantPoolEntry(tag)

/**
 * The bytes item of the [ConstantIntegerInfo] structure represents the value of the  int constant. The bytes of the
 * value are stored in big-endian (high byte first) order.
 */
class ConstantIntegerInfo(val value: Int) : Constant4ByteNumericInfo(3) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeInt(value)
    }
}

/**
 * The bytes item of the [ConstantFloatInfo] structure represents the value of the float constant in IEEE 754
 * floating-point single format (§2.3.2). The bytes of the single format representation are stored in big-endian
 * (high byte first) order.
 */
class ConstantFloatInfo(val value: Float) : Constant4ByteNumericInfo(4) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeFloat(value)
    }
}

/**
 * #IMPORTANT:
 * All 8-byte constants take up two entries in the constant_pool table of the class file. If a
 * [Constant8ByteNumericInfo] is the item in the constant pool table at index n, then the next usable item in
 * the pool is located at index n + 2. The  constant pool index n + 1 must be valid but is considered unusable.
 */
abstract class Constant8ByteNumericInfo(tag: Int) : ConstantPoolEntry(tag, 2)

class ConstantLongInfo(val value: Long) : Constant8ByteNumericInfo(5) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeLong(value)
    }
}

class ConstantDoubleInfo(val value: Double) : Constant8ByteNumericInfo(6) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeDouble(value)
    }
}

class ConstantNameAndInfoType(val name: ConstantUtf8Info, val descriptor: ConstantUtf8Info) :
        ConstantPoolEntry(12) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeShort(name.index)
        out.writeShort(descriptor.index)
    }
}

class ConstantUtf8Info(val string: String) : ConstantPoolEntry(1) {
    override fun writeBytes(out: DataOutputStream) {
        val bytes = string.toByteArray()
        out.writeShort(bytes.size)
        bytes.forEach { out.writeByte(it.toInt()) }
    }
}

class ConstantMethodHandleInfo(val referenceKind: Byte, val reference: ConstantPoolEntry) : ConstantPoolEntry(15) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeByte(referenceKind.toInt())
        out.writeShort(reference.index)
    }
}

class ConstantMethodTypeInfo(val descriptor: ConstantUtf8Info) : ConstantPoolEntry(16) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeShort(descriptor.index)
    }
}

class ConstantInvokeDynamicInfo(val bootstrapMethodAttributeIndex: Short, val nameAndType: ConstantNameAndInfoType) :
        ConstantPoolEntry(18) {
    override fun writeBytes(out: DataOutputStream) {
        out.writeShort(bootstrapMethodAttributeIndex.toInt())
        out.writeShort(nameAndType.index)
    }
}
