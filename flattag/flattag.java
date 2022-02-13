/*
 * flattag
 *
 * Copyright (c) 2022 Yuichiro MORIGUCHI
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/mit-license.php
 **/
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;

public class flattag extends Exception {

    public int exceptionLineNo;

    public flattag(String message) {
        super(message);
        exceptionLineNo = lineNo;
    }

    public String getMessageWithLine() {
        return "line " + exceptionLineNo + ": " + getMessage();
    }

    // I/O
    public static Reader input;
    public static PrintWriter output;

    // options
    public static int delimiter;
    public static int attrPrefix;
    public static int attrInfix;
    public static String lf;
    public static String tab;
    public static boolean ignoreAttr;
    public static boolean attrLine;
    public static HashSet<String> autoClose;
    public static String outputFileName;

    private static LinkedList<String[]> tagStack;
    public static int lineNo;

    private static int readChar() throws IOException {
        int result = input.read();

        if(result == '\n') {
            lineNo++;
        }
        return result;
    }

    private static String[] consTuple(String item1, String item2) {
        return new String[] { item1, item2 };
    }

    private static boolean containsTag(String tagName) {
        for(String[] tuple : tagStack) {
            if(tuple[0].equals(tagName)) {
                return true;
            }
        }
        return false;
    }

    private static void pushTagStack(String tagName, String aString) {
        if(autoClose.contains(tagName) && containsTag(tagName)) {
            popTagStack(tagName);
        }
        tagStack.addLast(consTuple(tagName, aString));
    }

    private static void pushTagStack(String tagName) {
        pushTagStack(tagName, tagName);
    }

    private static void popTagStack(String aString) {
        while(!tagStack.isEmpty() && !tagStack.removeLast()[0].equals(aString)) {}
    }

    private static void printTagStack() {
        for(String[] tuple : tagStack) {
            output.print(tuple[1]);
            output.print((char)delimiter);
        }
    }

    private static void printText(String text) {
        printTagStack();
        output.println(text);
    }

    private static void printKeyValue(LinkedList<String[]> keyValue) {
        for(String[] tuple : keyValue) {
            printTagStack();
            output.print((char)attrPrefix);
            output.print(tuple[0]);
            output.print((char)delimiter);
            output.println(tuple[1]);
        }
    }

    private static final int INNER_TAG = 0;
    private static final int TAG_OPEN_INIT = 100;
    private static final int TAG_OPEN = 101;
    private static final int TAG_ATTR = 200;
    private static final int TAG_ATTR_KEY = 201;
    private static final int TAG_ATTR_VALUE_INIT = 202;
    private static final int TAG_ATTR_VALUE_DOUBLE = 203;
    private static final int TAG_ATTR_VALUE_SINGLE = 204;
    private static final int TAG_ATTR_VALUE_NOQUOTE = 205;
    private static final int TAG_EMPTY = 206;
    private static final int TAG_CLOSE_INIT = 300;
    private static final int TAG_CLOSE = 301;
    private static final int TAG_SKIP_DEFINITION = 400;
    private static final int TAG_SKIP_BANG = 401;
    private static final int TAG_SKIP_BANG2 = 402;
    private static final int TAG_SKIP_DOCTYPE = 403;
    private static final int TAG_SKIP_COMMENT = 404;
    private static final int TAG_SKIP_COMMENT2 = 405;
    private static final int TAG_SKIP_COMMENT3 = 406;

    private static void appendBuilder(StringBuilder builder, char ch) {
        if(ch == '\n') {
            builder.append(lf);
        } else if(ch == '\t') {
            builder.append(tab);
        } else {
            builder.append((char)ch);
        }
    }

    private static boolean isWhitespace(int ch) {
        return Character.isWhitespace(ch);
    }

    private static String keyValueToString(String tagName, LinkedList<String[]> keyValue) {
        StringBuilder builder = new StringBuilder(tagName);

        for(String[] tuple : keyValue) {
            builder.append((char)attrPrefix);
            builder.append(tuple[0]);
            builder.append((char)attrInfix);
            builder.append(tuple[1]);
        }
        return builder.toString();
    }

