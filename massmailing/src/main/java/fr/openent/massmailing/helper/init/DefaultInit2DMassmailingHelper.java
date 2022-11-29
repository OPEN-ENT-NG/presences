package fr.openent.massmailing.helper.init;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.model.Mailing.Template;

import java.util.ArrayList;
import java.util.List;

public class DefaultInit2DMassmailingHelper implements IInitMassmailingHelper {
    private final static Integer NB_TEMPLATES = 2;

    protected DefaultInit2DMassmailingHelper() {
    }

    @Override
    public List<Template> getTemplates() {
        List<Template> templateList = new ArrayList<>();
        for (int i = 0; i < NB_TEMPLATES; i++) {
            Template template = new Template(i)
                    .setName("massmailing.init.2d.template.mail." + i + ".name")
                    .setContent("massmailing.init.2d.template.mail." + i + ".content")
                    .setCategory("massmailing.init.2d.template.mail." + i + ".category")
                    .setType(MailingType.MAIL);
            templateList.add(template);

            template = new Template(i)
                    .setName("massmailing.init.2d.template.sms." + i + ".name")
                    .setContent("massmailing.init.2d.template.sms." + i + ".content")
                    .setCategory("massmailing.init.2d.template.sms." + i + ".category")
                    .setType(MailingType.SMS);
            templateList.add(template);
        }

        Template template = new Template(0)
                .setName("massmailing.init.2d.template.pdf.0.name")
                .setContent("massmailing.init.2d.template.pdf.0.content")
                .setCategory("massmailing.init.2d.template.pdf.0.category")
                .setType(MailingType.PDF);
        templateList.add(template);

        return templateList;
    }
}
