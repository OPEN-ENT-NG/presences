import {ng} from "@presences/models/__mocks__/entcore"
import * as angular from 'angular';
import * as angularMock from 'angular-mocks/angular-mocks';
import {GroupingService, GroupService, SearchService} from "@common/services";
import {ReasonService} from "@presences/services";
import {MassmailingService, settingsService} from "@massmailing/services";
import {punishmentService, punishmentsTypeService} from "@incidents/services";
import {homeController} from "@massmailing/controllers";
import {IPunishmentType} from "@incidents/models/PunishmentType";
import {Reason} from '@presences/models/Reason';
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";
import {GroupsSearch, MassmailingPreferenceUtils, StudentsSearch} from "@common/utils";
import {IMassmailingFilterPreferences} from "@massmailing/model";

describe('HomeControllers', () => {
    let homeControllerTest: any;

    beforeEach(() => {
        //Registering a module for testing
        const testApp = angular.module('app', []);
        let $controller, $rootScope;

        //Mockup test module
        angularMock.module('app');

        //Instantiation of the services, controllers, directives we need
        homeController;

        //Adding services, controllers, directives in the module
        ng.initMockedModules(testApp);

        //Controller Injection
        angularMock.inject(function (_$controller_, _$rootScope_) {
            // The injector unwraps the underscores (_) from around the parameter names when matching
            $controller = _$controller_;
            $rootScope = _$rootScope_;
        });

        //Creates a new instance of scope
        let $scope = $rootScope.$new();

        //Fetching $location
        testApp.run(function ($rootScope, $location) {
            $rootScope.location = $location;
        });

        //Controller Recovery
        homeControllerTest = $controller('HomeController', {
            $scope: $scope,
            route: undefined,
            MassmailingService: MassmailingService,
            reasonService: ReasonService,
            searchService: SearchService,
            groupService: GroupService,
            groupingService: GroupingService,
            SettingsService: settingsService,
            punishmentService: punishmentService,
            punishmentTypeService: punishmentsTypeService,
        });

        MassmailingPreferenceUtils.updatePresencesMassmailingFilter = jest.fn((filter: IMassmailingFilterPreferences, structureId: string): void => {});
    })

    it('test of the proper functioning of the openForm method', done => {
        homeControllerTest.lightbox.filter = false
        homeControllerTest.filter.studentsSearch = new StudentsSearch("null", homeControllerTest.searchService);
        homeControllerTest.filter.groupsSearch = new GroupsSearch("null", homeControllerTest.searchService,
            homeControllerTest.groupService, homeControllerTest.groupingService);
        homeControllerTest.filter.studentsSearch._selectedStudents = [{displayName: "students1"}, {displayName: "students2"}];
        homeControllerTest.filter.groupsSearch._selectedGroups = [{name: "name1"}, {name: "name2"}];
        homeControllerTest.openForm();
        expect(homeControllerTest.lightbox.filter).toEqual(true);
        expect(homeControllerTest.formFilter.studentsSearch.getSelectedStudents()[0].displayName).toEqual("students1");
        expect(homeControllerTest.formFilter.studentsSearch.getSelectedStudents()[1].displayName).toEqual("students2");
        expect(homeControllerTest.formFilter.groupsSearch.getSelectedGroups()[0].name).toEqual("name1");
        expect(homeControllerTest.formFilter.groupsSearch.getSelectedGroups()[1].name).toEqual("name2");
        done();
    });

    it('test of the proper functioning of the validForm method', done => {
        const punishment1: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment1",
            punishment_category_id: 0,
            structure_id: "",
            type: "",
            isSelected: false,
        };
        const punishment2: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment2",
            punishment_category_id: 0,
            structure_id: "",
            type: "",
            isSelected: true,
        };
        homeControllerTest.punishmentsTypes = [punishment1, punishment2];
        homeControllerTest.lightbox.filter = true
        homeControllerTest.formFilter = {};
        homeControllerTest.fetchData = jest.fn((): void => {});
        homeControllerTest.validForm().then(() => {
            expect(homeControllerTest.lightbox.filter).toEqual(false);
            done();
        });
    });

    it('test of the proper functioning of the switchAllAbsenceReasons method', done => {
        homeControllerTest.formFilter = {
            allAbsenceReasons: false,
            allLatenessReasons: false,
            noReasons: false,
            noLatenessReasons: false,
            reasons: {
                "1": false,
                "2": false
            }
        }
        const reason1: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 1,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        };
        const reason2: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 2,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        };
        homeControllerTest.reasons = [reason1, reason2];

        homeControllerTest.switchAllAbsenceReasons();

        expect(homeControllerTest.formFilter.allAbsenceReasons).toEqual(true);
        expect(homeControllerTest.formFilter.allLatenessReasons).toEqual(false);
        expect(homeControllerTest.formFilter.noReasons).toEqual(true);
        expect(homeControllerTest.formFilter.noLatenessReasons).toEqual(false);
        expect(homeControllerTest.formFilter.reasons["1"]).toEqual(true);
        expect(homeControllerTest.formFilter.reasons["2"]).toEqual(false);

        homeControllerTest.switchAllAbsenceReasons();

        expect(homeControllerTest.formFilter.allAbsenceReasons).toEqual(false);
        expect(homeControllerTest.formFilter.allLatenessReasons).toEqual(false);
        expect(homeControllerTest.formFilter.noReasons).toEqual(false);
        expect(homeControllerTest.formFilter.noLatenessReasons).toEqual(false);
        expect(homeControllerTest.formFilter.reasons["1"]).toEqual(false);
        expect(homeControllerTest.formFilter.reasons["2"]).toEqual(false);

        homeControllerTest.switchAllLatenessReasons();

        expect(homeControllerTest.formFilter.allAbsenceReasons).toEqual(false);
        expect(homeControllerTest.formFilter.allLatenessReasons).toEqual(true);
        expect(homeControllerTest.formFilter.noReasons).toEqual(false);
        expect(homeControllerTest.formFilter.noLatenessReasons).toEqual(true);
        expect(homeControllerTest.formFilter.reasons["1"]).toEqual(false);
        expect(homeControllerTest.formFilter.reasons["2"]).toEqual(true);

        homeControllerTest.switchAllLatenessReasons();

        expect(homeControllerTest.formFilter.allAbsenceReasons).toEqual(false);
        expect(homeControllerTest.formFilter.allLatenessReasons).toEqual(false);
        expect(homeControllerTest.formFilter.noReasons).toEqual(false);
        expect(homeControllerTest.formFilter.noLatenessReasons).toEqual(false);
        expect(homeControllerTest.formFilter.reasons["1"]).toEqual(false);
        expect(homeControllerTest.formFilter.reasons["2"]).toEqual(false);

        done();
    });

    it('test of the proper functioning of the switchAllPunishmentTypes method', done => {
        homeControllerTest.formFilter = {
            allPunishments: false,
        }
        const punishment1: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment1",
            punishment_category_id: 0,
            structure_id: "",
            type: "",
            isSelected: false,
        };
        const punishment2: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment2",
            punishment_category_id: 0,
            structure_id: "",
            type: "",
            isSelected: true,
        };
        homeControllerTest.punishmentsTypes = [punishment1, punishment2];

        homeControllerTest.switchAllPunishmentTypes()

        expect(homeControllerTest.formFilter.allPunishments).toEqual(true);
        expect(homeControllerTest.punishmentsTypes[0].isSelected).toEqual(true);
        expect(homeControllerTest.punishmentsTypes[1].isSelected).toEqual(true);

        homeControllerTest.switchAllPunishmentTypes()

        expect(homeControllerTest.formFilter.allPunishments).toEqual(false);
        expect(homeControllerTest.punishmentsTypes[0].isSelected).toEqual(false);
        expect(homeControllerTest.punishmentsTypes[1].isSelected).toEqual(false);

        done();
    });

    it('test of the proper functioning of the togglePunishmentSanctionFormFilter method', done => {
        homeControllerTest.formFilter = {
            status: {
                "type1": false,
                "type2": false
            }
        }
        const punishment1: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment1",
            punishment_category_id: 0,
            structure_id: "",
            type: "type1",
            isSelected: false,
        };
        const punishment2: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment2",
            punishment_category_id: 0,
            structure_id: "",
            type: "type2",
            isSelected: false,
        };
        const punishment3: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment1",
            punishment_category_id: 0,
            structure_id: "",
            type: "type1",
            isSelected: false,
        };
        homeControllerTest.punishmentsTypes = [punishment1, punishment2, punishment3];

        homeControllerTest.togglePunishmentSanctionFormFilter("type1");

        expect(punishment1.isSelected).toEqual(true);
        expect(punishment2.isSelected).toEqual(false);
        expect(punishment3.isSelected).toEqual(true);
        expect(homeControllerTest.formFilter.status["type1"]).toEqual(true);
        expect(homeControllerTest.formFilter.status["type2"]).toEqual(false);

        homeControllerTest.togglePunishmentSanctionFormFilter("type2");

        expect(punishment1.isSelected).toEqual(true);
        expect(punishment2.isSelected).toEqual(true);
        expect(punishment3.isSelected).toEqual(true);
        expect(homeControllerTest.formFilter.status["type1"]).toEqual(true);
        expect(homeControllerTest.formFilter.status["type2"]).toEqual(true);

        homeControllerTest.togglePunishmentSanctionFormFilter("type1");

        expect(punishment1.isSelected).toEqual(false);
        expect(punishment2.isSelected).toEqual(true);
        expect(punishment3.isSelected).toEqual(false);
        expect(homeControllerTest.formFilter.status["type1"]).toEqual(false);
        expect(homeControllerTest.formFilter.status["type2"]).toEqual(true);

        done();
    });

    it('test of the proper functioning of the setSelectedPunishmentType method', done => {
        const punishment: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment1",
            punishment_category_id: 0,
            structure_id: "",
            type: "type1",
            isSelected: false,
        };
        homeControllerTest.formFilter = {
            allPunishments: undefined,
            status: {
                PUNISHMENT: undefined,
                SANCTION: undefined,
            },
        }

        homeControllerTest.setSelectedPunishmentType(punishment);

        expect(punishment.isSelected).toEqual(true);
        done();
    });

    it('test of the proper functioning of the getActivatedAbsenceCount and getActivatedLatenessCount method', done => {
        homeControllerTest.formFilter = {
            reasons: {
                "1": true,
                "2": true,
                "3": false,
                "4": false,
            }
        }

        const reason1: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 1,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        };
        const reason2: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 2,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        };
        const reason3: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 3,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        };
        const reason4: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 4,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        };

        homeControllerTest.reasons = [reason1, reason2, reason3, reason4];

        expect(homeControllerTest.getActivatedAbsenceCount()).toEqual(1);
        expect(homeControllerTest.getActivatedLatenessCount()).toEqual(1);
        done();
    });

    it('test of the proper functioning of the getActivatedPunishmentTypes method', done => {
        const punishment1: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment1",
            punishment_category_id: 0,
            structure_id: "",
            type: "type1",
            isSelected: true,
        };
        const punishment2: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment2",
            punishment_category_id: 0,
            structure_id: "",
            type: "type2",
            isSelected: false,
        };
        const punishment3: IPunishmentType = {
            hidden: false,
            id: 0,
            label: "punishment1",
            punishment_category_id: 0,
            structure_id: "",
            type: "type1",
            isSelected: true,
        };
        homeControllerTest.punishmentsTypes = [punishment1, punishment2, punishment3];
        expect(2).toEqual(homeControllerTest.getActivatedPunishmentTypes());

        homeControllerTest.punishmentsTypes = undefined;
        expect(0).toEqual(homeControllerTest.getActivatedPunishmentTypes());


        done();
    });

    it('test of the proper functioning of the getAbsenceCount and getLatenessCount method', done => {
        const reason1: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 1,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        };
        const reason2: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 2,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        };
        const reason3: Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 3,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        };

        homeControllerTest.reasons = [reason1, reason2, reason3];

        expect(2).toEqual(homeControllerTest.getAbsenceCount());

        expect(1).toEqual(homeControllerTest.getLatenessCount());

        done();
    });

    it('test of the proper functioning of the switchToRegularizedAbsences method', done => {
        homeControllerTest.formFilter = {
            reasons: {
                1: true,
                2: false,
                3: false,
            },
            status: {
                REGULARIZED: false,
            }
        }

        const reason1 : Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 1,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        }

        const reason2 : Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 2,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        }

        const reason3 : Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 3,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        }

        homeControllerTest.reasons = [reason1, reason2, reason3]

        homeControllerTest.switchToRegularizedAbsences()
        expect(homeControllerTest.formFilter.status.REGULARIZED).toEqual(true);
        expect(homeControllerTest.formFilter.reasons[1]).toEqual(true);
        expect(homeControllerTest.formFilter.reasons[2]).toEqual(true);
        expect(homeControllerTest.formFilter.reasons[3]).toEqual(false);

        homeControllerTest.switchToRegularizedAbsences()
        expect(homeControllerTest.formFilter.status.REGULARIZED).toEqual(false);
        expect(homeControllerTest.formFilter.reasons[1]).toEqual(false);
        expect(homeControllerTest.formFilter.reasons[2]).toEqual(false);
        expect(homeControllerTest.formFilter.reasons[3]).toEqual(false);

        done();
    });

    it('test of the proper functioning of the switchToUnregularizedAbsences method', done => {
        homeControllerTest.formFilter = {
            reasons: {
                1: true,
                2: false,
                3: false,
            },
            status: {
                UNREGULARIZED: false,
            }
        }

        const reason1 : Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 1,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        }

        const reason2 : Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 2,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.ABSENCE
        }

        const reason3 : Reason = {
            absence_compliance: false,
            comment: "",
            default: false,
            group: false,
            hidden: false,
            id: 3,
            isSelected: false,
            label: "",
            proving: false,
            structure_id: "",
            reason_type_id: REASON_TYPE_ID.LATENESS
        }

        homeControllerTest.reasons = [reason1, reason2, reason3]

        homeControllerTest.switchToUnregularizedAbsences()
        expect(homeControllerTest.formFilter.status.UNREGULARIZED).toEqual(true);
        expect(homeControllerTest.formFilter.reasons[1]).toEqual(true);
        expect(homeControllerTest.formFilter.reasons[2]).toEqual(true);
        expect(homeControllerTest.formFilter.reasons[3]).toEqual(false);

        homeControllerTest.switchToUnregularizedAbsences()
        expect(homeControllerTest.formFilter.status.UNREGULARIZED).toEqual(false);
        expect(homeControllerTest.formFilter.reasons[1]).toEqual(false);
        expect(homeControllerTest.formFilter.reasons[2]).toEqual(false);
        expect(homeControllerTest.formFilter.reasons[3]).toEqual(false);

        done();
    });

    it('test of the proper functioning of the switchToAbsencesWithoutReason method', done => {
        homeControllerTest.formFilter = {
            status: {
                NO_REASON: false,
                UNREGULARIZED: true
            }
        }

        homeControllerTest.switchToAbsencesWithoutReason()
        expect(homeControllerTest.formFilter.status.NO_REASON).toEqual(true);
        expect(homeControllerTest.formFilter.noAbsenceReasons).toEqual(true);

        homeControllerTest.switchToAbsencesWithoutReason()
        expect(homeControllerTest.formFilter.status.NO_REASON).toEqual(false);
        expect(homeControllerTest.formFilter.noAbsenceReasons).toEqual(true);

        done();
    });

    it('test of the proper functioning of the filterInError method', done => {
        homeControllerTest.errors = {}

        expect(homeControllerTest.filterInError()).toEqual(false);
        homeControllerTest.errors = {
            REASONS: false, STATUS: false, TYPE: false,
        }
        expect(homeControllerTest.filterInError()).toEqual(false);
        homeControllerTest.errors = {
            REASONS: true, STATUS: false, TYPE: false,
        }
        expect(homeControllerTest.filterInError()).toEqual(true);
        homeControllerTest.errors = {
            REASONS: false, STATUS: true, TYPE: false,
        }
        expect(homeControllerTest.filterInError()).toEqual(true);
        homeControllerTest.errors = {
            REASONS: false, STATUS: false, TYPE: true,
        }
        expect(homeControllerTest.filterInError()).toEqual(true);

        done();
    });

    it('test of the proper functioning of the getErrorMessage method', done => {
        homeControllerTest.errors = {
            TYPE: false, REASONS: false, STATUS: false
        }
        expect(homeControllerTest.getErrorMessage()).toEqual("]");

        homeControllerTest.errors = {
            TYPE: true, REASONS: false, STATUS: false
        }
        expect(homeControllerTest.getErrorMessage()).toEqual("[massmailing.filter.TYPE]");

        homeControllerTest.errors = {
            TYPE: true, REASONS: true, STATUS: true
        }
        expect(homeControllerTest.getErrorMessage()).toEqual("[massmailing.filter.TYPE/massmailing.filter.REASONS/massmailing.filter.STATUS]");
        done();
    });

    it('test of the proper functioning of the canMassmail method', done => {
        homeControllerTest.massmailingStatus = {
            "massmailing1": false,
            "massmailing2": false,
        }
        expect(homeControllerTest.canMassmail()).toEqual(false);

        homeControllerTest.massmailingStatus = {
            "massmailing1": true,
            "massmailing2": false,
        }
        expect(homeControllerTest.canMassmail()).toEqual(true);

        homeControllerTest.massmailingStatus = {
            "massmailing1": false,
            "massmailing2": true,
        }
        expect(homeControllerTest.canMassmail()).toEqual(true);

        homeControllerTest.massmailingStatus = {
            "massmailing1": true,
            "massmailing2": true,
        }
        expect(homeControllerTest.canMassmail()).toEqual(true);
        done();
    });

    it('test of the proper functioning of the getPrefetchTitle method', done => {
        expect(homeControllerTest.getPrefetchTitle("SMS")).toEqual("massmailing.prefetch.title.SMS");
        expect(homeControllerTest.getPrefetchTitle("PDF")).toEqual("massmailing.prefetch.title.PDF");
        expect(homeControllerTest.getPrefetchTitle("MAIL")).toEqual("massmailing.prefetch.title.MAIL");
        expect(homeControllerTest.getPrefetchTitle("")).toEqual("");
        done();
    });
});