    private static void pushTagAndKeyValue(String tagName, LinkedList<String[]> keyValue) {
        if(ignoreAttr) {
            pushTagStack(tagName);
        } else if(attrLine) {
            pushTagStack(tagName);
            printKeyValue(keyValue);
        } else {
            pushTagStack(tagName, keyValueToString(tagName, keyValue));
        }
    }

    public static void parseTag() throws IOException, flattag {
        StringBuilder builder = new StringBuilder();
        String[] tuple;
        String tagName = "", key = "";
        LinkedList<String[]> keyValue = new LinkedList<String[]>();
        int ch, state = INNER_TAG, doctypeBrackets = 0;

        while(true) {
            if((ch = readChar()) < 0) {
                if(state == INNER_TAG) {
                    if(builder.length() > 0) {
                        printText(builder.toString());
                    }
                    return;
                } else {
                    throw new flattag("unexpected EOF");
                }
            }

            switch(state) {
            case INNER_TAG:
                if(ch == '<') {
                    state = TAG_OPEN_INIT;
                } else {
                    appendBuilder(builder, (char)ch);
                }
                break;

            case TAG_OPEN_INIT:
                if(ch == '>' || ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid tag");
                } else if(ch == '/') {
                    printText(builder.toString());
                    keyValue = new LinkedList<String[]>();
                    builder = new StringBuilder();
                    state = TAG_CLOSE_INIT;
                } else if(ch == '?') {
                    state = TAG_SKIP_DEFINITION;
                } else if(ch == '!') {
                    state = TAG_SKIP_BANG;
                } else if(!isWhitespace(ch)) {
                    if(builder.length() > 0) {
                        printText(builder.toString());
                    }
                    keyValue = new LinkedList<String[]>();
                    builder = new StringBuilder();
                    appendBuilder(builder, (char)ch);
                    state = TAG_OPEN;
                }
                break;

            case TAG_OPEN:
                if(ch == '>') {
                    pushTagStack(builder.toString());
                    builder = new StringBuilder();
                    state = INNER_TAG;
                } else if(ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid tag");
                } else if(isWhitespace(ch)) {
                    tagName = builder.toString();
                    state = TAG_ATTR;
                } else {
                    appendBuilder(builder, (char)ch);
                }
                break;

            case TAG_ATTR:
                if(ch == '>') {
                    pushTagAndKeyValue(tagName, keyValue);
                    builder = new StringBuilder();
                    state = INNER_TAG;
                } else if(ch == '/') {
                    state = TAG_EMPTY;
                } else if(ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid attribute");
                } else if(!isWhitespace(ch)) {
                    builder = new StringBuilder();
                    appendBuilder(builder, (char)ch);
                    state = TAG_ATTR_KEY;
                }
                break;

            case TAG_ATTR_KEY:
                if(ch == '=') {
                    key = builder.toString();
                    builder = new StringBuilder();
                    state = TAG_ATTR_VALUE_INIT;
                } else if(ch == '>') {
                    tuple = new String[] { builder.toString(), "" };
                    keyValue.add(tuple);
                    pushTagAndKeyValue(tagName, keyValue);
                    builder = new StringBuilder();
                    state = INNER_TAG;
                } else if(ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid attribute");
                } else if(isWhitespace(ch)) {
                    tuple = new String[] { builder.toString(), "" };
                    keyValue.add(tuple);
                    state = TAG_ATTR;
                } else {
                    appendBuilder(builder, (char)ch);
                }
                break;

            case TAG_ATTR_VALUE_INIT:
                if(ch == '\"') {
                    state = TAG_ATTR_VALUE_DOUBLE;
                } else if(ch == '\'') {
                    state = TAG_ATTR_VALUE_SINGLE;
                } else if(ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid attribute");
                } else if(isWhitespace(ch)) {
                    tuple = new String[] { key, "" };
                    keyValue.add(tuple);
                    state = TAG_ATTR;
                } else {
                    appendBuilder(builder, (char)ch);
                    state = TAG_ATTR_VALUE_NOQUOTE;
                }
                break;

            case TAG_ATTR_VALUE_DOUBLE:
                if(ch == '\"') {
                    tuple = new String[] { key, builder.toString() };
                    keyValue.add(tuple);
                    state = TAG_ATTR;
                } else {
                    appendBuilder(builder, (char)ch);
                }
                break;

            case TAG_ATTR_VALUE_SINGLE:
                if(ch == '\'') {
                    tuple = new String[] { key, builder.toString() };
                    keyValue.add(tuple);
                    state = TAG_ATTR;
                } else {
                    appendBuilder(builder, (char)ch);
                }
                break;

            case TAG_ATTR_VALUE_NOQUOTE:
                if(isWhitespace(ch)) {
                    tuple = new String[] { key, builder.toString() };
                    keyValue.add(tuple);
                    state = TAG_ATTR;
                } else if(ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid attribute");
                } else if(ch == '>') {
                    pushTagAndKeyValue(tagName, keyValue);
                    builder = new StringBuilder();
                    state = INNER_TAG;
                } else {
                    appendBuilder(builder, (char)ch);
                }
                break;

            case TAG_EMPTY:
                if(ch == '>') {
                    pushTagAndKeyValue(tagName, keyValue);
                    printText("");
                    popTagStack(tagName);
                    builder = new StringBuilder();
                    state = INNER_TAG;
                } else if(!isWhitespace(ch)) {
                    throw new flattag("invalid empty tag");
                }
                break;

            case TAG_CLOSE_INIT:
                if(ch == '>') {
                    throw new flattag("invalid close tag");
                } else if(ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid close tag");
                } else if(!isWhitespace(ch)) {
                    appendBuilder(builder, (char)ch);
                    state = TAG_CLOSE;
                }
                break;

            case TAG_CLOSE:
                if(ch == '>') {
                    popTagStack(builder.toString());
                    builder = new StringBuilder();
                    state = INNER_TAG;
                } else if(ch == '<' || ch == '\"' || ch == '\'') {
                    throw new flattag("invalid close tag");
                } else if(!isWhitespace(ch)) {
                    appendBuilder(builder, (char)ch);
                }
                break;

            case TAG_SKIP_DEFINITION:
                if(ch == '>') {
                    state = INNER_TAG;
                }
                break;

            case TAG_SKIP_BANG:
                if(ch == '-') {
                    state = TAG_SKIP_BANG2;
                } else if(ch == '>') {
                    state = INNER_TAG;
                } else {
                    doctypeBrackets = 1;
                    state = TAG_SKIP_DOCTYPE;
                }
                break;

            case TAG_SKIP_BANG2:
                if(ch == '-') {
                    state = TAG_SKIP_COMMENT;
                } else if(ch == '>') {
                    state = INNER_TAG;
                } else {
                    doctypeBrackets = 1;
                    state = TAG_SKIP_DOCTYPE;
                }
                break;

            case TAG_SKIP_DOCTYPE:
                if(ch == '>' && --doctypeBrackets == 0) {
                    state = INNER_TAG;
                } else if(ch == '<') {
                    doctypeBrackets++;
                }
                break;

            case TAG_SKIP_COMMENT:
                if(ch == '-') {
                    state = TAG_SKIP_COMMENT2;
                }
                break;

            case TAG_SKIP_COMMENT2:
                if(ch == '-') {
                    state = TAG_SKIP_COMMENT3;
                } else {
                    state = TAG_SKIP_COMMENT;
                }
                break;

            case TAG_SKIP_COMMENT3:
                if(ch == '>') {
                    state = INNER_TAG;
                } else if(ch != '-') {
                    state = TAG_SKIP_COMMENT;
                }
                break;

            default:
                throw new RuntimeException("Internal error");
            }
        }
    }

