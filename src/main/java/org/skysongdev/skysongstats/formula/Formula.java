package org.skysongdev.skysongstats.formula;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToDoubleFunction;

// A stat formula such as "60 + {con} * 10", parsed once and evaluated many times.
//
// Supported: numbers (whole or decimal), {stat} references, + - * /, unary minus
// and parentheses, with the usual precedence. Intermediate results keep their
// decimals and the final result is rounded down, so "{foc} / 2 * 3" with foc = 3
// gives 4 (from 4.5). Stat ids inside braces are case-insensitive: {Con} and
// {con} are the same stat. There is no scripting engine behind this on purpose:
// a formula can only do arithmetic on stats.
public final class Formula {
    private final String source;
    private final Node root;
    private final Set<String> references;

    private Formula(String source, Node root, Set<String> references) {
        this.source = source;
        this.root = root;
        this.references = Collections.unmodifiableSet(references);
    }

    public static Formula parse(String source) {
        Objects.requireNonNull(source, "source");
        return new Parser(source).parse();
    }

    // The stat ids this formula reads, lowercased, in the order they first appear.
    // Stat definitions use this to reject unknown stats and circular formulas.
    public Set<String> references() {
        return references;
    }

    // Evaluates the formula, asking statValues for the value of each referenced
    // stat id (lowercased), and rounds the result down.
    public int evaluate(ToDoubleFunction<String> statValues) {
        Objects.requireNonNull(statValues, "statValues");
        return (int) Math.floor(evaluate(root, statValues));
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return source;
    }

    private double evaluate(Node node, ToDoubleFunction<String> statValues) {
        return switch (node) {
            case Constant constant -> constant.value();
            case StatReference reference -> statValues.applyAsDouble(reference.statId());
            case Negate negate -> -evaluate(negate.operand(), statValues);
            case Binary binary -> apply(binary.operator(),
                    evaluate(binary.left(), statValues), evaluate(binary.right(), statValues));
        };
    }

    private double apply(char operator, double left, double right) {
        return switch (operator) {
            case '+' -> left + right;
            case '-' -> left - right;
            case '*' -> left * right;
            default -> divide(left, right);
        };
    }

    private double divide(double left, double right) {
        if (right == 0) {
            throw new ArithmeticException("Division by zero in formula \"" + source + "\"");
        }
        return left / right;
    }

    private sealed interface Node permits Constant, StatReference, Negate, Binary {
    }

    private record Constant(double value) implements Node {
    }

    private record StatReference(String statId) implements Node {
    }

    private record Negate(Node operand) implements Node {
    }

    private record Binary(char operator, Node left, Node right) implements Node {
    }

    // Recursive descent parser for:
    //   expression := term (("+" | "-") term)*
    //   term       := unary (("*" | "/") unary)*
    //   unary      := "-" unary | "+" unary | primary
    //   primary    := number | "{" statId "}" | "(" expression ")"
    private static final class Parser {
        private final String source;
        private final Set<String> references = new LinkedHashSet<>();
        private int position;

        Parser(String source) {
            this.source = source;
        }

        Formula parse() {
            skipWhitespace();
            if (atEnd()) {
                throw error("Formula is empty");
            }
            Node root = expression();
            skipWhitespace();
            if (!atEnd()) {
                throw error("Unexpected '" + peek() + "'");
            }
            return new Formula(source, root, references);
        }

        private Node expression() {
            Node node = term();
            while (true) {
                skipWhitespace();
                if (accept('+')) {
                    node = new Binary('+', node, term());
                } else if (accept('-')) {
                    node = new Binary('-', node, term());
                } else {
                    return node;
                }
            }
        }

        private Node term() {
            Node node = unary();
            while (true) {
                skipWhitespace();
                if (accept('*')) {
                    node = new Binary('*', node, unary());
                } else if (accept('/')) {
                    node = new Binary('/', node, unary());
                } else {
                    return node;
                }
            }
        }

        private Node unary() {
            skipWhitespace();
            if (accept('-')) {
                return new Negate(unary());
            }
            if (accept('+')) {
                return unary();
            }
            return primary();
        }

        private Node primary() {
            skipWhitespace();
            if (atEnd()) {
                throw error("Expected a number, {stat} or '(' but the formula ended");
            }
            char c = peek();
            if (Character.isDigit(c) || c == '.') {
                return number();
            }
            if (accept('{')) {
                return statReference();
            }
            if (accept('(')) {
                int open = position - 1;
                Node inner = expression();
                skipWhitespace();
                if (!accept(')')) {
                    throw new FormulaException("Missing ')' for the '(' opened", source, open);
                }
                return inner;
            }
            throw error("Expected a number, {stat} or '(' but found '" + c + "'");
        }

        private Node number() {
            int start = position;
            while (!atEnd() && (Character.isDigit(peek()) || peek() == '.')) {
                position++;
            }
            String text = source.substring(start, position);
            try {
                return new Constant(Double.parseDouble(text));
            } catch (NumberFormatException e) {
                throw new FormulaException("Invalid number '" + text + "'", source, start);
            }
        }

        private Node statReference() {
            int open = position - 1;
            int end = source.indexOf('}', position);
            if (end < 0) {
                throw new FormulaException("Missing '}' for the '{' opened", source, open);
            }
            String statId = source.substring(position, end).trim().toLowerCase(Locale.ROOT);
            if (statId.isEmpty()) {
                throw new FormulaException("Empty stat reference {}", source, open);
            }
            if (!statId.matches("[a-z0-9_-]+")) {
                throw new FormulaException("Invalid stat id '" + statId
                        + "' (use letters, digits, '_' or '-')", source, open);
            }
            position = end + 1;
            references.add(statId);
            return new StatReference(statId);
        }

        private boolean accept(char expected) {
            if (!atEnd() && peek() == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(peek())) {
                position++;
            }
        }

        private boolean atEnd() {
            return position >= source.length();
        }

        private char peek() {
            return source.charAt(position);
        }

        private FormulaException error(String message) {
            return new FormulaException(message, source, position);
        }
    }
}
