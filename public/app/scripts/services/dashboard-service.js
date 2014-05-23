/**
 * Represents all the shared state that creates a dashboard and interaction
 * between the individual charts.
 */
bridge.service('dashboardService', ['$filter', function($filter) {

    function redrawAll(me, initial) {
        if (this.blockRedraw || initial) return;
        this.blockRedraw = true;
        var range = me.xAxisRange();
        this.trackers.forEach(function(tracker) {
            if (!tracker.hidden && tracker.graph && tracker.graph !== me) {
                tracker.graph.updateOptions({ dateWindow: range });
            }
        });
        this.dateWindowGraph.updateOptions({ dateWindow: range });
        this.blockRedraw = false;
    }
    
    function DashboardService() {
        this.dateWindowGraph = null;
        this.trackers = [];
        this.blockRedraw = false;
        this.xAxisOffset = 40;
        this.dateWindow = [new Date().getTime() - (14*24*60*60*1000), new Date().getTime()];
        this.dateFormatter = function(number, granularity, opts, dygraph) {
            return $filter('date')(new Date(number), 'M/d');
        };
    }
    DashboardService.prototype = {
        options: function(target) {
            var opts = {
                highlightSeriesOpts: { strokeWidth: 2 },
                interactionModel: Dygraph.Interaction.dragIsPanInteractionModel,
                showLabelsOnHighlight: false,
                legend: 'never',
                showRoller: false,
                rollPeriod: 0,
                showRangeSelector: false,
                drawCallback: redrawAll.bind(this),
                dateWindow: this.dateWindow,
                xRangePad: 0,
                errorBars: false, 
                // This might vary; there's buggy behavior when the charts don't have data across the same timeframes
                panEdgeFraction: 1.0 
            };          
            for (var prop in opts) {
                if (typeof target[prop] === "undefined") {
                    target[prop] = opts[prop];
                }
            }
            return target;
        },
        createPayload: function(form, dateFields, fields) {
            var payload = {
                startDate: form[dateFields[0]].$modelValue.getTime(),
                endDate: form[dateFields[1]].$modelValue.getTime(),
                data: {}
            };
            fields.forEach(function(field) {
                payload.data[field] = form[field].$modelValue;
            });
            return payload;
        }
    };
    return new DashboardService();
}]);