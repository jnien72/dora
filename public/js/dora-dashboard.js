var queryHotStatus;
var dsHotStatus;
var etlHotStatus;
var queryStatusResponse = null
var dataSourceStatusResponse = null
var etlStatusResponse = null

sectionMap["dashboard"] = (function () {
    return {
        setup: function setup() {
        }
        ,
        beforeShowSection: function beforeShowSection() {
        }
        ,
        showSection: function showSection() {
            dashboardReload()
        }
        ,
        cancelQuery: function cancelQuery(jobId) {
            if (jobId !== undefined) {
                Modal.confirm("Proceed cancelling query '" + jobId + "'?", "Query cancellation", function () {
                    $.ajax({
                        type: "GET",
                        url: "/dashboardQueryCancel?id=" + jobId
                    }).success(function () {
                        dashboardReload()
                    })
                })
            }
        }
        ,
        cancelDs: function cancelDs(jobId) {
            if (jobId !== undefined) {
                Modal.confirm("Proceed cancelling data source import '" + jobId + "'?", "DS Import cancellation", function () {
                    $.ajax({
                        type: "GET",
                        url: "/dashboardDsCancel?id=" + jobId
                    }).success(function () {
                        dashboardReload()
                    })
                })
            }
        }
        ,
        cancelEtl: function cancelEtl(jobId) {
            if (jobId !== undefined) {
                Modal.confirm("Proceed cancelling etl import '" + jobId + "'?", "ETL Import cancellation", function () {
                    $.ajax({
                        type: "GET",
                        url: "/dashboardEtlCancel?id=" + jobId
                    }).success(function () {
                        dashboardReload()
                    })
                })
            }
        }
    }
})()

function dashboardReload() {
    $("#etlRunning").html("<img src='/img/loading.gif'/>")
    $("#etlLabel").html("&nbsp;")
    $("#etlStatusTable").html("<img src='/img/loading.gif'/>")


    $("#dsRunning").html("<img src='/img/loading.gif'/>")
    $("#dsLabel").html("&nbsp;")
    $("#dsStatusTable").html("<img src='/img/loading.gif'/>")

    $("#queryRunning").html("<img src='/img/loading.gif'/>")
    $("#queryLabel").html("&nbsp;")
    $("#queryStatusTable").html("<img src='/img/loading.gif'/>")

    if($('#showQuery').val() === 'true') {
        $("#searchQueryDiv").hide();
        $.ajax({
            type: "GET",
            url: "/dashboardQueries.json"
        }).done(function (response) {
            queryStatusResponse = JSON.parse(response)
            var running = 0;
            var dateTimeIdx = 0;
            var statusIdx = queryStatusResponse[0].indexOf("Status");
            for (i = 1; i < queryStatusResponse.length; i++) {
                if (queryStatusResponse[i][statusIdx] == 'running') {
                    running++;
                }
                var millis = parseInt(queryStatusResponse[i][dateTimeIdx])
                var dt = new Date(millis);
                queryStatusResponse[i][dateTimeIdx] = formatDateTime(dt, "yyyy-MM-dd HH:mm:ss")
            }
            $("#queryRunning").html(running + "")
            $("#queryLabel").html("Running Queries")
            $("#searchQueryDiv").show();
            renderQueryTable(queryStatusResponse)
        }).error(function (result) {
            $("#queryRunning").html("n/a")
            $("#queryLabel").html("SQL Query")
            if (result.status == 400) {
                $("#queryStatusTable").html("Query engine is not responding")
            }
            refreshPage();
        })
    }

    if($('#showDs').val() === 'true') {
        $("#searchDsDiv").hide();
        $.ajax({
            type: "GET",
            url: "/dashboardDataSources.json"
        }).done(function (response) {
            dataSourceStatusResponse = JSON.parse(response)
            var running = 0;
            var dateTimeIdx = 0;
            var statusIdx = dataSourceStatusResponse[0].indexOf("Status");
            for (i = 1; i < dataSourceStatusResponse.length; i++) {
                if (dataSourceStatusResponse[i][statusIdx] == 'running') {
                    running++;
                }
                var millis = parseInt(dataSourceStatusResponse[i][dateTimeIdx])
                var dt = new Date(millis);
                dataSourceStatusResponse[i][dateTimeIdx] = formatDateTime(dt, "yyyy-MM-dd HH:mm:ss")
            }
            $("#dsRunning").html(running + "")
            $("#dsLabel").html("Importing DataSource(s)")
            $("#searchDsDiv").show();
            renderDsTable(dataSourceStatusResponse)
        }).error(function (result) {
            $("#dsRunning").html("n/a")
            $("#dsLabel").html("DataSource")
            if (result.status == 400) {
                $("#dsStatusTable").html("Data source engine is not responding")
            }
            refreshPage();
        })
    }

    if($('#showEtl').val() === 'true') {
        $("#searchEtlDiv").hide();
        $.ajax({
            type: "GET",
                url: "/dashboardEtl.json"
        }).done(function (response) {
            etlStatusResponse = JSON.parse(response)
            var running = 0;
            var dateTimeIdx = 0;
            var statusIdx = etlStatusResponse[0].indexOf("Status");
            for (i = 1; i < etlStatusResponse.length; i++) {
                if (etlStatusResponse[i][statusIdx] == 'running') {
                    running++;
                }
                var millis = parseInt(etlStatusResponse[i][dateTimeIdx])
                var dt = new Date(millis);
                etlStatusResponse[i][dateTimeIdx] = formatDateTime(dt, "yyyy-MM-dd HH:mm:ss")
            }
            $("#etlRunning").html(running + "")
            $("#etlLabel").html("Importing ETL Job(s)")
            $("#searchEtlDiv").show();
            renderEtlTable(etlStatusResponse)

        }).error(function (result) {
            $("#etlRunning").html("n/a")
            $("#etlLabel").html("ETL Workflow")
            if (result.status == 400) {
                $("#etlStatusTable").html("ETL Workflow engine is not responding")
            }
            refreshPage();
        })
    }

    $(function () {
        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            // save the latest tab; use cookies if you like 'em better:
            localStorage.setItem('dashboardTab', $(this).attr('href'));
        });
        var lastTab = localStorage.getItem('dashboardTab');
        if (lastTab) {
            $('[href="' + lastTab + '"]').tab('show');
        }
    });
}

