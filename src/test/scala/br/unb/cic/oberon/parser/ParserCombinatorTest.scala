package br.unb.cic.oberon.parser

import scala.util.parsing.combinator._
import br.unb.cic.oberon.ast._
import java.nio.file.{Files, Paths}
import org.scalatest.funsuite.AnyFunSuite
import org.scalactic.TolerantNumerics


class ParserCombinatorTestSuite extends AnyFunSuite with Oberon2ScalaParser {
    test("Testing Int Parser") {
        assert(IntValue(123) == parseAbs(parse(int, "123"))) // positive number
        assert(IntValue(-321) == parseAbs(parse(int, "-321"))) // negative number
        val thrown = intercept[Exception] {
            parseAbs(parse(int, "abc 123")) // not accepted chars in the beginning
        }
        assert(thrown.getMessage.length>0)
        assert(IntValue(123) == parseAbs(parse(int, "123 abc"))) // accepted chars in the end
    }

    test("Testing Real Parser") {
        assert(RealValue(12.3) == parseAbs(parse(real, "12.3"))) // positive number
        assert(RealValue(-32.1) == parseAbs(parse(real, "-32.1"))) // negative number
    }

    test("Testing Bool Parser") {
        assert(BoolValue(true) == parseAbs(parse(bool, "True")))
        assert(BoolValue(false) == parseAbs(parse(bool, "False")))
    }

    test("Testing String Parser") {
        assert(StringValue("teste") == parseAbs(parse(string, "\"teste\""))) // double quotes
    }

    test("Testing identifier parser") {
        assert("teste" == parseAbs(parse(identifier, "teste")))
        assert("teste_" == parseAbs(parse(identifier, "teste_")))
        assert("teste1" == parseAbs(parse(identifier, "teste1")))
        ("teste_1abc" == parseAbs(parse(identifier, "teste_1abc")))
    }

    test("Testing type parser") {
        assert(IntegerType == parseAbs(parse(typeParser, "INTEGER"))) 
        assert(RealType == parseAbs(parse(typeParser, "REAL")))
        assert(CharacterType == parseAbs(parse(typeParser, "CHAR")))
        assert(BooleanType == parseAbs(parse(typeParser, "BOOLEAN")))
        assert(StringType == parseAbs(parse(typeParser, "STRING")))
        assert(NullType == parseAbs(parse(typeParser, "NIL")))
        assert(ReferenceToUserDefinedType("bolo") == parseAbs(parse(typeParser, "bolo")))
    }

    test("Testing expValueParser Expression") {
        assert(IntValue(16) == parseAbs(parse(expValueParser, "16")))
        assert(RealValue(-35.2) == parseAbs(parse(expValueParser, "-35.2")))
        assert(CharValue('a') == parseAbs(parse(expValueParser, "'a'")))
        assert(StringValue("teste") == parseAbs(parse(expValueParser, "\"teste\"")))
        assert(BoolValue(true) == parseAbs(parse(expValueParser, "True")))
        assert(BoolValue(false) == parseAbs(parse(expValueParser, "False")))
        assert(NullValue == parseAbs(parse(expValueParser, "NIL")))
    }

    test("Testing expressionParser") {
        assert(IntValue(16) == parseAbs(parse(expressionParser, "16")))
        assert(RealValue(-35.2) == parseAbs(parse(expressionParser, "-35.2")))
        assert(CharValue('a') == parseAbs(parse(expressionParser, "'a'")))
        assert(StringValue("teste") == parseAbs(parse(expressionParser, "\"teste\"")))
        assert(BoolValue(true) == parseAbs(parse(expressionParser, "True")))
        assert(BoolValue(false) == parseAbs(parse(expressionParser, "False")))
        assert(NullValue == parseAbs(parse(expressionParser, "NIL")))
        assert(Brackets(StringValue("testao")) == parseAbs(parse(expressionParser, "(\"testao\")")))

        var exp1 = IntValue(16)
        var exp2 = RealValue(-35.2)
        assert(MultExpression(exp1, exp2) == parseAbs(parse(expressionParser, "16 * -35.2")))
        assert(DivExpression(exp1, exp2) == parseAbs(parse(expressionParser, "16 / -35.2")))
        assert(AndExpression(exp1, exp2) == parseAbs(parse(expressionParser, "16 && -35.2")))

        assert(AndExpression(DivExpression(DivExpression(MultExpression(Brackets(DivExpression(IntValue(16),IntValue(4))),RealValue(-35.2)),IntValue(-4)),IntValue(3)),IntValue(4)) == parseAbs(parse(expressionParser, "(16 / 4) * -35.2 / -4 / 3 && 4")))
        assert(AndExpression(MultExpression(MultExpression(DivExpression(Brackets(IntValue(16)),IntValue(4)),Brackets(DivExpression(RealValue(-35.2),IntValue(-4)))),IntValue(3)),IntValue(-66)) == parseAbs(parse(expressionParser, "(16) / 4 * (-35.2 / -4) * 3 && -66")))
    }
    test("Testing addExpParser"){
        assert(OrExpression(SubExpression(AddExpression(IntValue(2),IntValue(4)),IntValue(3)),IntValue(2)) == parseAbs(parse(expressionParser, "2 + 4 - 3 || 2")))
    }
    test("Testing mulExpParser"){
        assert(AndExpression(DivExpression(MultExpression(IntValue(2),IntValue(4)),IntValue(3)),IntValue(2)) == parseAbs(parse(expressionParser, "2 * 4 / 3 && 2")))
    }
    test("Testing relExpParser"){
        assert(GTEExpression(GTExpression(LTEExpression(LTExpression(NEQExpression(EQExpression(IntValue(2),IntValue(4)),IntValue(3)),IntValue(2)),IntValue(4)),IntValue(1)),IntValue(7)) == parseAbs(parse(expressionParser, "2 = 4 # 3 < 2 <= 4 > 1 >= 7")))
    }

