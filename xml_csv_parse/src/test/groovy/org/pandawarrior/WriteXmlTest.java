package org.pandawarrior;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * Created by Simon on 7/12/16.
 */
public class WriteXmlTest {

    @Test
    public void testGetRowsForText() {
        List<String[]> rows = WriteXml.getRowsForText("\"first line\"");

        assertThat(rows, hasSize(1));

        String[] columns = rows.get(0);

        assertThat(columns.length, is(1));
        assertThat(columns[0], is("\"first line\""));
    }

    @Test
    public void testGetRowsForTextWithTwoColumns() {
        List<String[]> rows = WriteXml.getRowsForText("\"first column\",\"second column\"");

        assertThat(rows, hasSize(1));

        String[] columns = rows.get(0);

        assertThat(columns.length, is(2));
        assertThat(columns[0], is("\"first column\""));
        assertThat(columns[1], is("\"second column\""));
    }

    @Test
    public void testGetRowsForTextWithRowsContainingComma() {
        List<String[]> rows = WriteXml.getRowsForText("\"first column\",\"second column, with comma\"");

        assertThat(rows, hasSize(1));

        String[] columns = rows.get(0);

        assertThat(columns.length, is(2));
        assertThat(columns[0], is("\"first column\""));
        assertThat(columns[1], is("\"second column, with comma\""));
    }

    @Test
    public void testGetRowsForTextWithLinebreak() {
        String realText = "\"ratePopup_after_upgrade\",\"true\",\"Congratulations! The upgrade was successful. Have fun using %1$s!\n\nIf you enjoy it, please take a moment to rate it. Thanks for your support!\",\"Herzlichen Glückwunsch, das Upgrade war erfolgreich. Viel Spaß weiterhin mit %1$s!\n\nWenn dir die App gefällt, freuen wir uns über eine Bewertung im Play Store. Vielen Dank!\"\n";
        List<String[]> rows = WriteXml.getRowsForText(realText);

        assertThat(rows, hasSize(1));

        String[] columns = rows.get(0);

        assertThat(columns.length, is(4));
        assertThat(columns[3], is("\"Herzlichen Glückwunsch, das Upgrade war erfolgreich. Viel Spaß weiterhin mit %1$s!\n\nWenn dir die App gefällt, freuen wir uns über eine Bewertung im Play Store. Vielen Dank!\""));
    }
}
