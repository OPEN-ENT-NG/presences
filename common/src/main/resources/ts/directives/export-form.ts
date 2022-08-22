import {Me, ng} from 'entcore';
import {EXPORT_TYPE, ExportType} from "@common/core/enum/export-type.enum";
import {IScope} from "angular";
import {PresencesPreferenceUtils} from "@common/utils";

declare let window: any;

interface IViewModel {
    displayPdf: boolean;
    displayCsv: boolean;
    isDisabled: boolean;
    lightboxPreference: string;

    displayLightBox: boolean;
    hideLightboxSetting: boolean;

    exportTypes: typeof EXPORT_TYPE;
    exportType: ExportType;

    $onInit(): Promise<void>;

    openExportForm(exportType: ExportType): void;

    submitForm(): Promise<void>;
}

class Controller implements ng.IController, IViewModel {

    /**
     * Display PDF export button
     */
    displayPdf: boolean;

    /**
     * Display CSV export button
     */
    displayCsv: boolean;

    /**
     * Disable buttons
     */
    isDisabled: boolean;

    /**
     * Preference key for lightbox display
     */
    lightboxPreference: string;

    displayLightBox: boolean;
    hideLightboxSetting: boolean;


    exportTypes: typeof EXPORT_TYPE;

    exportType: ExportType;


    constructor(private $scope: IScope) {
        this.displayLightBox = false;
        this.hideLightboxSetting = false;
        this.exportTypes = EXPORT_TYPE;
    }

    $onInit = async (): Promise<void> => {
        try {
            let preference: boolean = await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS[this.lightboxPreference]);
            preference = preference ? preference[window.structure.id] : null;
            this.hideLightboxSetting = (preference !== undefined) ? !preference : false;
        } catch (e) {
            this.hideLightboxSetting = false;
        }
    }

    openExportForm = (exportType: ExportType): void => {
        this.exportType = exportType;
        if (this.hideLightboxSetting) {
            this.submitForm();
        } else {
            this.displayLightBox = true;
        }
    }

    submitForm = async (): Promise<void> => {
        this.displayLightBox = false;
        if (this.hideLightboxSetting) {
            try {
                await PresencesPreferenceUtils.savePreferenceBoolean(PresencesPreferenceUtils.PREFERENCE_KEYS[this.lightboxPreference],
                    !this.hideLightboxSetting, window.structure.id);
            } catch (e) {
                throw e;
            }
        }
        this.$scope.$parent.$eval(this.$scope['vm']['onSubmit'])(this.exportType);
    }
}

function directive() {
    return {
        restrict: 'E',
        template: `
         <div class="cell export-form-buttons">
            <button ng-if="vm.displayPdf"
                    ng-disabled="vm.isDisabled"
                    class="right-magnet"
                    data-ng-click="vm.openExportForm(vm.exportTypes.PDF)">
                <i18n>presences.export.topdf</i18n>
            </button>
            <button ng-if="vm.displayCsv"
                    ng-disabled="vm.isDisabled"
                    class="right-magnet"
                    data-ng-click="vm.openExportForm(vm.exportTypes.CSV)">
                <i18n>presences.export.tocsv</i18n>
            </button>
        </div>

        <lightbox class="export-form-lightbox" show="vm.displayLightBox" on-close="vm.displayLightBox = false">
            <section class="head export-form-lightbox-head">
                <h3>
                    <i18n>presences.filters</i18n>
                </h3>
            </section>
            <section class="body lightbox-form export-form-lightbox-content">
                <div class="row vertical-spacing">
                    <i18n>presences.export.lightbox.warning.1</i18n>
                    <b><i18n>presences.export.lightbox.warning.2</i18n></b>
                    <a href="/workspace/workspace" target="_blank">
                        <b><u><i18n>presences.export.lightbox.warning.3</i18n></u></b>
                    </a>
                    <b><i18n>presences.export.lightbox.warning.4</i18n></b>
                    <i18n>presences.export.lightbox.warning.5</i18n>
                    <i18n>presences.export.lightbox.warning.6</i18n>
                    <b><i18n>presences.export.lightbox.warning.7</i18n></b>
                    <i18n>presences.export.lightbox.warning.8</i18n>
                </div>

                <div class="row vertical-spacing">
                    <label class="checkbox">
                        <input type="checkbox" data-ng-model="vm.hideLightboxSetting"/>
                        <span>
                            <i18n>presences.do.not.show.this.message.again</i18n>
                        </span>
                    </label>
                </div>
            </section>

            <!-- Form submit/cancel buttons -->
            <section>
                <div class="row horizontal-spacing">
                    <div class="twelve cell">
                        <button class="right-magnet" data-ng-click="vm.submitForm()">
                            <i18n>presences.exemptions.form.submit</i18n>
                        </button>
                        <button class="right-magnet cancel" data-ng-click="vm.displayLightBox = false">
                            <i18n>presences.exemptions.form.cancel</i18n>
                        </button>
                    </div>
                </div>
            </section>
        </lightbox>
        `,
        scope: {
            displayPdf: '=',
            displayCsv: '=',
            isDisabled: '=',
            lightboxPreference: '=',
            onSubmit: '&'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', Controller]
    };
}

export const ExportForm = ng.directive('exportForm', directive);