    private static void addAutoClose(String option) {
        String[] tags = option.split(",");

        for(String tag : tags) {
            autoClose.add(tag);
        }
    }

    public static final int DEFAULT_DELIMITER = '\t';
    public static final int DEFAULT_ATTR_PREFIX = '@';
    public static final int DEFAULT_ATTR_INFIX = '=';
    public static final String DEFAULT_LF = " ";
    public static final String DEFAULT_TAB = " ";

    public static void initOptions() {
        delimiter = DEFAULT_DELIMITER;
        attrPrefix = DEFAULT_ATTR_PREFIX;
        attrInfix = DEFAULT_ATTR_INFIX;
        lf = DEFAULT_LF;
        tab = DEFAULT_TAB;
        ignoreAttr = false;
        attrLine = false;
        autoClose = new HashSet<String>();
        outputFileName = null;
        tagStack = new LinkedList<String[]>();
        lineNo = 1;
    }

    private static int getArgChar(String arg, int defaultValue) {
        if(arg.equals("\\t")) {
            return '\t';
        } else {
            return arg.length() > 0 ? arg.charAt(0) : defaultValue;
        }
    }

    public static int parseOption(String... args) {
        int argptr;

        for(argptr = 0; argptr < args.length;) {
            if(args[argptr].equals("-I")) {
                ignoreAttr = true;
                argptr++;
            } else if(args[argptr].equals("-L")) {
                attrLine = true;
                argptr++;
            } else if(args[argptr].equals("-d") && argptr < args.length - 1) {
                delimiter = getArgChar(args[argptr + 1], DEFAULT_DELIMITER);
                argptr += 2;
            } else if(args[argptr].equals("-a") && argptr < args.length - 1) {
                attrPrefix = getArgChar(args[argptr + 1], DEFAULT_ATTR_PREFIX);
                argptr += 2;
            } else if(args[argptr].equals("-i") && argptr < args.length - 1) {
                attrInfix = getArgChar(args[argptr + 1], DEFAULT_ATTR_INFIX);
                argptr += 2;
            } else if(args[argptr].equals("-n") && argptr < args.length - 1) {
                lf = args[argptr + 1];
                argptr += 2;
            } else if(args[argptr].equals("-t") && argptr < args.length - 1) {
                tab = args[argptr + 1];
                argptr += 2;
            } else if(args[argptr].equals("-c") && argptr < args.length - 1) {
                addAutoClose(args[argptr + 1]);
                argptr += 2;
            } else if(args[argptr].equals("-o") && argptr < args.length - 1) {
                outputFileName = args[argptr + 1];
                argptr += 2;
            } else if(args[argptr].length() > 0 && args[argptr].charAt(0) == '-') {
                return -1;
            } else {
                break;
            }
        }
        return argptr;
    }

