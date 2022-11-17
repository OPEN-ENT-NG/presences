package fr.openent.presences.helper.init;

import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.model.*;

import java.util.List;

public interface IInitPresencesHelper {
    Init1DPresencesHelper init1DPresencesHelper = new Init1DPresencesHelper();
    Init2DPresencesHelper init2DPresencesHelper = new Init2DPresencesHelper();

    List<ReasonModel> getReasonsInit();

    List<Action> getActionsInit();

    Settings getSettingsInit();

    List<Discipline> getDisciplinesInit();

    static IInitPresencesHelper getInstance(InitTypeEnum initTypeEnum) {
        return (initTypeEnum == InitTypeEnum.ONE_D) ? init1DPresencesHelper : init2DPresencesHelper;
    }
}
