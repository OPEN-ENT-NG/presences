package fr.openent.massmailing.helper.init;

import fr.openent.massmailing.model.Mailing.Template;
import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.model.Settings;

import java.util.List;

public interface IInitMassmailingHelper {
    Init1DMassmailingHelper init1DMassmailingHelper = new Init1DMassmailingHelper();
    Init2DMassmailingHelper init2DMassmailingHelper = new Init2DMassmailingHelper();

    static IInitMassmailingHelper getInstance(InitTypeEnum initTypeEnum) {
        return (initTypeEnum == InitTypeEnum.ONE_D) ? init1DMassmailingHelper : init2DMassmailingHelper;
    }

    List<Template> getTemplates();
}
