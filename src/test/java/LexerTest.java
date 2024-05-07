import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {

    @Test

    public void testIntegerToken() {
        Lexer lexer = new Lexer("123");
        Lexer.Token token = lexer.getToken();
        assertEquals(Lexer.TokenType.Integer, token.tokentype);
        assertEquals("123", token.value);
    }

    @Test
    public void testCommentToken() {
        Lexer lexer = new Lexer("/* This is a comment */");
        Lexer.Token token = lexer.getToken();
        assertEquals(Lexer.TokenType.End_of_input, token.tokentype);
        assertEquals("", token.value);
    }

    @Test
    public void testDivideToken() {
        Lexer lexer = new Lexer("/");
        Lexer.Token token = lexer.getToken();
        assertEquals(Lexer.TokenType.Op_divide, token.tokentype);
        assertEquals("", token.value);
    }



}