    test("Testing Aritmetic operations") {
        assert(EQExpression(AddExpression(IntValue(25), IntValue(12)), IntValue(5)) == parseAbs(parse(expressionParser, "25 + 12 = 5")))
        assert(EQExpression(AddExpression(IntValue(25), MultExpression(IntValue(12), IntValue(3))), IntValue(5)) == parseAbs(parse(expressionParser, "25 + 12 * 3 = 5")))
    }

    test("Testing FieldAcces") {
        assert(FieldAccessExpression(VarExpression("abc"), "ab") == parseAbs(parse(expressionParser, "abc.ab")))
    }
    test("Testing variable parser") {
        assert(VarExpression("abc") == parseAbs(parse(expressionParser, "abc")))
    }
    test("Testing function parser") {
        assert(FunctionCallExpression("abc", List(IntValue(12), StringValue("oi"))) == parseAbs(parse(expressionParser, "abc(12, \"oi\")")))
    }
    test("Testing pointer parser") {
        assert(PointerAccessExpression("abc") == parseAbs(parse(expressionParser, "abc^")))
    }
    test("Testing ArraySubscript parser") {
        assert( ArraySubscript(VarExpression("abc"), IntValue(3)) == parseAbs(parse(expressionParser, "abc[3]")))
    }

    test("Testing Statement parser") {
        // identifier 
        assert(AssignmentStmt("functionTest",IntValue(456)) == parseAbs(parse(multStatementParser, "functionTest := 456")))
        // designator
        assert(EAssignmentStmt(ArrayAssignment(FunctionCallExpression("functionTest",List()),IntValue(123)),IntValue(456)) == parseAbs(parse(multStatementParser, "functionTest()[123] := 456")))
        // readReal, readInt, readChar, write
        assert(ReadRealStmt("oi") == parseAbs(parse(multStatementParser, "readReal(oi)")))
        assert(ReadIntStmt("oi") == parseAbs(parse(multStatementParser, "readInt(oi)")))
        assert(ReadCharStmt("oi") == parseAbs(parse(multStatementParser, "readChar(oi)")))
        assert(WriteStmt(StringValue("oi")) == parseAbs(parse(multStatementParser, "write(\"oi\")")))
        // Procedure Call
        assert(ProcedureCallStmt("teste", List()) == parseAbs(parse(multStatementParser, "teste()")))
        assert(ProcedureCallStmt("teste", List(IntValue(1), IntValue(2))) == parseAbs(parse(multStatementParser, "teste(1, 2)")))
        assert(ProcedureCallStmt("teste", List(StringValue("oi"), VarExpression("banana"), IntValue(12))) == parseAbs(parse(multStatementParser, "teste(\"oi\", banana, 12)")))
        // IF THEN ELSE
        assert(
            IfElseStmt(
                Brackets(
                    GTExpression(
                        MultExpression(IntValue(2),IntValue(5)),
                        FunctionCallExpression("teste",List(IntValue(1))))
                    ),
                EAssignmentStmt(
                    ArrayAssignment(
                        FunctionCallExpression("functionTest",List()),
                        IntValue(123)
                    ),
                    IntValue(456)
                ),
                Some(
                    EAssignmentStmt(
                        ArrayAssignment(
                            FunctionCallExpression("testFunc",List()),
                            IntValue(321)
                        ),
                        IntValue(567)
                    )
                )
            ) == parseAbs(parse(multStatementParser, "IF (2 * 5 > teste(1)) THEN functionTest()[123] := 456 ELSE testFunc()[321] := 567 END"))
        )
        // IF THEN ELSIF THEN ELSE END
        assert(
            IfElseIfStmt(
                GTExpression(IntValue(2),IntValue(5)),
                AssignmentStmt("functionTest",IntValue(456)),
                List(
                    ElseIfStmt(
                        LTExpression(IntValue(3),IntValue(5)),
                        AssignmentStmt("functionTest",IntValue(567))
                    )
                ),
                Some(AssignmentStmt("functionTest",IntValue(678)))
            ) == parseAbs(parse(multStatementParser, "IF 2 > 5 THEN functionTest := 456 ELSIF 3 < 5 THEN functionTest := 567 ELSE functionTest := 678 END"))
        )
        // IF THEN ELSIF THEN ELSE END
        assert(
            IfElseIfStmt(
                GTExpression(IntValue(2),IntValue(5)),
                AssignmentStmt("functionTest",IntValue(456)),
                List(
                    ElseIfStmt(
                        LTExpression(IntValue(3),IntValue(5)),
                        AssignmentStmt("functionTest",IntValue(567))
                    )
                ),
                Some(AssignmentStmt("functionTest",IntValue(678)))
            ) == parseAbs(parse(multStatementParser, "IF 2 > 5 THEN functionTest := 456 ELSIF 3 < 5 THEN functionTest := 567 ELSE functionTest := 678 END"))
        )
        // IF THEN ELSIF THEN END
        assert(
            IfElseIfStmt(
                GTExpression(IntValue(2),IntValue(5)),
                AssignmentStmt("functionTest",IntValue(456)),
                List(
                    ElseIfStmt(
                        LTExpression(IntValue(3),IntValue(5)),
                        AssignmentStmt("functionTest",IntValue(567))
                    )
                ),
                None : Option[Statement]
            ) == parseAbs(parse(multStatementParser, "IF 2 > 5 THEN functionTest := 456 ELSIF 3 < 5 THEN functionTest := 567 END"))
        )
        // WHILE DO END
        assert(
            WhileStmt(
                GTExpression(IntValue(2),IntValue(5)),
                AssignmentStmt("functionTest",IntValue(456))
            ) == parseAbs(parse(multStatementParser, "WHILE 2 > 5 DO functionTest := 456 END"))
        )
        // REPEAT UNTIL
        assert(
            RepeatUntilStmt(
                GTExpression(IntValue(2),IntValue(5)),
                AssignmentStmt("functionTest",IntValue(456))
            ) == parseAbs(parse(multStatementParser, "REPEAT functionTest := 456 UNTIL 2 > 5"))
        )
        // FOR TO DO END
        assert(
            ForStmt(
                AssignmentStmt("functionTest",IntValue(456)),
                LTExpression(IntValue(3),IntValue(5)),
                AssignmentStmt("functionTest",IntValue(678))
            ) == parseAbs(parse(multStatementParser, "FOR functionTest := 456 TO 3 < 5 DO functionTest := 678 END"))
        )
        // LOOP END
        assert(
            LoopStmt(
                AssignmentStmt("functionTest",IntValue(456))
            ) == parseAbs(parse(multStatementParser, "LOOP functionTest := 456 END"))
        )
        // RETURN
        assert(
            ReturnStmt(
                LTExpression(IntValue(3),IntValue(5))
            ) == parseAbs(parse(multStatementParser, "RETURN 3 < 5"))
        )
        // CASE OF ELSE END
        assert(
            CaseStmt(
                VarExpression("X"),
                List(
                    SimpleCase(IntValue(0),AssignmentStmt("X",IntValue(2))), 
                    RangeCase(IntValue(123),IntValue(321),AssignmentStmt("X",IntValue(5)))
                ),
                Some(AssignmentStmt("functionTest",IntValue(678)))
            ) == parseAbs(parse(multStatementParser, "CASE X OF 0: X := 2 | 123..321: X := 5 ELSE functionTest := 678 END"))
        )
        // EXIT
        assert(ExitStmt() == parseAbs(parse(multStatementParser, "EXIT")))
    }