function renderQueryTable(data){
    if($('#showQuery').val() !== 'true') {
        return
    }
    var filter = $('#searchQuery').val().trim()
    if(filter && filter.length > 0){
        data=queryStatusResponse.filter(function(o, i){
            if(i==0){
                return true;
            }else{
                var valid=false;
                for(j=0; j< o.length && !valid;j++){
                    if(o[j].indexOf(filter)>=0){
                        valid=true;
                    }
                }
                return valid;
            }
        });
    }

    $("#queryStatusTable").html("")
    var queryStatusContainer = document.getElementById('queryStatusTable');
    if(queryHotStatus){
        queryHotStatus.destroy();
    }
    if(data.length>1) {
        queryHotStatus = new Handsontable(queryStatusContainer,
            {
                colHeaders: data.shift(),
                data: data,
                readOnly: true,
                manualColumnResize: true,
                manualRowResize: true,
                rowHeaders: true,
                copyRowsLimit: 2147483647,
                contextMenu: false,
                colWidths: [140, 140, 100, 140, 70, 500],
                preventOverflow: 'horizontal',
                wordWrap: false,
                comments: false,
                stretchH: 'none',
                cells: function (row, col, prop) {
                    var cellProperties = {}
                    if (col === 1) {
                        cellProperties.renderer = function (instance, td, row, col, prop, value, cellProperties) {
                            Handsontable.renderers.HtmlRenderer.apply(this, arguments)
                        }
                    }

                    return cellProperties
                }
            });
    }
    refreshPage();
}

