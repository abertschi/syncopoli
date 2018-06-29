package org.amoradi.syncopoli;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ArgumentTokenizerTest {
    protected void printList(ArrayList<String> l) {
        for (String s : l) {
            System.out.print(s + "|");
        }
    }

    @Test
    public void basic_single_word() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hello");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello"))
                            )
                   );
    }
    
    @Test
    public void basic_multi_word() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hello world");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello", "world"))
                            )
                   );
    }

    @Test
    public void escape_space() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hello\\ world");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello world"))
                            )
                   );
    }

    @Test
    public void escape_single_quote() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hello\\' world");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello'", "world"))
                            )
                   );
    }

    @Test
    public void escape_double_quote() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hello\\\" world");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello\"", "world"))
                            )
                   );
    }

    @Test
    public void escape_backslash() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hello\\\\world");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello\\world"))
                            )
                   );
    }

    @Test
    public void stringify_single_quote() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hell 'o w' orld");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hell", "o w", "orld"))
                            )
                   );
    }

    @Test
    public void stringify_single_quote_attached() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hell'o w' orld");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello w", "orld"))
                            )
                   );
    }

    public void stringify_single_quote_missing() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hell \'o w orld");
        assertNull(t);
    }

    @Test
    public void stringify_double_quote() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hell \"o w\" orld");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hell", "o w", "orld"))
                            )
                   );
    }

    @Test
    public void stringify_double_quote_attached() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hell\"o w\" orld");
        assertTrue(t.equals(
                            new ArrayList<String>(Arrays.asList("hello w", "orld"))
                            )
                   );
    }

    @Test
    public void stringify_double_quote_missing() throws Exception {
        ArrayList<String> t = ArgumentTokenizer.tokenize("hell \"o w orld");
        assertNull(t);
    }
        
}
