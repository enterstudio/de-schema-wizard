(function () {

    var schemaWizardApp = angular.module('schemaWizardApp');

    schemaWizardApp.controller('schemaDetailsCtrl',
        function($rootScope, $scope, $resource, $location, $route, $routeParams, schemaData,$confirm, statusCodesFactory) {

            schemaData.$promise.then(function () {
                    $rootScope.$broadcast("setCurrentSchema", {
                        schema: schemaData
                    })
                }, function (error) {
                    console.log(error);
                    statusCodesFactory.get().$promise.then(function (response) {
                        $confirm(
                            {
                                title: response.gettingSchemaDataFailed.title,
                                text: response.gettingSchemaDataFailed.message +
                                " (" + error.status + ")",
                                ok: 'OK'
                            },
                            {templateUrl: 'schema-wizard/schema-wizard.confirm.template.html'})
                    })
                }
            );
        }); // schemaDetailsCtrl
})();
