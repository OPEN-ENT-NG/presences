package fr.openent.massmailing.helper.init;

import fr.openent.massmailing.model.Mailing.Template;
import fr.openent.presences.enums.InitTypeEnum;

import java.util.List;

public interface IInitMassmailingHelper {
    DefaultInit1DMassmailingHelper defaultInit1DMassmailingHelper = new DefaultInit1DMassmailingHelper();
    DefaultInit2DMassmailingHelper defaultInit2DMassmailingHelper = new DefaultInit2DMassmailingHelper();

    //Must be changed if implementing a different function per platform
    static IInitMassmailingHelper getInstance(InitTypeEnum initTypeEnum, String ignoredPlateform) {
        return getDefaultInstance(initTypeEnum);
    }

    static IInitMassmailingHelper getDefaultInstance(InitTypeEnum initTypeEnum) {
        return (initTypeEnum == InitTypeEnum.ONE_D) ? defaultInit1DMassmailingHelper : defaultInit2DMassmailingHelper;
    }

    List<Template> getTemplates();
}
