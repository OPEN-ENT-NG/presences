import {ng} from "@presences/models/__mocks__/entcore"
import {PunishmentDetentionForm} from "@incidents/directives/punishment-form";
import * as angular from 'angular';
import 'angular-mocks';


//TODO not functional

describe('PunishmentsDetentionForm', () => {

    let punishmentDirectiveTest: any;

    beforeEach(() => {
        //Registering a module for testing
        const testApp = angular.module('app', []);
        let $compile, $rootScope;

        //Mockup test module
        angular.mock.module('app');

        //Instantiation of the services, controllers, directives we need
        PunishmentDetentionForm;

        //Adding services, controllers, directives in the module
        ng.initMockedModules(testApp);

        //Controller Injection
        angular.mock.inject(function (_$compile_, _$rootScope_) {
            // The injector unwraps the underscores (_) from around the parameter names when matching
            $compile = _$compile_;
            $rootScope = _$rootScope_;
        });

        //Controller Recovery
        var element = $compile("<punishment-detention-form></punishment-detention-form>")($rootScope);

        var controllerScope = element.scope().$$childHead;
        console.log(controllerScope);
    })

    it('test directive', done => {
        done();
    });
});
