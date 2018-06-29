package org.amoradi.syncopoli;

import java.util.List;
import java.util.ArrayList;

public class ArgumentTokenizer {
    enum STATE {
        ESCAPED,
        STRINGIFIED_SINGLE,
        STRINGIFIED_DOUBLE,
        NORMAL
    }

    public static ArrayList<String> tokenize(String args) {
        ArrayList<String> tokens = new ArrayList<String>();
        
        String cur_token = "";
        STATE cur_state = STATE.NORMAL;

        for (char c : args.toCharArray()) {
            switch(c) {

            case '\\':
                if (cur_state == STATE.ESCAPED) {
                    cur_token += c;
                    cur_state = STATE.NORMAL;
                } else if (cur_state == STATE.STRINGIFIED_DOUBLE) {
                    cur_token += c;
                } else if (cur_state == STATE.STRINGIFIED_SINGLE) {
                    cur_token += c;
                } else {
                    cur_state = STATE.ESCAPED;
                }

                break;

            case '\"':
                if (cur_state == STATE.ESCAPED) {
                    cur_token += c;
                    cur_state = STATE.NORMAL;

                } else if (cur_state == STATE.STRINGIFIED_DOUBLE) {
                    cur_state = STATE.NORMAL;
                    tokens.add(cur_token);
                    cur_token = "";

                } else if (cur_state == STATE.STRINGIFIED_SINGLE) {
                    cur_token += c;

                } else {
                    cur_state = STATE.STRINGIFIED_DOUBLE;
                }

                break;

            case '\'':
                if (cur_state == STATE.ESCAPED) {
                    cur_token += c;
                    cur_state = STATE.NORMAL;

                } else if (cur_state == STATE.STRINGIFIED_DOUBLE) {
                    cur_token += c;

                } else if (cur_state == STATE.STRINGIFIED_SINGLE) {
                    cur_state = STATE.NORMAL;
                    tokens.add(cur_token);
                    cur_token = "";

                } else {
                    cur_state = STATE.STRINGIFIED_SINGLE;
                }

                break;

            case ' ':
                if (cur_state == STATE.ESCAPED) {
                    cur_token += c;
                    cur_state = STATE.NORMAL;

                } else if (cur_state == STATE.STRINGIFIED_SINGLE ||
                           cur_state == STATE.STRINGIFIED_DOUBLE) {
                    cur_token += c;

                } else {
                    if (!cur_token.isEmpty()) {
                        tokens.add(cur_token);
                        cur_token = "";
                    }
                }

                break;
                    
            default:
                cur_token += c;
            }
        }

        if (!cur_token.isEmpty()) {
            tokens.add(cur_token);
        }

        if (cur_state != STATE.NORMAL) {
            return null;
        }

        return tokens;
    }
}
