package fr.openent.massmailing.mailing;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.TemplateCode;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(VertxUnitRunner.class)
public class TemplateTest {
    private Template template;
    private static final String STRUCTURE_ID = "111";
    private static final String HALF_DAY = "11:55:00";
    private static final HashMap<TemplateCode, Object> codeValues = new HashMap<>();


    @Before
    public void setUp() {
        this.template = new Template(MailingType.MAIL, 1, STRUCTURE_ID, new JsonObject());
        setTemplateContent();
        setSystemCodes();
        setCodes();
    }

    @Test
    public void testTemplateProcess(TestContext ctx) {
        String expectedTemplate = "<div>Madame, Monsieur,</div><div></div><div>Votre enfant FOO Bar en classe de " +
                "3EME3 a été absent ce jour:</div><div>\u200B</div><div>\u200B<div><style>table{border-collapse:collapse}" +
                "table thead tr{background:#fff;box-shadow:0 4px 6px rgba(0,0,0,.12);border-bottom:none}" +
                "td{padding:5px 15px}tr{border-bottom:1px solid #ccc;background:0 0}</style><div>massmailing.summary.ABSENCE:" +
                "</div><table><thead><tr><td>massmailing.date</td><td>massmailing.hours</td><td>massmailing.reason</td>" +
                "<td>massmailing.regularized</td></tr></thead><tbody><tr><td>07/02/2022</td><td>08:55:00 - 09:50:00</td>" +
                "<td>massmailing.reasons.none</td><td>massmailing.false</td></tr></tbody></table></div></div>" +
                "<div>\u200B</div><div>Merci de bien vouloir justifier puis régulariser cette absence si " +
                "cela n'a pas été fait.</div><div>\u200B</div><div>\u200BCordialement,</div><div>\u200B</div>" +
                "<div>\u200BLa Vie Scolaire</div>";

        ctx.assertEquals(template.process(codeValues, HALF_DAY), expectedTemplate);
    }

    private void setTemplateContent() {
        this.template.setContent("<div>Madame, Monsieur,</div><div></div><div>Votre enfant [NOM ENFANT] en classe de " +
                "[CLASSE] a été absent ce jour:</div><div>\u200B</div><div>\u200B[RECAPITULATIF]</div><div>\u200B</div>" +
                "<div>Merci de bien vouloir justifier puis régulariser cette absence si cela n'a pas été fait.</div>" +
                "<div>\u200B</div><div>\u200BCordialement,</div><div>\u200B</div><div>\u200BLa Vie Scolaire</div>");
    }

    private void setCodes() {
        codeValues.put(TemplateCode.ADDRESS, "address");
        codeValues.put(TemplateCode.DATE, "04/05/2022");
        codeValues.put(TemplateCode.ZIPCODE_CITY, "10000 HERE");
        codeValues.put(TemplateCode.ABSENCE_NUMBER, "1 demi-journées");
        codeValues.put(TemplateCode.CHILD_NAME, "FOO Bar");
        codeValues.put(TemplateCode.LATENESS_NUMBER, 2);
        codeValues.put(TemplateCode.SUMMARY, new JsonObject("{\"ABSENCE\":[{\"student_id\":\"222\"," +
                "\"start_date\":\"2022-02-07T08:55:00.000\",\"end_date\":\"2022-02-07T09:50:00.000\",\"type_id\":1," +
                "\"recovery_method\":\"HOUR\",\"events\":[{\"id\":74,\"start_date\":\"2022-02-07T08:55:00\"," +
                "\"end_date\":\"2022-02-07T09:50:00\",\"comment\":\"\",\"counsellor_input\":true," +
                "\"student_id\":\"a16b8b88-26b6-436a-8ff1-420f8a42c8e7\",\"register_id\":172,\"type_id\":1," +
                "\"reason_id\":null,\"owner\":\"7c0c0975-f895-47af-a4a6-a85e93b10e47\"," +
                "\"created\":\"2022-03-15T15:08:51.453243\",\"counsellor_regularisation\":false,\"followed\":false," +
                "\"massmailed\":false,\"reason\":{\"id\":null,\"absence_compliance\":null}}]," +
                "\"display_start_date\":\"2022-02-07T08:55:00.000\"," +
                "\"display_end_date\":\"2022-02-07T09:50:00.000\"}],\"LATENESS\":null}"));
        codeValues.put(TemplateCode.CLASS_NAME, "3EME3");
        codeValues.put(TemplateCode.LEGAL_NAME, "M. FOO Jean");
    }

    private void setSystemCodes() {
        HashMap<TemplateCode, String> systemCodes = new HashMap<>();
        systemCodes.put(TemplateCode.ADDRESS, "[ADRESSE]");
        systemCodes.put(TemplateCode.DATE, "[DATE]");
        systemCodes.put(TemplateCode.ZIPCODE_CITY, "[CP - VILLE]");
        systemCodes.put(TemplateCode.ABSENCE_NUMBER, "[NOMBRE ABSENCE]");
        systemCodes.put(TemplateCode.CHILD_NAME, "[NOM ENFANT]");
        systemCodes.put(TemplateCode.LATENESS_NUMBER, "[NOMBRE RETARD]");
        systemCodes.put(TemplateCode.SUMMARY, "[RECAPITULATIF]");
        systemCodes.put(TemplateCode.CLASS_NAME, "[CLASSE]");
        systemCodes.put(TemplateCode.LEGAL_NAME, "[NOM LEGAL]");
        template.setSystemCodes(systemCodes);
    }
}
