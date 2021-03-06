package dk.thrane.compiler.ast

import java.nio.file.Files
import java.nio.file.Paths

import dk.thrane.compiler.ast.Tokens.*
import dk.thrane.compiler.type.SymbolGatherer
import dk.thrane.compiler.type.TypeChecker
import dk.thrane.compiler.weeder.ReturnCheck

class FunctionCheck : Visitor() {
    override fun enterNode(node: Node) {
        when (node) {
            is FunctionNode -> {
                if (node.head.name != node.tail.name) {
                    throw IllegalStateException("Name in head of function does not match name in tail of function!" +
                            " Head defined at line ${node.head.lineNumber}, tail at line ${node.tail.lineNumber}")
                }
                visitChildren = false
            }
        }
    }

    override fun exitNode(node: Node) {
    }
}

fun main(args: Array<String>) {
    println("Hello!")

    val source = Files.readAllLines(Paths.get("./programs", "operators.die")).joinToString("\n")
    val parser = Parser()
    val functionChecker = FunctionCheck()
    val gatherer = SymbolGatherer()
    val checker = TypeChecker()
    val returnCheck = ReturnCheck()

    val ast = parser.parse(source)
    gatherer.traverse(ast)
    // FIXME NEEDS TO BE SUPPORTED a[0][1][2];
    functionChecker.traverse(ast)
    returnCheck.traverse(ast)
    checker.traverse(ast)
}

