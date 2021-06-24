import {_, idiom as lang, Me, ng, toasts} from 'entcore';
import {ReasonService} from '@presences/services/ReasonService';
import {GroupService, SearchService, SettingsService, Template} from '../services';
import {
    IMassmailingFilterPreferences,
    MailingType,
    Massmailing,
    MassmailingAnomaliesResponse,
    MassmailingStatusResponse
} from '../model';
import {Reason} from '@presences/models/Reason';
import {MassmailingPreferenceUtils, PresencesPreferenceUtils} from '@common/utils';
import {HomeUtils} from '../utilities';
import {IPunishmentService, IPunishmentsTypeService} from "@incidents/services";
import {IPunishmentType} from "@incidents/models/PunishmentType";
import {EVENT_TYPES} from "@common/model";
import {MassmailingFilters} from "@massmailing/model/Preferences";

interface Filter {
    start_date: Date;
    end_date: Date;
    start_at: number;
    status: {
        REGULARIZED: boolean,
        UNREGULARIZED: boolean,
        NO_REASON: boolean,
        LATENESS: boolean,
        PUNISHMENT: boolean,
        SANCTION: boolean
    };
    massmailing_status: {
        mailed: boolean,
        waiting: boolean
    };
    allReasons: boolean;
    allPunishments: boolean;
    reasons: any;
    punishments: Array<IPunishmentType>;
    noReasons: boolean;
    student: any;
    students: any[];
    group: any;
    groups: any[];
    selected: {
        students: any[],
        groups: any[]
    };
    anomalies: {
        MAIL: boolean,
        SMS: boolean
    };
}

interface ViewModel {
    filter: Filter;
    formFilter: any;
    reasons: Array<Reason>;
    massmailingStatus: MassmailingStatusResponse;
    massmailingAnomalies: MassmailingAnomaliesResponse[];
    massmailingCount: {
        MAIL?: number,
        SMS?: number
    };
    errors: {
        TYPE: boolean,
        REASONS: boolean,
        STATUS: boolean
    };
    massmailing: Massmailing;
    mailingType: typeof MailingType;
    lightbox: {
        filter: boolean,
        massmailing: boolean
    };
    templates: Template[];

    punishmentsTypes: IPunishmentType[];

    fetchData(): void;

    loadData(): Promise<void>;

    switchAllReasons(): void;

    switchAllPunishmentTypes(): void;

    togglePunishmentSanctionFormFilter(punishmentType: string): void

    setSelectedPunishmentType(punishmentType: IPunishmentType): void;

    getActivatedReasonsCount(): number;

    getActivatedPunishmentTypes(): number;

    getReasonsCount(): number;

    searchStudent(value: string): void;

    selectStudent(model: any, teacher: any): void;

    selectGroup(model: any, classObject: any): void;

    searchGroup(value: string): Promise<void>;

    dropFilter(object, list): void;

    openForm(): void;

    validForm(): void;

    getKeys(object): string[];

    switchToRegularizedAbsences(): void;

    switchToUnregularizedAbsences(): void;

    switchToAbsencesWithoutReason(): void;

    filterInError(): boolean;

    getErrorMessage(): string;

    canMassmail(): boolean;

    prefetch(type: "MAIL" | "PDF" | "SMS"): Promise<void>;

    getPrefetchTitle(type: "MAIL" | "PDF" | "SMS"): string;

    massmail(): Promise<void>;

    toggleStudent(student): void;

    toggleRelative(relative, student): void;

    filterAnomalies(item: any): boolean;
}

declare let window: any;

