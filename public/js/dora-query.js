var queryTemplates = [];
var queryEditor;
var queryCurrentTemplateName = "";
var queryMaxRows = 500
var queryResultHolder = null
var queryCurrentExecuteAjax = null
var queryResult = null
var querySelectableHeaders = true
var queryStopping = false
var queryResultPageSize = 10000
var currentQueryJobId=null

sectionMap["query"] = (function () {
    var tableMetaData = (function () {
        var schemas = {};

        function parseObjectKeyToArray(obj) {
            var arr = [];
            for (var key in obj) {
                if (obj.hasOwnProperty(key)) {
                    arr.push(key);
                }
            };
            return arr
        }

        var getSchemas = function(forceUpdate) {
            if(forceUpdate || $.isEmptyObject(schemas)) {
                return $.when(asyncQuery('/querySchema.json'))
                    .then(function(response) {
                        if(response) {
                            $.each(response, function(key, value) {
                                schemas[value] = {};
                            })
                        }
                        return parseObjectKeyToArray(schemas)
                    })
            } else {
                var dfd = $.Deferred();
                dfd.resolve(parseObjectKeyToArray(schemas));
                return dfd.promise();
            }
        }

        var getTables = function(schema, forceUpdate) {
            if(!schema) return
            if(forceUpdate || $.isEmptyObject(schemas[schema])) {
                return $.when(asyncQuery('/queryTable.json?schema=' + schema))
                    .then(function(response) {
                        if(response) {
                            $.each(response, function(key, value) {
                                schemas[schema][value] = [];
                            })
                        }
                        return parseObjectKeyToArray(schemas[schema])
                    })
            } else {
                var dfd = $.Deferred();
                dfd.resolve(parseObjectKeyToArray(schemas[schema]));
                return dfd.promise();
            }
        }

        return {
            getSchemas: getSchemas,
            getSchema: function(schemaName) {
                if(!schemaName) return false
                return $.when(getSchemas())
                    .then(function(schemas) {
                        var matchedSchema = null
                        $.each(schemas, function(key, value) {
                            if(schemaName === value) {
                                matchedSchema = value;
                            }
                        })
                        return matchedSchema;
                    })
            },
            getTables: getTables,
            getFields: function(schema, table, forceUpdate) {
                if(!schema || !table) return
                if(forceUpdate || $.isEmptyObject(schemas[schema]) || $.isEmptyObject(schemas[schema][table])) {
                    return $.when(asyncQuery('/queryField.json?schema=' + schema + '&table=' + table))
                        .then(function(response) {
                            if(!schemas[schema]) {
                                schemas[schema] = {}
                            }
                            if(!schemas[schema][table]) {
                                schemas[schema][table] = []
                            }
                            $.each(response, function(key, value) {
                                schemas[schema][table].push(value)
                            })
                            return schemas[schema][table]
                        })
                } else {
                    var dfd = $.Deferred();
                    dfd.resolve(schemas[schema][table]);
                    return dfd.promise();
                }
            },
        }
    })()


    return {
        setup: function setup() {
            setupQueryEditor();
            setupQueryTemplateList();
            setupQueryToolTips();
            setupQuerySchema();
            setupQueryResult();
            setupQueryActions();
        }
        ,
        beforeShowSection: function beforeShowSection() {
            //do nothing
        }
        ,
        showSection: function showSection() {
            if (queryEditor != null) {
                queryEditor.focus()
            }

            if (queryResultHolder != null) {
                queryResultHolder.render()
            }
        }
    }


    function setupQueryTemplateList() {
        $.getJSON("/queryTemplate.json", function (json) {
            queryTemplates = []
            $.each(json, function (k, v) {
                queryTemplates.push({key: k, value: v})
            })
            queryTemplates = queryTemplates.sort(function (obj1, obj2) {
                a = obj1.key
                b = obj2.key
                return a < b ? -1 : (a > b ? 1 : 0)
            })
        })
    }

    function setupQueryEditor() {
        var langTools = ace.require("ace/ext/language_tools");
        queryEditor = ace.edit("queryEditor")
        queryEditor.setShowPrintMargin(false)
        queryEditor.setTheme("ace/theme/chrome")
        queryEditor.getSession().setMode("ace/mode/hive")
        queryEditor.getSession().setOptions({
            tabSize: 2,
            useSoftTabs: true,
        })
        queryEditor.getSession().setUseWrapMode(true)
        queryEditor.setOptions({
            minLines: 18,
            maxLines: 18,
            enableBasicAutocompletion: true,
        })

        queryEditor.setValue("select 'Hello' as greeting")
        queryEditor.focus()
        $("#queryEditor").show
    }

    function setupQueryResult() {
        $("#queryErrorBlock").on("focus", function () {
            $("#queryErrorBlock").blur()
        })
    }

    function setupQueryToolTips() {
        $('.queryBtns').tooltip({
            selector: "[data-hover=tooltip]",
            container: "body"
        })
    }

    function setupQuerySchema() {
        $('#querySchema')
            .jstree('destroy')
            .jstree({
                'core': {
                    'data': function (obj, cb) {
                        if (obj.id === '#') {   //schema
                            $.when(tableMetaData.getSchemas(true))
                                .then(function(schemas) {
                                    var schemaNodes = $.map(schemas, function (schema) {
                                        return {
                                            id: schema,
                                            text: schema,
                                            icon: 'fa fa-database',
                                            state: {
                                                selected: schema === 'default' ? true : false,
                                            },
                                            children: true,
                                        }
                                    })
                                    cb.call(this, schemaNodes)
                                });
                        } else if (obj.parent === '#') {    //table
                            $.when(tableMetaData.getTables(obj.id, true))
                                .then(function(tables) {
                                    var tableNodes = $.map(tables, function (table) {
                                        return {
                                            id: table,
                                            text: table,
                                            icon: 'fa fa-table',
                                            children: true,
                                        }
                                    })
                                    cb.call(this, tableNodes)
                                })
                        } else {    //field
                            $.when(tableMetaData.getFields(obj.parent, obj.id, true))
                                .then(function(fields) {
                                    var fieldNodes = $.map(fields, function (field) {
                                        if (field.endsWith('[P]')) {
                                            return {
                                                id: obj.id + "." + field,
                                                text: field,
                                                icon: 'fa fa-key',
                                                children: false,
                                            }
                                        } else {
                                            return {
                                                id: obj.id + "." + field,
                                                text: field,
                                                icon: 'fa fa-bars',
                                                children: false,
                                            }
                                        }

                                    })
                                    cb.call(this, fieldNodes)
                                })
                        }
                    }
                }
            })

        $('#querySchema').on("select_node.jstree", function (e, data) {
            var content = data.node.text.split(' ')[0]
            if(queryEditor != null) {
                queryEditor.insert(content);
            }
        });

        $("#schemaNav").bind("select_node.jstree", function (e, data) {
            return data.instance.toggle_node(data.node);
        });
    }

    function queryRenderResult() {
        console.log('Enter queryRenderResult')
        if (queryResult != null) {
            if (queryResult.error!=undefined) {
                $("#queryErrorBlock").html(queryResult.error)
                $("#queryErrorDiv").fadeIn(500)
                var errorBlock = document.getElementById("queryErrorBlock")
                errorBlock.style.height = (25 + errorBlock.scrollHeight) + "px"
            } else {
                var dataLength = queryResult.data && queryResult.data.length || 0
                if (dataLength > 0 && queryResult.data[0].length > 1) {
                    $('#queryStatsMsg').html(dataLength + " rows, " + queryResult.elapsed + " (max.rows=" + queryMaxRows + ")&nbsp;&nbsp; <a onclick='showQueryChart()' data-toggle='modal' data-target='#queryChartModal'><i class='fa fa-bar-chart'></i></a>")
                } else {
                    $('#queryStatsMsg').html(dataLength + " rows, " + queryResult.elapsed + " (max.rows=" + queryMaxRows + ")")
                }
                $('#queryStatsDiv').fadeIn(100)
                $("#queryResultDiv").html("")
                if (dataLength > 0) {
                    if (queryResultHolder) {
                        queryResultHolder.destroy();
                    }
                    $("#queryResultDiv").fadeIn(100)
                    if (!querySelectableHeaders) {
                        var queryResultDiv = document.getElementById('queryResultDiv')
                        queryResultHolder = new Handsontable(queryResultDiv, {
                            data: queryResult.data,
                            preventOverflow: 'horizontal',
                            readOnly: true,
                            manualColumnResize: true,
                            manualRowResize: true,
                            rowHeaders: true,
                            colHeaders: queryResult.columns,
                            stretchH: 'none',
                            copyRowsLimit: 2147483647,
                            contextMenu: false
                        })
                    } else {
                        var queryResultDiv = document.getElementById('queryResultDiv')
                        var headers = Array.from(Array(dataLength + 1).keys())
                        headers[0] = ""
                        queryResultHolder = new Handsontable(queryResultDiv, {
                            data: Array(queryResult.columns).concat(queryResult.data),
                            preventOverflow: 'horizontal',
                            readOnly: true,
                            manualColumnResize: true,
                            manualRowResize: true,
                            rowHeaders: headers,
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
                        })
                    }
                    if (queryResultHolder != null) {
                        queryResultHolder.render()
                    }
                }
            }
        }
    }

    function setupQueryActions() {

        $(".namespace").click(function() {
            $('#namespaceInput').val($(this).prop("name"));
            $('#stateInput').val(window.location.hash.substr(1));
            $('#namespaceForm').submit();
        })

        $("#querySchemaRefreshBtn").click(function () {
            setupQuerySchema()
        })

        $("#queryExecuteBtn").click(function () {
            var d = new Date().getTime();
            if(Date.now){
                d = Date.now(); //high-precision timer
            }
            currentQueryJobId='xxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = (d + Math.random()*16)%16 | 0;
                d = Math.floor(d/16);
                return (c=='x' ? r : (r&0x3|0x8)).toString(16);
            });

            var sql = ""
            if (queryEditor.getSelectedText().length == 0) {
                sql = queryEditor.getValue()
            } else {
                sql = queryEditor.getSelectedText()
            }
            var txDate = document.getElementById('queryTxDateInput').value

            if (queryCurrentExecuteAjax != null) {
                queryCurrentExecuteAjax.abort()
                queryCurrentExecuteAjax = null
            }
            $('#queryErrorDiv').hide()
            $('#queryResultDiv').hide()
            $('#queryStatsDiv').hide()

            if (queryMaxRows <= queryResultPageSize) {
                $("#queryExecuteBtn").hide()
                $("#queryStopBtn").stop()
                $("#queryStopBtn").fadeIn(100)
                $('#queryProcessingDiv').show()
                var data = {
                    jobId: currentQueryJobId,
                    limit: queryMaxRows,
                    sql: sql,
                    txDate: txDate,
                }
                queryCurrentExecuteAjax = $.ajax({
                    type: "POST",
                    url: "/queryResult",
                    data: JSON.stringify(data),
                    contentType: "application/json charset=utf-8",
                    timeout: 3600000
                }).done(function (response) {
                    $('#queryProcessingDiv').stop()
                    $('#queryProcessingDiv').fadeOut(100, function () {
                        queryResult = JSON.parse(response.substring(response.indexOf('{')))
                        queryRenderResult()
                    })
                    $("#queryStopBtn").stop()
                    $("#queryStopBtn").hide()
                    $("#queryExecuteBtn").show()
                    queryCurrentExecuteAjax == null
                }).error(function (result) {
                    if (queryCurrentExecuteAjax != null) {
                        if (!queryStopping) {
                            $('#queryProcessingDiv').stop()
                            $('#queryProcessingDiv').fadeOut(100, function () {
                                $("#queryErrorBlock").html("Please try again later")
                                $("#queryErrorDiv").fadeIn(500)
                                var errorBlock = document.getElementById("queryErrorBlock")
                                errorBlock.style.height = (25 + errorBlock.scrollHeight) + "px"
                            })
                            queryCurrentExecuteAjax == null
                        } else {
                            $('#queryProcessingDiv').stop()
                            $('#queryProcessingDiv').fadeOut(100)
                        }
                    }
                    $("#queryStopBtn").stop()
                    $("#queryStopBtn").hide()
                    $("#queryExecuteBtn").show()
                })
                queryEditor.focus()
            } else {
                $('#queryStatsMsg').html('your query result is being downloaded as file (max.rows=' + queryMaxRows + ')')
                $('#queryStatsDiv').fadeIn(100)
                queryEditor.focus()

                $('#queryDownloadSql').val(sql)
                $('#queryDownloadLimit').val(queryMaxRows)
                $('#queryDownloadTxDate').val(txDate)
                $('#queryDownloadJobId').val(currentQueryJobId)
                $('#queryDownloadTsv').submit()
            }
        })

        $("#queryStopBtn").click(function () {
            queryStopping = true
            if (queryCurrentExecuteAjax != null) {
                queryCurrentExecuteAjax.abort()
                queryCurrentExecuteAjax = null
            }
            queryStopping = false
            $.ajax({
                type: "GET",
                url: "/dashboardQueryCancel?id=" + currentQueryJobId
            })

            $("#queryStopBtn").stop()
            $("#queryStopBtn").hide()
            $("#queryExecuteBtn").show()
        })


        $("#querySettingsMaxResults").change(function () {
            queryMaxRows = $("#querySettingsMaxResults option:selected").val()
            if (queryMaxRows <= queryResultPageSize) {
                $("#queryExecuteBtnIcon").removeClass().addClass('fa').addClass('fa-play');
            } else {
                $("#queryExecuteBtnIcon").removeClass().addClass('fa').addClass('fa-download');
            }
        })

        $("#querySettingsResultColNames").change(function () {
            querySelectableHeaders = $.parseJSON($("#querySettingsResultColNames option:selected").val())
            queryRenderResult()
        })

        $("#queryOpenTemplateBtn").click(function () {
            setupQueryTemplateList()
            $("#queryOpenTemplateName").empty()
            $("#queryOpenTemplateName").append($("<option />").val("").text("select"))
            $.each(queryTemplates, function (k, v) {
                $("#queryOpenTemplateName").append($("<option />").val(v.value).text(v.key))
            })
            $("#queryOpenTemplateName option").filter(function () {
                return $(this).text() == queryCurrentTemplateName
            }).prop('selected', true)
            $("#queryOpenTemplateContent").val($("#queryOpenTemplateName  option:selected").val())
            setTimeout(function () {
                $("#queryOpenTemplateName").focus()
            }, 500)
        })

        $("#queryOpenTemplateName").change(function () {
            $("#queryOpenTemplateContent").val($("#queryOpenTemplateName  option:selected").val())
        })

        $("#queryOpenTemplateContent").on("focus", function () {
            $("#queryOpenTemplateName").focus()
        })

        $("#queryOpenTemplateConfirm").click(function () {
            queryCurrentTemplateName = $("#queryOpenTemplateName  option:selected").text()
            queryEditor.setValue($("#queryOpenTemplateContent").val(), 1)
        })

        $('#queryOpenTemplateModal').on('hidden.bs.modal', function () {
            queryEditor.focus()
        })

        $("#querySaveTemplateBtn").click(function () {
            $("#querySaveTemplateName").val(queryCurrentTemplateName)
            $("#querySaveTemplateContent").val(queryEditor.getValue())
            setTimeout(function () {
                $("#querySaveTemplateName").focus()
                $("#querySaveTemplateName").select()
            }, 500)
        })

        $("#querySaveTemplateContent").on("focus", function () {
            $("#querySaveTemplateName").focus()
        })

        $("#querySaveTemplateConfirm").click(function () {
            var data = {
                name: $("#querySaveTemplateName").val(),
                sql: $("#querySaveTemplateContent").val()
            }
            queryCurrentTemplateName = $("#querySaveTemplateName").val()
            $.ajax({
                type: "POST",
                url: "/queryTemplate.json?post=true",
                data: JSON.stringify(data),
                contentType: "application/json charset=utf-8",
                dataType: "json",
                complete: function () {
                    setupQueryTemplateList()
                }
            })
        })

        $('#querySaveTemplateModal').on('hidden.bs.modal', function () {
            queryEditor.focus()
        })

        $("#queryDeleteTemplateName").change(function () {
            $("#queryDeleteTemplateContent").val($("#queryDeleteTemplateName  option:selected").val())
        })

        $("#queryDeleteTemplateBtn").click(function () {
            $("#queryDeleteTemplateName").empty()
            $("#queryDeleteTemplateName").append($("<option />").val("").text("select"))
            $.each(queryTemplates, function (k, v) {
                $("#queryDeleteTemplateName").append($("<option />").val(v.value).text(v.key))
            })
            $("#queryDeleteTemplateName option").filter(function () {
                return $(this).text() == queryCurrentTemplateName
            }).prop('selected', true)
            $("#queryDeleteTemplateContent").val($("#queryDeleteTemplateName  option:selected").val())
            setTimeout(function () {
                $("#queryDeleteTemplateName").focus()
            }, 500)
        })

        $("#queryDeleteTemplateContent").on("focus", function () {
            $("#queryDeleteTemplateName").focus()
        })

        $("#queryDeleteTemplateConfirm").click(function () {
            $.ajax({
                type: "GET",
                url: "/queryTemplate.json?delete=" + $('#queryDeleteTemplateName option:selected').text(),
                complete: function () {
                    setupQueryTemplateList()
                }
            })
        })

        $('#deleteTemplateModal').on('hidden.bs.modal', function () {
            queryEditor.focus()
        })


        $("#queryNewBtn").click(function () {
            queryCurrentTemplateName = ""
            queryEditor.setValue('')
            queryEditor.focus()
        })

        $("#queryChartSelect").change(function () {
            queryChartUpdate();
        })
    }
})()

