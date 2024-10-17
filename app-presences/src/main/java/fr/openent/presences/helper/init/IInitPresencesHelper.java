package fr.openent.presences.helper.init;

import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.model.*;

import java.util.List;

public interface IInitPresencesHelper {
    DefaultInit1DPresencesHelper defaultInit1DPresencesHelper = new DefaultInit1DPresencesHelper();
    DefaultInit2DPresencesHelper defaultInit2DPresencesHelper = new DefaultInit2DPresencesHelper();

    List<ReasonModel> getReasonsInit();

    List<Action> getActionsInit();

    Settings getSettingsInit();

    List<Discipline> getDisciplinesInit();

    //Must be changed if implementing a different function per platform
    static IInitPresencesHelper getInstance(InitTypeEnum initTypeEnum, String ignoredPlatform) {
        return getDefaultInstance(initTypeEnum);
    }

    static IInitPresencesHelper getDefaultInstance(InitTypeEnum initTypeEnum) {
        return (initTypeEnum == InitTypeEnum.ONE_D) ? defaultInit1DPresencesHelper : defaultInit2DPresencesHelper;
    }
}
