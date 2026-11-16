package org.apertium.grammar;

// Adapted from https://csvparser.github.io/mathparser.html which in turn is adapted from book "C++ The Complete Reference" by H.Schildt.

import static java.lang.Math.*;
import static org.apertium.grammar.Inlines.ISDELIM;
import static org.apertium.grammar.Inlines.ISSPACE;
import static org.apertium.grammar.MathParser.TokenType.*;

public class MathParser {

    enum TokenType {
        DELIMITER, VARIABLE, NUMBER, FUNCTION
    }

    int NUMVARS = 26;
    String expression;
    int expr_p = 0;
    String token;
    TokenType tok_type;
    double[] vars = new double[NUMVARS];
    double min = 0;
    double max = 0;

    public MathParser(){
        new MathParser(0,0);
    }
    public MathParser(double numericMin, double numericMax) {
        this.min = numericMin;
        this.max = numericMax;
    }

    public double eval(String exp) {
        double result = 0;
        get_token();
        if (token.isEmpty()) {
            throw new RuntimeException("Expression empty");
        }
        eval_assign(result);
        if (!token.isEmpty()) {
            throw new RuntimeException("Syntax error");
        }
        return result;
    }

    private void eval_assign(double result) {
        String temp_token;
        if (tok_type == VARIABLE) {
            String t_ptr = expression;
            temp_token = token;
            int slot = token.charAt(0) - 'A';
            get_token();
            if (token.charAt(0) != '=') {
                expression = t_ptr;
                token = temp_token;
                tok_type = VARIABLE;
            }
            else {
                get_token();
                eval_add_sub(result);
                vars[slot] = result;
                return;
            }
        }
        eval_add_sub(result);
    }

    private void eval_add_sub(double result) {
        char op;
        double temp = 0;
        eval_mul_div(result);
        while (!token.isEmpty() && ((op = token.charAt(0)) == '+' || op == '-')) {
            get_token();
            eval_mul_div(temp);
            switch (op) {
                case '-':
                    result = result - temp;
                    break;
                case '+':
                    result = result + temp;
                    break;
            }
        }
    }

    private void eval_mul_div(double result) {
        char op;
        double temp = 0;
        eval_exp(result);
        while (!token.isEmpty() && ((op = token.charAt(0)) == '*' || op == '/')) {
            get_token();
            temp = eval_exp(temp);
            switch (op) {
                case '*':
                    result = result * temp;
                    break;
                case '/':
                    result = result / temp;
                    break;
            }
        }
    }

    private double eval_exp(double result) {
        double temp = 0;
        eval_unary(result);
        while (!token.isEmpty() && token.charAt(0) == '^') {
            get_token();
            eval_unary(temp);
            result = pow(result, temp);
        }
        return result;
    }

    private double eval_unary(double result) {
        char op = 0;
        if ((tok_type == TokenType.DELIMITER) && (token.charAt(0) == '+' || token.charAt(0) == '-')) {
            op = token.charAt(0);
            get_token();
        }
        eval_func(result);
        if (op == '-') {
            result = -result;
        }
        return result;
    }