export const homeController = ng.controller('HomeController', ['$scope', 'route', 'MassmailingService', 'ReasonService',
    'SearchService', 'GroupService', 'SettingsService', 'PunishmentService', 'PunishmentsTypeService',
    function ($scope, route, MassmailingService, reasonService: ReasonService, SearchService: SearchService,
              GroupService: GroupService, SettingsService: SettingsService, punishmentService: IPunishmentService,
              punishmentTypeService: IPunishmentsTypeService) {
        const vm: ViewModel = this;
        vm.massmailingStatus = {};
        vm.templates = [];
        vm.lightbox = {
            massmailing: false,
            filter: false
        };
        vm.errors = {
            TYPE: false,
            STATUS: false,
            REASONS: false
        };

        vm.filter = {
            start_date: new Date(),
            end_date: new Date(),
            start_at: 1,
            status: {
                REGULARIZED: false,
                UNREGULARIZED: false,
                NO_REASON: true,
                LATENESS: false,
                PUNISHMENT: false,
                SANCTION: false
            },
            massmailing_status: {
                mailed: false,
                waiting: true
            },
            allReasons: true,
            allPunishments: true,
            noReasons: true,
            reasons: {},
            punishments: [],
            students: undefined,
            student: '',
            group: '',
            groups: undefined,
            selected: {
                students: [],
                groups: []
            },
            anomalies: {
                MAIL: true,
                SMS: true
            }
        };
        vm.mailingType = MailingType;

        vm.massmailingCount = {
            MAIL: 0
        };

        vm.punishmentsTypes = [];

        vm.fetchData = function () {
            fetchMassmailingAnomalies();
            fetchMassmailingStatus();
        };

        vm.openForm = function () {
            vm.lightbox.filter = true;
            vm.formFilter = JSON.parse(JSON.stringify(vm.filter));
            vm.formFilter.selected.students.forEach((item) => item.toString = () => item.displayName);
            vm.formFilter.selected.groups.forEach((obj) => obj.toString = () => obj.name);
        };

        vm.validForm = async function () {
            vm.formFilter.punishments = vm.punishmentsTypes.filter((punishmentType: IPunishmentType) => punishmentType.isSelected);
            const {start_date, end_date} = vm.filter;
            vm.filter = {...vm.formFilter, start_date, end_date};
            let preferenceFilters: IMassmailingFilterPreferences = {
                start_at: vm.formFilter.start_at,
                status: vm.formFilter.status,
                massmailing_status: vm.formFilter.massmailing_status,
                allReasons: vm.formFilter.allReasons,
                noReasons: vm.formFilter.noReasons,
                reasons: vm.formFilter.reasons,
                punishments: vm.formFilter.punishments,
                anomalies: vm.formFilter.anomalies
            };
            await MassmailingPreferenceUtils.updatePresencesMassmailingFilter(
                HomeUtils.buildFilteredMassmailingPreference(preferenceFilters),
                window.structure.id
            );
            vm.formFilter = {};
            vm.fetchData();
            vm.lightbox.filter = false;
        };

        vm.loadData = async function (): Promise<void> {
            if (!window.structure) return;
            await loadFormFilter();
            await Promise.all([getReasons(), getPunishmentTypes()]);
            vm.fetchData();
            $scope.$apply();
        };

        const getReasons = (): Promise<void> => {
            return new Promise((resolve) => {
                reasonService.getReasons(window.structure.id)
                    .then((reasons: Reason[]) => {
                        reasons.forEach((reason: Reason) => vm.filter.reasons[reason.id] = (vm.filter.status.REGULARIZED || vm.filter.status.UNREGULARIZED));
                        vm.reasons = reasons;
                        $scope.$apply();
                        resolve(undefined);
                    });
            });
        };

        const getPunishmentTypes = (): Promise<void> => {
            return new Promise((resolve) => {
                punishmentTypeService.get(window.structure.id)
                    .then((punishmentTypes: IPunishmentType[]) => {
                        punishmentTypes.forEach((punishmentType: IPunishmentType) => punishmentType.isSelected = false);

                        const fetchedPunishmentTypePreference: Map<number, IPunishmentType> = HomeUtils.getPunishmentTypePreferenceMap(vm.filter.punishments);

                        vm.punishmentsTypes = punishmentTypes;

                        vm.punishmentsTypes.forEach((punishmentType: IPunishmentType) => {
                            if (fetchedPunishmentTypePreference.has(punishmentType.id)) {
                                punishmentType.isSelected = fetchedPunishmentTypePreference.get(punishmentType.id).isSelected;
                            }
                        });

                        $scope.$apply();
                        resolve(undefined);
                    });
            });
        };

        vm.switchAllReasons = function () {
            vm.formFilter.allReasons = !vm.formFilter.allReasons;
            vm.formFilter.noReasons = vm.formFilter.allReasons;
            for (let reason in vm.formFilter.reasons) {
                vm.formFilter.reasons[reason] = vm.formFilter.allReasons;
            }
        };

        vm.switchAllPunishmentTypes = (): void => {
            vm.formFilter.allPunishments = !vm.formFilter.allPunishments;
            vm.punishmentsTypes.forEach((punishmentType: IPunishmentType) => punishmentType.isSelected = vm.formFilter.allPunishments);
        };

        /**
         * toggle punishmentType status
         *
         * @param type        punishmentType ('PUNISHMENT' or 'SANCTION')
         */
        vm.togglePunishmentSanctionFormFilter = (type: string): void => {
            vm.formFilter.status[type] = !vm.formFilter.status[type];
            vm.punishmentsTypes
                .filter((punishmentType: IPunishmentType) => punishmentType.type === type)
                .forEach((punishmentType: IPunishmentType) => punishmentType.isSelected = vm.formFilter.status[type]);
        };

        vm.setSelectedPunishmentType = (punishmentType: IPunishmentType): void => {
            punishmentType.isSelected = !punishmentType.isSelected;

            // toggle allPunishment on what punishmentType is selected
            vm.formFilter.allPunishments = vm.getActivatedPunishmentTypes() === vm.punishmentsTypes.length;

            // toggle punishment status if all selected punishment type 'PUNISHMENT' are empty
            vm.formFilter.status.PUNISHMENT = updatePunishmentRule(EVENT_TYPES.PUNISHMENT)

            // toggle punishment status if all selected punishment type 'SANCTION' are empty
            vm.formFilter.status.SANCTION = updatePunishmentRule(EVENT_TYPES.SANCTION)
        };

        const updatePunishmentRule = (punishmentTypeRule: string): boolean => {
            let isAllSelected: boolean = false;
            vm.punishmentsTypes
                .filter(punishmentType => punishmentType.type === punishmentTypeRule)
                .forEach(punishmentType => {
                    if (punishmentType.isSelected) {
                        isAllSelected = true;
                    }
                });
            return isAllSelected;
        };


        vm.getActivatedReasonsCount = (): number => {
            let count: number = 0;
            for (let reason in vm.formFilter.reasons) {
                count += vm.formFilter.reasons[reason];
            }
            return count;
        };

        vm.getActivatedPunishmentTypes = (): number => {
            if (vm.punishmentsTypes) {
                return vm.punishmentsTypes.filter((punishmentType: IPunishmentType) => punishmentType.isSelected).length;
            } else {
                return 0;
            }
        };

        vm.getReasonsCount = (): number => {
            if (vm.formFilter.reasons) {
                return Object.keys(vm.formFilter.reasons).length;
            } else {
                return 0;
            }
        };

        // If mailed AND waiting, return null. Empty massmailed parameter = non parameter filter.
        function massmailedParameter(): boolean {
            const {waiting, mailed} = vm.filter.massmailing_status;
            if (mailed && waiting) {
                return null;
            }

            return mailed;
        }

        async function retrieveMassmailings(type: string): Promise<any> {
            const {waiting, mailed} = vm.filter.massmailing_status;
            if (!checkFilter()) {
                return type === 'Status' ? {} : [];
            }

            const reasons: Array<Number> = [];
            const types: Array<String> = [];

            const punishmentTypes: Array<Number> = vm.punishmentsTypes
                .filter((punishmentType: IPunishmentType) => punishmentType.isSelected && punishmentType.type === EVENT_TYPES.PUNISHMENT)
                .map((punishmentType: IPunishmentType) => punishmentType.id);

            const sanctionsTypes: Array<Number> = vm.punishmentsTypes
                .filter((punishmentType: IPunishmentType) => punishmentType.isSelected && punishmentType.type === EVENT_TYPES.SANCTION)
                .map((punishmentType: IPunishmentType) => punishmentType.id);

            for (let i = 0; i < vm.reasons.length; i++) if (vm.filter.reasons[vm.reasons[i].id]) reasons.push(vm.reasons[i].id);
            for (let status in vm.filter.status) if (vm.filter.status[status]) types.push(status);
            const students = [], groups = [];
            vm.filter.selected.students.forEach(({id}) => students.push(id));
            vm.filter.selected.groups.forEach(({id}) => groups.push(id));
            return await MassmailingService[`get${type}`](window.structure.id, massmailedParameter(), reasons, punishmentTypes,
                sanctionsTypes, vm.filter.start_at, vm.filter.start_date, vm.filter.end_date, groups, students, types, vm.filter.noReasons);
        }

        function fetchMassmailingStatus() {
            retrieveMassmailings('Status').then(data => {
                vm.massmailingStatus = data;
                $scope.safeApply();
            });
        }

        function fetchMassmailingAnomalies() {
            retrieveMassmailings('Anomalies').then(data => {
                vm.massmailingAnomalies = data;
                resetMassmailingCount();
                data.forEach(({bug}) => {
                    Object.keys(bug).forEach(key => {
                        vm.massmailingCount[key]++
                    });
                });
                vm.filter.anomalies = {
                    MAIL: true,
                    SMS: true
                };
                Object.keys(vm.massmailingCount).forEach(key => {
                    if (vm.massmailingCount[key] === 0) vm.filter.anomalies[key] = false;
                });
                $scope.safeApply();
            });
        }

        function resetMassmailingCount() {
            vm.massmailingCount = {
                MAIL: 0,
                SMS: 0
            }
        }

        vm.searchStudent = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.formFilter.students = await SearchService.searchUser(structureId, value, 'Student');
                $scope.safeApply();
            } catch (err) {
                vm.formFilter.students = [];
                throw err;
            }
        };

        vm.selectStudent = async function (model, student) {
            if (_.findWhere(vm.filter.selected.students, {id: student.id})) {
                return;
            }
            vm.formFilter.selected.students.push(student);
            vm.formFilter.student = '';
            vm.formFilter.students = undefined;
            $scope.safeApply();
        };

        vm.searchGroup = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.formFilter.groups = await GroupService.search(structureId, value);
                vm.formFilter.groups.map((obj) => obj.toString = () => obj.name);
                $scope.safeApply();
            } catch (err) {
                vm.formFilter.groups = [];
                throw err;
            }
            return;
        };

        vm.selectGroup = async function (model, group) {
            if (_.findWhere(vm.filter.selected.groups, {id: group.id})) {
                return;
            }
            vm.formFilter.selected.groups.push(group);
            vm.formFilter.group = '';
            vm.formFilter.groups = undefined;
            $scope.safeApply();
        };

        vm.dropFilter = function (object, list) {
            vm.formFilter.selected[list] = _.without(vm.formFilter.selected[list], object);
            $scope.safeApply();
        };

        vm.getKeys = (object) => Object.keys(object);

        vm.switchToRegularizedAbsences = (): void => {
            vm.formFilter.status.REGULARIZED = !vm.formFilter.status.REGULARIZED;
            vm.reasons.forEach((reason: Reason) => {
                vm.formFilter.reasons[reason.id] = vm.formFilter.status.REGULARIZED;
            });
        };

        vm.switchToUnregularizedAbsences = (): void => {
            vm.formFilter.status.UNREGULARIZED = !vm.formFilter.status.UNREGULARIZED;
            vm.reasons.forEach((reason: Reason) => {
                vm.formFilter.reasons[reason.id] = vm.formFilter.status.UNREGULARIZED;
            });
        };

        vm.switchToAbsencesWithoutReason = (): void => {
            vm.formFilter.status.NO_REASON = !vm.formFilter.status.NO_REASON;
            vm.formFilter.noReasons = vm.formFilter.status.UNREGULARIZED;
        };

        const checkFilter = (): boolean => {
            function allIsFalse(map): boolean {
                let bool = false;
                const keys = Object.keys(map);
                keys.forEach((key: string) => bool = bool || map[key]);

                return !bool;
            }

            const {noReasons, massmailing_status, status, reasons} = vm.filter;
            const reasonCheck: boolean = (!status.UNREGULARIZED && !status.REGULARIZED &&
                (status.LATENESS || status.NO_REASON || status.PUNISHMENT || status.SANCTION))
                ? false : (allIsFalse(reasons) && !noReasons);
            const massmailingStatusCheck: boolean = allIsFalse(massmailing_status);
            const statusCheck: boolean = allIsFalse(status);

            vm.errors = {
                REASONS: reasonCheck,
                STATUS: massmailingStatusCheck,
                TYPE: statusCheck
            };

            return !(reasonCheck || massmailingStatusCheck || statusCheck);
        };

        const loadFormFilter = async (): Promise<void> => {
            let formFiltersPreferences: MassmailingFilters = await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS.MASSMAILING_FILTER);
            let formFilters: IMassmailingFilterPreferences = formFiltersPreferences ? formFiltersPreferences[window.structure.id] : null;
            let defaultFilters: IMassmailingFilterPreferences = {
                start_at: 1,
                status: {
                    NO_REASON: true, REGULARIZED: false, UNREGULARIZED: false, LATENESS: false,
                    PUNISHMENT: false, SANCTION: false
                },
                massmailing_status: {mailed: false, waiting: true},
                allReasons: true,
                noReasons: true,
                reasons: {},
                punishments: [],
                anomalies: {MAIL: true, SMS: true}
            }
            if (formFilters) {
                Object.keys(formFilters)
                    .forEach((key: string) => {
                        if (formFilters[key] === null || formFilters[key] === undefined) delete formFilters[key]
                    });
            }
            let filter: IMassmailingFilterPreferences = {...defaultFilters, ...formFilters}
            let {...toMergeFilters} = filter;
            vm.filter = {...vm.filter, ...toMergeFilters};
            vm.filter.status = {
                REGULARIZED: filter.status["JUSTIFIED"] ? filter.status["JUSTIFIED"] : filter.status.REGULARIZED,
                UNREGULARIZED: filter.status["UNJUSTIFIED"] ? filter.status["UNJUSTIFIED"] : filter.status.UNREGULARIZED,
                LATENESS: filter.status.LATENESS,
                NO_REASON: filter.status.NO_REASON,
                PUNISHMENT: filter.status.PUNISHMENT,
                SANCTION: filter.status.SANCTION
            };
        };

        vm.filterInError = function () {
            let inError = false;
            const errorTypes = Object.keys(vm.errors);
            errorTypes.forEach((type) => inError = inError || vm.errors[type]);

            return inError;
        };

        vm.getErrorMessage = function () {
            let message = `[`;
            const types = Object.keys(vm.errors);
            types.forEach(type => {
                if (vm.errors[type]) message += `${lang.translate(`massmailing.filter.${type}`)}/`;
            });
            message = `${message.substr(0, message.length - 1)}]`;
            return message
        };

        vm.canMassmail = function (): boolean {
            for (let key in vm.massmailingStatus) {
                if (vm.massmailingStatus[key]) return true;
            }

            return false;
        };

        vm.prefetch = async function (type: "MAIL" | "PDF" | "SMS"): Promise<void> {
            try {
                if (!checkFilter()) {
                    throw 'Invalid filter';
                }

                const reasons: Array<Number> = [];

                const punishmentTypes: Array<Number> = vm.punishmentsTypes
                    .filter((punishmentType: IPunishmentType) => punishmentType.isSelected && punishmentType.type === EVENT_TYPES.PUNISHMENT)
                    .map((punishmentType: IPunishmentType) => punishmentType.id);

                const sanctionsTypes: Array<Number> = vm.punishmentsTypes
                    .filter((punishmentType: IPunishmentType) => punishmentType.isSelected && punishmentType.type === EVENT_TYPES.SANCTION)
                    .map((punishmentType: IPunishmentType) => punishmentType.id);
                const types: Array<String> = [];
                for (let i = 0; i < vm.reasons.length; i++) if (vm.filter.reasons[vm.reasons[i].id]) reasons.push(vm.reasons[i].id);
                for (let status in vm.filter.status) if (vm.filter.status[status]) types.push(status);
                const students = [], groups = [];
                vm.filter.selected.students.forEach(({id}) => students.push(id));
                vm.filter.selected.groups.forEach(({id}) => groups.push(id));
                const settings: Promise<any> = SettingsService.get(type, window.structure.id);
                const prefetch: Promise<Massmailing> = MassmailingService.prefetch(
                    type, window.structure.id, massmailedParameter(), reasons, punishmentTypes, sanctionsTypes,
                    vm.filter.start_at, vm.filter.start_date, vm.filter.end_date, groups, students, types, vm.filter.noReasons
                );
                vm.lightbox.massmailing = true;
                const data = await Promise.all([settings, prefetch]);
                vm.massmailing = data[1];
                vm.templates = data[0];
                vm.massmailing.filter = vm.filter;
                if (vm.templates.length > 0) vm.massmailing.template = vm.templates[0];
            } catch (e) {
                vm.lightbox.massmailing = false;
                toasts.warning('massmailing.prefetch.error');
                throw e;
            } finally {
                $scope.safeApply();
            }
        };

        vm.getPrefetchTitle = (type: "MAIL" | "PDF" | "SMS"): string => {
            switch (type) {
                case vm.mailingType[vm.mailingType.PDF]:
                    return lang.translate("massmailing.prefetch.title.PDF");
                case vm.mailingType[vm.mailingType.SMS]:
                    return lang.translate("massmailing.prefetch.title.SMS");
                case vm.mailingType[vm.mailingType.MAIL]:
                    return lang.translate("massmailing.prefetch.title.MAIL");
                default:
                    return "";
            }
        };

        vm.massmail = async function () {
            vm.lightbox.massmailing = false;
            await vm.massmailing.process();
            toasts.info('massmailing.in-progress');
        };

        vm.toggleStudent = function ({selected, relative}) {
            relative.forEach(rel => rel.selected = selected);
            $scope.safeApply();
        };

        vm.toggleRelative = function (relative, student) {
            let bool = false;
            student.relative.forEach(relative => (bool = bool || relative.selected));
            student.selected = bool;
            $scope.safeApply();
        };

        vm.filterAnomalies = item => {
            let keys = Object.keys(vm.filter.anomalies);
            for (let i = 0; i < keys.length; i++) {
                let key = keys[i];
                if (vm.filter.anomalies[key] && key in item.bug && item.bug[key]) return true;
            }

            return false;
        };

        $scope.$watch(() => window.structure, vm.loadData);
    }]);