    test("Testing Statement sequence parser") {
        assert(SequenceStmt(List(ReadRealStmt("oi"), ReadRealStmt("oi"))) == parseAbs(parse(multStatementParser, "readReal(oi);readReal(oi)")))
    }

    test("Testing the oberon simple01 code") {
        val module = parseResource("simple/simple01.oberon")

        assert(module.name == "SimpleModule1")
        assert(module.constants.size == 1)
        assert(module.constants.head == Constant("x", IntValue(5)))
    }

    test("Testing the oberon simple02 code. This module has one constants and two variables") {
        val module = parseResource("simple/simple02.oberon")

        assert(module.name == "SimpleModule2")
        assert(module.constants.size == 1)
        assert(module.constants.head == Constant("x", IntValue(5)))
        assert(module.variables.size == 2)
        assert(module.variables.head == VariableDeclaration("abc", IntegerType))
        assert(module.variables(1) == VariableDeclaration("def", BooleanType))
    }

    test("Testing the oberon simple03 code. This module has three constants and two variables") {
        val module = parseResource("simple/simple03.oberon")

        assert(module.name == "SimpleModule3")
        assert(module.constants.size == 3)
        assert(module.constants.head == Constant("x", IntValue(5)))
        assert(module.constants(1) == Constant("y", IntValue(10)))
        assert(module.constants(2) == Constant("z", BoolValue(true)))

        assert(module.variables.size == 2)
        assert(module.variables.head == VariableDeclaration("abc", IntegerType))
        assert(module.variables(1) == VariableDeclaration("def", BooleanType))
    }