function renderDsTable(data){
    if($('#showDs').val() !== 'true') {
        return
    }
    var filter = $('#searchDs').val().trim()
    if(filter.length>0){
        data=dataSourceStatusResponse.filter(function(o, i){
            if(i==0){
                return true;
            }else{
                var valid=false;
                for(j=0; j< o.length && !valid;j++){
                    if(o[j].indexOf(filter)>=0){
                        valid=true;
                    }
                }
                return valid;
            }
        });
    }

    $("#dsStatusTable").html("")
    var dataSourceStatusContainer = document.getElementById('dsStatusTable');
    if(dsHotStatus){
        dsHotStatus.destroy();
    }
    if(data.length>1) {
        dsHotStatus = new Handsontable(dataSourceStatusContainer,
            {
                colHeaders: data.shift(),
                data: data,
                readOnly: true,
                manualColumnResize: true,
                manualRowResize: true,
                rowHeaders: true,
                copyRowsLimit: 2147483647,
                contextMenu: false,
                colWidths: [140, 140, 260, 140, 140, 70, 200],
                preventOverflow: 'horizontal',
                wordWrap: false,
                comments: false,
                cells: function (row, col, prop) {
                    var cellProperties = {}
                    if (col === 1) {
                        cellProperties.renderer = function (instance, td, row, col, prop, value, cellProperties) {
                            Handsontable.renderers.HtmlRenderer.apply(this, arguments)
                        }
                    }
                    return cellProperties
                }
            });
    }
    refreshPage();
}

function renderEtlTable(data){
    if($('#showEtl').val() !== 'true') {
        return
    }
    var filter = $('#searchEtl').val().trim()
    if(filter && filter.length > 0){
        data=etlStatusResponse.filter(function(o, i){
            if(i==0){
                return true;
            }else{
                var valid=false;
                for(j=0; j< o.length && !valid;j++){
                    if(o[j].indexOf(filter)>=0){
                        valid=true;
                    }
                }
                return valid;
            }
        });
    }

    $("#etlStatusTable").html("")
    var etlStatusContainer = document.getElementById('etlStatusTable');
    if(etlHotStatus){
        etlHotStatus.destroy();
    }
    if(data.length>1) {
        etlHotStatus = new Handsontable(etlStatusContainer,
            {
                colHeaders: data.shift(),
                data: data,
                readOnly: true,
                manualColumnResize: true,
                manualRowResize: true,
                rowHeaders: true,
                copyRowsLimit: 2147483647,
                contextMenu: false,
                colWidths: [140, 140, 240, 240, 140, 140, 70, 150],
                preventOverflow: 'horizontal',
                wordWrap: false,
                comments: false,
                cells: function (row, col, prop) {
                    var cellProperties = {}
                    if (col === 1) {
                        cellProperties.renderer = function (instance, td, row, col, prop, value, cellProperties) {
                            Handsontable.renderers.HtmlRenderer.apply(this, arguments)
                        }
                    }
                    return cellProperties
                }
            });
    }
    refreshPage();
}

$('a[href="#dashboardQuery"]').on('shown.bs.tab', function (e) {
    if(queryHotStatus) {
        queryHotStatus.render();
    }
    refreshPage();
});

$('a[href="#queryDashboardTab"]').on('shown.bs.tab', function (e) {
    if(queryHotStatus) {
        queryHotStatus.render();
    }
    refreshPage();
});

$('a[href="#dsDashboardTab"]').on('shown.bs.tab', function (e) {
    if(dsHotStatus) {
        dsHotStatus.render();
    }
    refreshPage();
});

$('a[href="#etlDashboardTab"]').on('shown.bs.tab', function (e) {
    if(etlHotStatus) {
        etlHotStatus.render();
    }
    refreshPage();
});


$('#searchEtl').on('input', function() {
    if(etlStatusResponse!=null){
        renderEtlTable(etlStatusResponse)
    }
});

$('#searchQuery').on('input', function() {
    if(queryStatusResponse!=null){
        renderQueryTable(queryStatusResponse)
    }
});

$('#searchDs').on('input', function() {
    if(dataSourceStatusResponse!=null){
        renderDsTable(dataSourceStatusResponse)
    }
});


