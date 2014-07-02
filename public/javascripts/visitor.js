angular.module('visitorApp', ['ngRoute'])

.config(function($routeProvider) {
  $routeProvider
    .when('/', {
      controller:'ListCtrl',
      templateUrl:'/assets/partials/welcome.html'
    })
    .when('/addstation', {
      controller:'AddStationCtrl',
      templateUrl:'/assets/partials/addstation.html'
    })
    .when('/read/:id', {
      controller:'ReadCtrl',
      templateUrl:'/assets/partials/read.html'
    })
    .when('/chunk/:id', {
      controller:'ChunkCtrl',
      templateUrl:'/assets/partials/chunk.html'
    })
    .otherwise({
      redirectTo:'/'
    });
})
 
.controller('ListCtrl', function($scope, $http) {
  	$scope.stations = [];
  	
  	/* get stations from server*/
    $http.get("/stations")
    .success(function (data) {
    	$scope.stations = data;
  	});  
  	
})

.controller('ReadCtrl', function($scope, $http, $routeParams, $sce) {  	
  	/* get station from server*/
    $http.get("/station/" + $routeParams.id)
    .success(function (data) {
    	$scope.station = data;
    	$scope.streamUrl = $sce.trustAsResourceUrl(data.url);
  	});  

  	$scope.msgs = [];
 		/** handle incoming messages: add to messages array */
 		$scope.addMsg = function (msg) {
 		console.log(msg.data);
 		var chunk = JSON.parse(msg.data);
  		$scope.$apply(function () { $scope.msgs.push(chunk); });
  	};
 	
 		/** start listening on messages from selected room */
    $scope.listen = function () {
   		$scope.chatFeed = new EventSource("chatfeed/" + $routeParams.id);
  		$scope.chatFeed.addEventListener("message", $scope.addMsg, false);
 		};

 		$scope.listen();
})

.controller('ChunkCtrl', function($scope, $http, $routeParams, $sce) {  	
  	/* get chunk from server*/
    $http.get("/chunk/" + $routeParams.id)
    .success(function (data) {
    	$scope.chunk = data;
    	$scope.streamUrl = $sce.trustAsResourceUrl("/chunkstream/" + $routeParams.id);
    	console.log(data.location);
    	var stationId = data.stationId.$oid;
    	
  		/* get chunk from server*/
    	$http.get("/station/" + stationId)
    	.success(function (data) {
    		$scope.station = data;
  		});  
    	
  	});  
})

.controller('AddStationCtrl', function($scope, $location, $http) {
	
    $scope.submitMyForm = function(){
        /* while compiling form , angular created this object*/
        var data=$scope.fields;  
        /* post to server*/
        var url = "/savestation"
        $http.post(url, data)
        .success(function (data) {
    		$location.path('/');
  		});        
    }
	
});