    test("Testing the oberon simple04 code. This module has three constants, a sum, and two variables") {
        val module = parseResource("simple/simple04.oberon")
        assert(module.name == "SimpleModule4")
        assert(module.constants.size == 3)
        assert(module.constants.head == Constant("x", IntValue(5)))
        assert(module.constants(1) == Constant("y", IntValue(10)))
        assert(module.constants(2) == Constant("z", AddExpression(IntValue(5), IntValue(10))))


        assert(module.variables.size == 2)
        assert(module.variables.head == VariableDeclaration("abc", IntegerType))
        assert(module.variables(1) == VariableDeclaration("def", BooleanType))
    }

    test("Testing the oberon simple05 code. This module has one constant, a multiplication, and two variables") {
        val module = parseResource("simple/simple05.oberon")

        assert(module.name == "SimpleModule5")
        assert(module.constants.size == 1)
        assert(module.constants.head == Constant("z", MultExpression(IntValue(5), IntValue(10))))


        assert(module.variables.size == 2)
        assert(module.variables.head == VariableDeclaration("abc", IntegerType))
        assert(module.variables(1) == VariableDeclaration("def", BooleanType))
    }


    test("Testing the oberon simple06 code. This module has one constants, complex expression, and two variables") {
        val module = parseResource("simple/simple06.oberon")

        assert(module.name == "SimpleModule6")
        assert(module.constants.size == 1)
        assert(module.constants.head == Constant("z", AddExpression(IntValue(5), MultExpression(IntValue(10), IntValue(3)))))


        assert(module.variables.size == 2)
        assert(module.variables.head == VariableDeclaration("abc", IntegerType))
        assert(module.variables(1) == VariableDeclaration("def", BooleanType))
    }

    test("Testing the oberon simple07 code. This module has two constants, a complex expression, and two variables") {
        val module = parseResource("simple/simple07.oberon")

        assert(module.name == "SimpleModule7")
        assert(module.constants.size == 2)
        assert(module.constants.head == Constant("x", AddExpression(IntValue(5), MultExpression(IntValue(10), IntValue(3)))))
        assert(module.constants(1) == Constant("y",
            AddExpression(IntValue(5),
            DivExpression(
            MultExpression(IntValue(10), IntValue(3)),
            IntValue(5)))))

        assert(module.variables.size == 2)
        assert(module.variables.head == VariableDeclaration("abc", IntegerType))
        assert(module.variables(1) == VariableDeclaration("def", BooleanType))
    }

    test("Testing the oberon simple08 code. This module has three constants, a boolean expresson, and two variables") {
        val module = parseResource("simple/simple08.oberon")

        assert(module.name == "SimpleModule8")
        assert(module.constants.size == 3)
        assert(module.constants.head == Constant("x", BoolValue(false)))
        assert(module.constants(1) == Constant("y", BoolValue(true)))
        assert(module.constants(2) == Constant("z", AndExpression(BoolValue(true), BoolValue(false))))
    }

    test("Testing the oberon simple09 code. This module has one constant and an expression involving both 'and' and 'or'") {
        val module = parseResource("simple/simple09.oberon")

        assert(module.name == "SimpleModule9")
        assert(module.constants.size == 1)
        assert(module.constants.head == Constant("z", OrExpression(AndExpression(BoolValue(true), BoolValue(false)), BoolValue(false))))
    }
    
}