    private double eval_func(double result) {
        boolean isfunc = (tok_type == FUNCTION);
        String temp_token = "";
        if (isfunc) {
            temp_token = token;
            get_token();
        }
        if (token.charAt(0) == '(') {
            get_token();
            eval_add_sub(result);
            if (token.charAt(0) != ')') {
                throw new RuntimeException("Unbalanced parentheses");
            }
            if (isfunc) {
                assert temp_token != null;
                if (temp_token.equalsIgnoreCase("SIN")) {
                    result = sin( Math.PI/ 180.0 * result);
                }
                else if (temp_token.equalsIgnoreCase("COS")) {
                    result = cos(Math.PI / 180.0 * result);
                }
                else if (temp_token.equalsIgnoreCase("TAN")) {
                    result = tan(Math.PI / 180.0 * result);
                }
                else if (temp_token.equalsIgnoreCase("ASIN")) {
                    result = 180.0 / Math.PI * asin(result);
                }
                else if (temp_token.equalsIgnoreCase("ACOS")) {
                    result = 180.0 / Math.PI * acos(result);
                }
                else if (temp_token.equalsIgnoreCase("ATAN")) {
                    result = 180.0 / Math.PI * atan(result);
                }
                else if (temp_token.equalsIgnoreCase("SINH")) {
                    result = sinh(result);
                }
                else if (temp_token.equalsIgnoreCase("COSH")) {
                    result = cosh(result);
                }
                else if (temp_token.equalsIgnoreCase("TANH")) {
                    result = tanh(result);
                }
                else if (temp_token.equalsIgnoreCase("ASINH")) {
                    result = Math.log(result + Math.sqrt(result * result + 1.0)); // asinh(result);
                }
                else if (temp_token.equalsIgnoreCase("ACOSH")) {
                    result = Math.log(result + Math.sqrt(result * result - 1)); // acosh(result);
                }
                else if (temp_token.equalsIgnoreCase("ATANH")) {
                    result = Math.log((1 + result) * Math.sqrt(1 / (1 - result * result))); // atanh(result);
                }
                else if (temp_token.equalsIgnoreCase("LN")) {
                    result = log(result);
                }
                else if (temp_token.equalsIgnoreCase("LOG")) {
                    result = log10(result);
                }
                else if (temp_token.equalsIgnoreCase("EXP")) {
                    result = exp(result);
                }
                else if (temp_token.equalsIgnoreCase("SQRT")) {
                    result = sqrt(result);
                }
                else if (temp_token.equalsIgnoreCase("SQR")) {
                    result = result * result;
                }
                else if (temp_token.equalsIgnoreCase("ROUND")) {
                    result = round(result);
                }
                else if (temp_token.equalsIgnoreCase("FLOOR")) {
                    result = floor(result);
                }
                else {
                    throw new RuntimeException("Unknown function");
                }
            }
            get_token();
        }
        else {
            switch (tok_type) {
                case VARIABLE:
                    if (token.equalsIgnoreCase("MIN")) {
                        result = min;
                    }
                    else if (token.equalsIgnoreCase("MAX")) {
                        result = max;
                    }
                    else {
                        result = vars[token.charAt(0) - 'A'];
                    }
                    get_token();
                    return result;
                case NUMBER: {
                    try{
                        result = Double.parseDouble(token);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Result did not fit in a double");
                    }
                    get_token();
                    return result;
                }
                default:
                    throw new RuntimeException("Syntax error");
            }
        }
        return result;
    }

    public void get_token() {
        // token = exp_ptr; (En caso de terminar vacía, el token toma el resto)
        this.token = this.expression.substring(this.expr_p);
        this.tok_type = null;

        if (this.expr_p >= this.expression.length()) {
            return;
        }

        // Saltarse los espacios iniciales (while ISSPACE)
        while (this.expr_p < this.expression.length() && ISSPACE(this.expression.charAt(this.expr_p))) {
            this.expr_p++;
        }

        // Control por si tras saltar espacios llegamos al final del string
        if (this.expr_p >= this.expression.length()) {
            this.token = "";
            return;
        }

        char currentChar = this.expression.charAt(this.expr_p);

        // CASO 1: Es un delimitador
        if (ISDELIM(currentChar)) {
            this.tok_type = TokenType.DELIMITER;
            this.token = Character.toString(currentChar);
            this.expr_p++; // exp_ptr.remove_prefix(1);
        }
        // CASO 2: Es un identificador alfabético (Variable o Función)
        else if (Character.isAlphabetic(currentChar)) {
            int start = this.expr_p;

            // Buscar el primer delimitador o espacio hacia adelante
            while (this.expr_p < this.expression.length()) {
                char c = this.expression.charAt(this.expr_p);
                if (" +-/*%^=()".indexOf(c) != -1) {
                    break;
                }
                this.expr_p++;
            }

            this.token = this.expression.substring(start, this.expr_p);

            // Saltarse los espacios posteriores (while ISSPACE)
            while (this.expr_p < this.expression.length() && ISSPACE(this.expression.charAt(this.expr_p))) {
                this.expr_p++;
            }

            // Determinar si es FUNCIÓN o VARIABLE mirando el siguiente carácter sin avanzar el cursor
            if (this.expr_p < this.expression.length() && this.expression.charAt(this.expr_p) == '(') {
                this.tok_type = FUNCTION;
            } else {
                this.tok_type = VARIABLE;
            }
        }
        // CASO 3: Es un número literal
        else if (Character.isDigit(currentChar) || currentChar == '.') {
            int start = this.expr_p;

            // Buscar el final del número
            while (this.expr_p < this.expression.length()) {
                char c = this.expression.charAt(this.expr_p);
                if (" +-/*%^=()".indexOf(c) != -1) {
                    break;
                }
                this.expr_p++;
            }

            this.token = this.expression.substring(start, this.expr_p);
            this.tok_type = NUMBER;
        }

        // VALIDACIÓN POSTERIOR DE VARIABLES
        if (this.tok_type == VARIABLE) {
            // Reutilizamos el uxSimpleCaseCmp que simplificamos antes
            // Nota: Aquí "token" y "MIN"/"MAX" se comparan directamente como String de Java
            if (this.token.length() > 1 &&
                    !(this.token.equalsIgnoreCase("MIN") || this.token.equalsIgnoreCase("MAX"))) {
                throw new RuntimeException("Variables other than MIN and MAX must be 1 letter");
            }
        }
    }
}
