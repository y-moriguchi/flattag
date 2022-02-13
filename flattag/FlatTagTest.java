/*
 * flattag
 *
 * Copyright (c) 2022 Yuichiro MORIGUCHI
 *
 * This software is released under the MIT License.
 * http\t//opensource.org/licenses/mit-license.php
 **/
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FlatTagTest {

    private void assertExec(String file, String expected) {
        StringWriter result = new StringWriter();

        try {
            flattag.lineNo = 1;
            flattag.input = new StringReader(file);
            flattag.output = new PrintWriter(result);
            flattag.parseTag();
            assertEquals(expected, result.toString());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertSyntaxError(String file, int lineNo, String message) {
        StringWriter result = new StringWriter();

        try {
            flattag.lineNo = 1;
            flattag.input = new StringReader(file);
            flattag.output = new PrintWriter(result);
            flattag.parseTag();
            fail();
        } catch(flattag e) {
            assertEquals(message, e.getMessage());
            assertEquals(lineNo, e.exceptionLineNo);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        flattag.initOptions();
    }

    @Test
    public void testSimpleTag1() {
        assertExec("<tr><td>aaa</td></tr>", "tr\ttd\taaa\ntr\t\n");
        assertExec("< tr><td >aaa< /td></ tr>", "tr\ttd\taaa\ntr\t\n");
        assertExec("< tr  >< td >aaa<  /  td></tr   >", "tr\ttd\taaa\ntr\t\n");
    }

    @Test
    public void testSimpleTag2() {
        assertExec("a<br />b", "a\nbr\t\nb\n");
        assertExec("a<   br /   >b", "a\nbr\t\nb\n");
    }

    @Test
    public void testSimpleTag3() {
        flattag.lf = "\\n";
        flattag.tab = "\\t";
        assertExec("a\n\tb", "a\\n\\tb\n");
    }

    @Test
    public void testSimpleTag4() {
        assertSyntaxError("<>", 1, "invalid tag");
        assertSyntaxError("<    >", 1, "invalid tag");
        assertSyntaxError("<<='aaa'   >", 1, "invalid tag");
        assertSyntaxError("<'='aaa'   >", 1, "invalid tag");
        assertSyntaxError("<\"='aaa'   >", 1, "invalid tag");
        assertSyntaxError("< <='aaa'   >", 1, "invalid tag");
        assertSyntaxError("< '='aaa'   >", 1, "invalid tag");
        assertSyntaxError("< \"='aaa'   >", 1, "invalid tag");
        assertSyntaxError("< aaa=<   >", 1, "invalid tag");
        assertSyntaxError("< aaa='   >", 1, "invalid tag");
        assertSyntaxError("< aaa=\"   >", 1, "invalid tag");
        assertSyntaxError("<span <='aaa'   >", 1, "invalid attribute");
        assertSyntaxError("<span '='aaa'   >", 1, "invalid attribute");
        assertSyntaxError("<span \"='aaa'   >", 1, "invalid attribute");
        assertSyntaxError("<span aaa=<   >", 1, "invalid attribute");
        assertSyntaxError("<span aaa='   >", 1, "unexpected EOF");
        assertSyntaxError("<span aaa=\"   >", 1, "unexpected EOF");
        assertSyntaxError("<span aaa=aa <='aaa'   >", 1, "invalid attribute");
        assertSyntaxError("<span aaa=aa '='aaa'   >", 1, "invalid attribute");
        assertSyntaxError("<span aaa=aa \"='aaa'   >", 1, "invalid attribute");
        assertSyntaxError("<span aaa=aa aaa=<   >", 1, "invalid attribute");
        assertSyntaxError("<span aaa=aa aaa='   >", 1, "unexpected EOF");
        assertSyntaxError("<span aaa=aa aaa=\"   >", 1, "unexpected EOF");
        assertSyntaxError("<br / a>", 1, "invalid empty tag");
        assertSyntaxError("</>", 1, "invalid close tag");
        assertSyntaxError("</<>", 1, "invalid close tag");
        assertSyntaxError("</'>", 1, "invalid close tag");
        assertSyntaxError("</\">", 1, "invalid close tag");
        assertSyntaxError("</  <>", 1, "invalid close tag");
        assertSyntaxError("</  '>", 1, "invalid close tag");
        assertSyntaxError("\n\n</  \">", 3, "invalid close tag");
        assertSyntaxError("\n\n<", 3, "unexpected EOF");
    }

    @Test
    public void testDefinition() {
        assertExec("<? aaaa >", "");
        assertExec("<   ?aaaa>", "");
    }

    @Test
    public void testDoctype() {
        assertExec("<!DOCTYPE html>", "");
        assertExec("< ! ENTITY aaaa >", "");
        assertExec("<!ENTITY [ <!ENTITY aaa [ <!ENTITY aaaa> ]> ]>", "");
        assertExec("<!- DOCTYPE html>", "");
        assertExec("<! DOCTYPE\n html>", "");
    }

    @Test
    public void testComment() {
        assertExec("<!-- --- -->", "");
        assertExec("<!-- ------>", "");
        assertExec("<!-- <!-- -- > -> -->", "");
        assertExec("<!-- \n---\n -->", "");
    }

    @Test
    public void testAttribute1() {
        assertExec("<span id=aaa class=\"bbb'\" data-a='ccc \"ccc' />", "span@id=aaa@class=bbb'@data-a=ccc \"ccc\t\n");
        assertExec("<span class= data-a />", "span@class=@data-a=\t\n");
    }

    @Test
    public void testLf() {
        flattag.lf = "@@";
        assertExec(" \n ", " @@ \n");
    }

    @Test
    public void testIgnoreAttr() {
        flattag.ignoreAttr = true;
        assertExec("<span id=aaa class=\"bbb'\" data-a='ccc \"ccc' />", "span\t\n");
        assertExec("<span class= data-a />", "span\t\n");
    }

    @Test
    public void testAttrLine() {
        flattag.attrLine = true;
        assertExec("<span id=aaa class=\"bbb'\" data-a='ccc \"ccc' />", "span\t@id\taaa\nspan\t@class\tbbb'\nspan\t@data-a\tccc \"ccc\nspan\t\n");
        assertExec("<span class= data-a />", "span\t@class\t\nspan\t@data-a\t\nspan\t\n");
        assertExec("<span class= data-a></span>", "span\t@class\t\nspan\t@data-a\t\nspan\t\n");
    }

    @Test
    public void testAutoClose() {
        flattag.autoClose.add("tr");
        flattag.autoClose.add("td");
        assertExec("<tr><td>aaa<td>bbb<tr><td>ccc<td>ddd", "tr\ttd\taaa\ntr\ttd\tbbb\ntr\ttd\tccc\ntr\ttd\tddd\n");
    }

    @Test
    public void test001() {
        assertExec(
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<!DOCTYPE document [\n" +
            "   <!ENTITY aaaaa SYSTEM \"another-file.xml\">\n" +
            "]>\n" +
            "<document>\n" +
            " 765<!-- 666 -->346\n" +
            " <sentence>sentence1</sentence>\n" +
            " <sentence>sentence2</sentence>\n" +
            " &aaaaa\n" +
            "</document>",
            "  \n" +
            "document\t  765346  \n" +
            "document\tsentence\tsentence1\n" +
            "document\t  \n" +
            "document\tsentence\tsentence2\n" +
            "document\t  &aaaaa \n");
    }

    @Test
    public void test002() {
        assertExec(
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<!DOCTYPE document [\n" +
            "   <!ENTITY aaaaa SYSTEM \"another-file.xml\">\n" +
            "]>\n" +
            "<document>\n" +
            " 765<!-- 666 -->346\n" +
            " <sentence id='aaa'>sentence1</sentence>\n" +
            " <sentence>sentence2</sentence>\n" +
            " &aaaaa\n" +
            "</document>",
            "  \n" +
            "document\t  765346  \n" +
            "document\tsentence@id=aaa\tsentence1\n" +
            "document\t  \n" +
            "document\tsentence\tsentence2\n" +
            "document\t  &aaaaa \n");
    }

    @Test
    public void testOptionI() {
        assertEquals(1, flattag.parseOption("-I"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(true, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptionL() {
        assertEquals(1, flattag.parseOption("-L"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(true, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptionc() {
        assertEquals(2, flattag.parseOption("-c", "tr,td"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(2, flattag.autoClose.size());
        assertTrue(flattag.autoClose.contains("tr"));
        assertTrue(flattag.autoClose.contains("td"));
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptiond() {
        assertEquals(2, flattag.parseOption("-d", "!"));
        assertEquals('!', flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptiona() {
        assertEquals(2, flattag.parseOption("-a", "!"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals('!', flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptiona2() {
        assertEquals(2, flattag.parseOption("-a", "\t"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals('\t', flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptioni() {
        assertEquals(2, flattag.parseOption("-i", "!"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals('!', flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptionn() {
        assertEquals(2, flattag.parseOption("-n", "@"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals("@", flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptiont() {
        assertEquals(2, flattag.parseOption("-t", "@"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals("@", flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertNull(flattag.outputFileName);
    }

    @Test
    public void testOptiono() {
        assertEquals(2, flattag.parseOption("-o", "file.txt"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(false, flattag.attrLine);
        assertEquals(0, flattag.autoClose.size());
        assertEquals("file.txt", flattag.outputFileName);
    }

    @Test
    public void testOptionMany() {
        assertEquals(5, flattag.parseOption("-L", "-c", "tr,td", "-o", "file.txt", "input.xml"));
        assertEquals(flattag.DEFAULT_DELIMITER, flattag.delimiter);
        assertEquals(flattag.DEFAULT_ATTR_PREFIX, flattag.attrPrefix);
        assertEquals(flattag.DEFAULT_ATTR_INFIX, flattag.attrInfix);
        assertEquals(flattag.DEFAULT_LF, flattag.lf);
        assertEquals(flattag.DEFAULT_TAB, flattag.tab);
        assertEquals(false, flattag.ignoreAttr);
        assertEquals(true, flattag.attrLine);
        assertEquals(2, flattag.autoClose.size());
        assertTrue(flattag.autoClose.contains("tr"));
        assertTrue(flattag.autoClose.contains("td"));
        assertEquals("file.txt", flattag.outputFileName);
    }

    @Test
    public void testOptionUsage() {
        assertEquals(-1, flattag.parseOption("-l", "-a", "tr,td", "-o"));
    }

}