function showQueryChart() {
    var container = $('#queryChartFieldsDiv');
    var inputs = container.find('input');
    for (var i = 0; i < inputs.lengh; i++) {
        container.removeChild(inputs[i]);
    }
    $('#queryChartFieldsDiv').empty();
    for (var i = 1; i < queryResult.columns.length; i++) {
        var cbValue = queryResult.columns[i]
        $('<input />', {
            type: 'checkbox',
            id: 'cb_' + queryResult.columns[i],
            value: cbValue,
            checked: i==1,
            onclick: 'queryChartUpdate();'
        }).appendTo(container);
        $('<span />', {
            'for': queryResult.columns[i] + '_cb',
            id: 'p_' + queryResult.columns[i],
            text: '  ' + cbValue
        }).appendTo(container);
        $('<br />', {
            'for': queryResult.columns[i] + '_p',
            id: 'br_' + queryResult.columns[i],
            text: cbValue
        }).appendTo(container);
    }

    $("#queryChartGroupBySelect").empty()
    $("#queryChartGroupBySelect").append($("<option />").val("").text("select"))
    for (var i = 1; i < queryResult.columns.length; i++) {
        $("#queryChartGroupBySelect").append($("<option />").val(queryResult.columns[i]).text(queryResult.columns[i]))
    }
    queryChartUpdate();
}

