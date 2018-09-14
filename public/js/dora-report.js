var reportDataTable = null
var reportHotStatus = null
var currentReport=null
var reportCharts=[]
var lastDate=null
sectionMap["report"] = (function () {
    return {
        setup: setup,
        beforeShowSection: beforeShowSection,
        showSection: showSection,
        showEntityForm: showEntityForm,
        closeEntityForm: closeEntityForm
    }

    function setup() {
        $("#reportSelectTxDate").on('dp.change', function(e){
            if ($('#reportSelectTxDate').data("DateTimePicker")) {
                var selectedDate=$("#reportSelectTxDate").find("input").val()
                showEntityForm(currentReport,selectedDate)
            }
        })
    }

    function beforeShowSection() {
        if (!getUiEntityOpened("report")) {
            $('#reportTableDiv').fadeOut(0);
            $('#reportListLoading').fadeIn(1000);
        }
    }

    function showSection() {
        var hash = window.location.hash.substring(1)
        if(hash.split('/').length > 1){
            var name = hash.split('/')[1]
            var date = ""
            if(hash.split('/')[2] && hash.split('/')[2].length > 0) {
                date = hash.split('/')[2]
            }
            showEntityForm(name, date);
        }else{
            if (getUiEntityOpened("report")) {
                closeEntityForm();
            }else{
                showReportTable();
            }
        }
        if (getUiLastFocus("report") != null) {
            getUiLastFocus("report").focus();
        }
    }

    function showEntityForm(name, date) {
        $('#reportEntity').hide();
        $('#reportDatePicker').hide();
        $('#reportDescription').hide();
        clearCharts();
        setUiEntityOpened("report", true)
        currentReport=name;
        $('#reportTableDiv').fadeOut(0, function () {
            $('#reportListLoading').fadeIn(1000)

            $.ajax({
                type: "GET",
                url: "reportDataset.html?name=" + name + "&date=" + date
            }).done(function (response) {
                var data = JSON.parse(response)
                var reportDate = data.dateTime && data.dateTime.substr(0, 10) || date
                var path = '#report/' + name
                if(reportDate && reportDate.length > 0) {
                    path = path + "/" + reportDate
                }
                window.location.hash = path;
                $('#reportListLoading').stop();
                $('#reportListLoading').fadeOut(100, function () {
                    $('#reportPageHeader').html('<i class="fa fa-file-text-o fa-fw"></i> <a href="#report" onclick="sectionMap[\'report\'].closeEntityForm();">Report</a>' + '<i class="fa fa-angle-right fa-fw"></i> ' + name)
                    renderReport(data)
                });
            }).error(function (result) {
                Modal.show("<textarea " +
                    "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                    " disabled>" + result.responseText + "</textarea>", "ERROR");
            })
        })
    }

    function clearCharts(){
        //remove existing chart containers
        var chartsDiv=document.getElementById("reportChartsDiv");
        while (chartsDiv.firstChild) {
            chartsDiv.removeChild(chartsDiv.firstChild);
        }

        //remove existing chart elements
        for(var i=0;i<reportCharts.length;i++){
            try{
                reportCharts[i].destroy();
            }catch(x){}
        }
        reportCharts=[]
        $("#reportChartsDiv").html("");
    }

    function renderReport(data) {
        var charts=data.charts
        if(charts.length>0){
            for(var i=0;i<charts.length;i++){
                var newSpan = document.createElement('span');
                newSpan.setAttribute('id', 'rep_chart_'+i);
                $("#reportChartsDiv").append(newSpan);
                var chart=renderChart('rep_chart_'+i,charts[i],data.fieldNames,data.data,400,350);
                reportCharts.push(chart);
            }
            $('#reportChartsDiv').fadeIn(0);
        }


        $("#reportEntity").html("");
        $('#reportEntity').fadeIn();
        if(data.description!=null && data.description.trim().length>0){
            $('#reportDescription').fadeIn();
            $("#reportDescription").html("<pre>"+data.description+"</pre><br/>")
        }
        var reportContainer = document.getElementById('reportEntity');
        var tableContent = Array(data.fieldNames).concat(data.data)
        if(reportHotStatus!=null){
            reportHotStatus.destroy();
        }
        reportHotStatus = new Handsontable(reportContainer,
            {
                data: tableContent,
                preventOverflow: 'horizontal',
                readOnly: true,
                manualColumnResize: true,
                manualRowResize: true,
                stretchH: 'none',
                copyRowsLimit: 2147483647,
                contextMenu: false,
                cells: function (row, col, prop) {
                    var cellProperties = {}
                    if (row === 0) {
                        cellProperties.renderer = function (instance, td, row, col, prop, value, cellProperties) {
                            Handsontable.renderers.TextRenderer.apply(this, arguments)
                            td.style.align = "right"
                            td.style.background = '#eee'
                        }
                    }
                    return cellProperties
                }
            });

        $("#reportSelectTxDate").find("input").val(data.dateTime.substr(0,10))

        $('#reportDatePicker').fadeIn();
        if ($('#reportSelectTxDate').data("DateTimePicker")) {
            $('#reportSelectTxDate').data("DateTimePicker").destroy();
        }
        $('#reportSelectTxDate').datetimepicker({
            format: 'YYYY-MM-DD'
        });



        $('#reportSelectTxDate').data("DateTimePicker").enabledDates(data.availableDates)
        refreshPage();
    }

    function closeEntityForm() {
        setUiEntityOpened("report", false)
        for (var i = 0; i < getUiBuffer("report").length; i++) {
            getUiBuffer("report")[i].destroy();
        }
        clearCharts();
        $('#reportDatePicker').hide();
        $('#reportEntity').empty();
        $('#reportEntity').hide();
        $('#reportDescription').hide();
        $('#reportPageHeader').html("<i class=\"fa fa-file-text-o fa-fw\"></i> <a style=\"cursor:pointer ;\" onclick=\"$('#menu_report').click()\">Report</a>")
        $('#reportListLoading').fadeIn(1000);
        showReportTable();
    }

    function showReportTable() {
        clearCharts();

        $.getJSON('/reportList.json', function (result) {
            if (getUiEntityOpened("report")) {
                return;
            }
            $('#reportListLoading').stop();
            $('#reportListLoading').fadeOut(100, function () {
                $('#reportTableDiv').fadeIn(100, function () {
                    $('#reportListTable').empty();
                    var reportList = $.map(result, function (report, idx) {
                        var txDate = report.dateTime.substr(0,10);
                        var onClick = "sectionMap[\"report\"].showEntityForm(\"" + report.name + "\",\"" + txDate + "\")";
                        report.clickableName =
                            "<a class='viewBtn' onclick='" + onClick + "' style='cursor:pointer;' "
                            + report.name + "'>" + report.name + "</a>";
                        report.txDate=txDate
                        return report
                    })
                    if (reportDataTable) {
                        reportDataTable.destroy();
                    }

                    reportDataTable = $('#reportListTable').DataTable({
                        data: reportList,
                        paging: false,
                        bStateSave: true,
                        columnDefs: [{
                            defaultContent: "",
                            targets: "_all"
                        }],
                        columns: [
                            {title: 'Name', data: 'clickableName',"width": "200px"},
                            {title: 'Description', data: 'description'},
                            {title: 'Last TxDate', data: 'txDate', "width": "100px"},
                        ]
                    });
                    if (sectionMap["reportClearSearch"] == null) {
                        sectionMap["reportClearSearch"] = true;
                        reportDataTable.search('').columns().search('').draw();
                    }
                });
            });
        })
    }
})()