import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * @author rosie
 * Lexer class for tokenizing input text into tokens
 */
public class Lexer {
    private int line;
    private int pos;
    private int position;
    private char chr;
    private String s;

    Map<String, TokenType> keywords = new HashMap<>();

    /**
     * @param token The tokentype of the token
     * @param value The value associated with the token
     * @param line The line number in the input text where the token is located
     * @param pos The position within the line where the token starts
     */
    static class Token {
        public TokenType tokentype;
        public String value;
        public int line;
        public int pos;
        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }

        /**
         *
         * @return string representing the Token
         */
        @Override
        public String toString() {
            String result = String.format("%5d  %5d %-15s", this.line, this.pos, this.tokentype);
            switch (this.tokentype) {
                case Integer:
                    result += String.format("  %4s", value);
                    break;
                case Identifier:
                    result += String.format(" %s", value);
                    break;
                case String:
                    result += String.format(" \"%s\"", value);
                    break;
            }
            return result;
        }
    }

    static enum TokenType {
        End_of_input, Op_multiply,  Op_divide, Op_mod, Op_add, Op_subtract,
        Op_negate, Op_not, Op_less, Op_lessequal, Op_greater, Op_greaterequal,
        Op_equal, Op_notequal, Op_assign, Op_and, Op_or, Keyword_if,
        Keyword_else, Keyword_while, Keyword_print, Keyword_putc, LeftParen, RightParen,
        LeftBrace, RightBrace, Semicolon, Comma, Identifier, Integer, String
    }

    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s in line %d, pos %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }

    Lexer(String source) {
        this.line = 1;
        this.pos = 0;
        this.position = 0;
        this.s = source;
        this.chr = this.s.charAt(0);
        this.keywords.put("if", TokenType.Keyword_if);
        this.keywords.put("else", TokenType.Keyword_else);
        this.keywords.put("print", TokenType.Keyword_print);
        this.keywords.put("putc", TokenType.Keyword_putc);
        this.keywords.put("while", TokenType.Keyword_while);

    }


    Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (getNextChar() == expect) {
            getNextChar();
            return new Token(ifyes, "", line, pos);
        }
        if (ifno == TokenType.End_of_input) {
            error(line, pos, String.format("follow: unrecognized character: (%d) '%c'", (int)this.chr, this.chr));
        }
        return new Token(ifno, "", line, pos);
    }

    /**
     *
     * @param line number where the character literal is located
     * @param pos The position within the line where the character literal starts
     * @return A token representing the parsed character literal
     */
    Token char_lit(int line, int pos) { // handle character literals
        char c = getNextChar(); // skip opening quote
        int n = (int)c;
        //check for escape sequences
        if (c == '\\') {
            //if the character after '\' is 'n', treat it as a newline character
            c = getNextChar();
            switch (c) {
                case 'n':
                    n = 10; // ASCII value for newline character
                    break;
                case '\\':
                    n = '\\'; // Treat '\\' as a single backslash
                    break;
                default:
                    error(line, pos, String.format("unknown escape sequence \\%c", c));
                    break;
            }
        }
        // Ensure there's only one character in the literal, which should be the closing quote
        if (c != '\'') {
            error(line, pos, "multi-character constant");
        }
        //Move past the closing quote
        getNextChar();
        return new Token(TokenType.Integer, "" + n, line, pos);
    }

    /**
     *
     * @param start The Character marks the start of string literal
     * @param line The line number where the string  literal is located
     * @param pos The position within the line where the string literal starts
     * @return A token represented the parsed string literal
     */
    Token string_lit(char start, int line, int pos) { // handle string literals
        String result = "";
        while (getNextChar() != start) {
            //check for null character, indicating an unexpected end of file
            if (this.chr == '\u0000') {
                error(line, pos, "End of file");
            }
            //check for newline character
            if (this.chr == '\n') {
                error(line, pos, "End of file");
            }
            result += this.chr;
        }
        //Move past the start character to prepare for next token
        getNextChar();
        return new Token(TokenType.String, result, line, pos);
    }

    /**
     *
     * @param line The line number where the division operator or comment starts
     * @param pos The position within th line where the division operator or comment starts
     * @return A token that represents either a division operator or the end of a comment
     */
    Token div_or_comment(int line, int pos) { // handle division or comments
        // code here
        char currentChar = getNextChar(); //Get the next character
        //If the next character is not '*', it's a division operator
        if (currentChar != '*') {
            return new Token(TokenType.Op_divide, "", line, pos);
        }
        //Move to the next character
        getNextChar();

        //check for comments
        while (true) {
            //If the current character is null(end of file), raise an error
            if (this.chr == '\u0000') {
                error(line, pos, "End of file in comment");

                //if the current character is '*', check if it's part of the end of a comment
            } else if (this.chr == '*') {
                //Move to the next character
                char nextChar = getNextChar();
                //If the next character is '/', the comment ends
                if (getNextChar() == '/') {
                    //Move the end of comment and return the next token
                    getNextChar();
                    return getToken();
                }
                //Move to the next character if it's not the end of a comment
            } else {
                currentChar = getNextChar();
            }
        }
    }

    /**
     *
     * @param line The line number where the identifier or integer starts
     * @param pos The position within the line where the identifier or integer starts
     * @return A token representing either an identifier, integer or keyword
     */
    Token identifier_or_integer(int line, int pos) { // handle identifiers and integers
        boolean is_number = true;
        String text = "";
        while (Character.isAlphabetic(this.chr) || Character.isDigit(this.chr)) {
        text += this.chr; //Add character to the text

            if (!Character.isDigit(this.chr)){
                is_number = false; //if it's not a digit, it's not a number
            }
            getNextChar();

        }
        if (Character.isDigit(text.charAt(0)) && is_number) {
            return new Token(TokenType.Integer, text, line, pos);
        }
        //check if it's a keyword
        if(this.keywords.containsKey(text)) {
            return new Token(this.keywords.get(text), "", line, pos); //created a toke for the keyword
        }
        //if not a number or keyword, return as in identifier
        return new Token(TokenType.Identifier, text, line, pos);
    }


    Token getToken() {
        int line, pos;
        while (Character.isWhitespace(this.chr)) {
            getNextChar();
        }
        line = this.line;
        pos = this.pos;

        // switch statement on character for all forms of tokens with return to follow.... one example left for you

        switch (this.chr) {
            case '\u0000': return new Token(TokenType.End_of_input, "", this.line, this.pos);

            case '(':
                chr = getNextChar();
                return new Token(TokenType.LeftParen, "", line, pos);
            case ')':
                chr = getNextChar();
                return new Token(TokenType.RightParen, "", line, pos);
            case '{' :
                chr = getNextChar();
                return new Token(TokenType.LeftBrace, "", line, pos);
            case'}' :
                chr = getNextChar();
                return new Token(TokenType.RightBrace, "", line, pos);
            case ';':
                chr = getNextChar();
                return new Token(TokenType.Semicolon, "", line, pos);
            case ',':
                chr = getNextChar();
                return new Token(TokenType.Comma, "", line, pos);

            case '*':
                chr = getNextChar();
                return new Token(TokenType.Op_multiply, "", line, pos);
            case '/':
                chr = getNextChar();
               return div_or_comment(line, pos);
            case '%' :
                chr = getNextChar();
                return new Token(TokenType.Op_mod, "", line,pos);
            case'+' :
                chr = getNextChar();
                return new Token(TokenType.Op_add, "", line,pos);
            case '-':
                chr = getNextChar();
                return new Token(TokenType.Op_subtract, "", line, pos);
            case '<':
                chr = getNextChar();
                if(chr != '='){
                    return  new Token(TokenType.Op_less, "", line, pos);
                }
                else {
                    return  new Token(TokenType.Op_lessequal, "", line, pos);
                }

            case '>':
                chr = getNextChar();
                if (chr != '=') {
                    return new Token(TokenType.Op_greater, "",line,pos);
                }
                else {
                    return  new Token(TokenType.Op_greaterequal, "", line, pos);
                }
            case '!':
                chr = getNextChar();
                if (chr != '=') {
                    return new Token(TokenType.Op_not, "", line,pos);
                }
                else {
                    return  new Token(TokenType.Op_notequal, "", line, pos);
                }
            case '=':
                chr = getNextChar();
                if (chr != '=') {
                    return new Token(TokenType.Op_assign, "", line, pos);
                }
                else {
                    return  new Token(TokenType.Op_equal, "", line, pos);
                }
                case '&':
                chr = getNextChar();
                if (chr == '&') {
                    return new Token(TokenType.Op_and, "", line, pos);
                }
                else {
                    return char_lit(line, pos);
                }
            case '|':
                chr = getNextChar();
                if (chr == '|') {
                    return new Token(TokenType.Op_or, "", line,pos);
                }
                else {
                    return char_lit(line, pos);
                }
            case '"':

                return string_lit(this.chr, line, pos);

                default: return identifier_or_integer(line, pos);
        }
    }


    char getNextChar() {
        this.pos++;
        this.position++;
        if (this.position >= this.s.length()) {
            this.chr = '\u0000';
            return this.chr;
        }
        this.chr = this.s.charAt(this.position);
        if (this.chr == '\n') {
            this.line++;
            this.pos = 0;
        }
        return this.chr;
    }


    String printTokens() {
        Token t;
        StringBuilder sb = new StringBuilder();
        while ((t = getToken()).tokentype != TokenType.End_of_input) {
            sb.append(t);
            sb.append("\n");
            System.out.println(t);
        }
        sb.append(t);
        System.out.println(t);
        return sb.toString();
    }

    static void outputToFile(String result) {
        try {
            FileWriter myWriter = new FileWriter("src/main/resources/hello.lex");
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (1==1) {
            try {

                File f = new File("src/main/resources/count.c");
                Scanner s = new Scanner(f);
                String source = " ";
                String result = " ";
                while (s.hasNext()) {
                    source += s.nextLine() + "\n";
                }
                Lexer l = new Lexer(source);
                result = l.printTokens();

                outputToFile(result);

            } catch(FileNotFoundException e) {
                error(-1, -1, "Exception: " + e.getMessage());
            }
        } else {
            error(-1, -1, "No args");
        }
    }
}