    private static void usage() {
        System.err.println("usage: flattag [options] [file]");
        System.err.println();
        System.err.println("options:");
        System.err.println("-F delimiter");
        System.err.println("  Specify one character of delimiter.");
        System.err.println("  The default is tab.");
        System.err.println("-a attribute-prefix");
        System.err.println("  Specify attribute prefix added beginning of attribute.");
        System.err.println("  The default is '@'.");
        System.err.println("-i attribute-infix");
        System.err.println("  Specify attribute infix added between attribute key and its value.");
        System.err.println("  The default is '='.");
        System.err.println("-c auto-close-tag-name");
        System.err.println("  Tags which close automatically if another tag is occurred are specified.");
        System.err.println("  Tags must delimited by comma.");
        System.err.println("-n newline-replace");
        System.err.println("  A string to which newline character is replaced is specified.");
        System.err.println("  The default is one space character.");
        System.err.println("-t tab-replace");
        System.err.println("  A string to which tab character is replaced is specified.");
        System.err.println("  The default is one space character.");
        System.err.println("-I");
        System.err.println("  Ignores the attributes of tag.");
        System.err.println("-L");
        System.err.println("  Treats the attribute like a tag.");
        System.err.println("  The 'tag' is the name of attribute and the 'value' is the value of attribute.");
    }

    public static void main(String[] args) {
        int returnCode = 0, argptr;

        initOptions();
        if((argptr = parseOption(args)) < 0) {
            usage();
            System.exit(2);
        }

        if(argptr < args.length) {
            try {
                input = new BufferedReader(new InputStreamReader(new FileInputStream(args[argptr])));
            } catch(IOException e) {
                System.err.println("Cannot open file " + args[argptr]);
                System.exit(4);
            }
        } else {
            input = new BufferedReader(new InputStreamReader(System.in));
        }

        if(outputFileName != null) {
            try {
                output = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)));
            } catch(IOException e) {
                System.err.println("Cannot open file " + outputFileName);
                System.exit(4);
            }
        } else {
            output = new PrintWriter(new OutputStreamWriter(System.out));
        }

        try {
            parseTag();
        } catch(IOException e) {
            throw new RuntimeException(e);
        } catch(flattag e) {
            System.err.println(e.getMessageWithLine());
            returnCode = 4;
        } finally {
            try {
                input.close();
                output.close();
            } catch(IOException e) {
                throw new RuntimeException();
            }
        }
        System.exit(returnCode);
    }

}