$('#queryChartGroupBySelect').change(function () {
    queryChartUpdate();
})

var lastIndices = []
function queryChartUpdate() {
    var selectedIndices = [];
    for (var i = 1; i < queryResult.columns.length; i++) {
        var cb = $('#cb_' + queryResult.columns[i])
        if (cb.prop('checked')) {
            selectedIndices.push(i);
        }
    }

    if (selectedIndices.length == 0) {
        selectedIndices = lastIndices
        for (var i = 1; i < queryResult.columns.length; i++) {
            var cb = $('#cb_' + queryResult.columns[i])
            if(selectedIndices.indexOf(i)>=0) {
                cb.prop("checked", true);
            }
        }
    }
    lastIndices=selectedIndices;


    if(selectedIndices.length==1){
        $('#queryChartGroupByLabel').show();
        $('#queryChartGroupBySelect').show();
    }else{
        $('#queryChartGroupByLabel').hide();
        $('#queryChartGroupBySelect').hide();
        $('#queryChartGroupBySelect').val("");
    }

    var selectedFields = "";
    for (var i = 1; i < queryResult.columns.length; i++) {
        var cb = $('#cb_' + queryResult.columns[i])
        if (cb.prop('checked')) {
            if (selectedFields.length > 0) {
                selectedFields += ', '
            }
            selectedFields += queryResult.columns[i]
        }
    }

    var formula = '';
    if (selectedIndices.length == queryResult.columns.length - 1) {
        formula = $("#queryChartSelect").val() + '()';
    } else {
        formula = $("#queryChartSelect").val() + '(' + selectedFields + ')';
    }

    var groupBy=$('#queryChartGroupBySelect').val()
    if(selectedIndices.length==1 && groupBy!=''){
        formula+=".groupBy("+groupBy+")"
    }

    $('#queryChartFormula').html(formula)

    renderChart("queryChartDiv", formula, jQuery.extend(true, [], queryResult.columns), jQuery.extend(true, [], queryResult.data), 550, 450);
}

function asyncQuery(url) {
    var dfd = $.Deferred();

    $.getJSON(url, function (response) {
        dfd.resolve(response);
    }, function (error) {
        dfd.reject(error);
    })

    return dfd.promise();
}