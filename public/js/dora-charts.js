function renderChart(targetId, formula, cols, data, width, height) {
    var renderCols=cols || [];
    var renderData=data || [];
    if (formula != null && formula.trim().length > 0) {
        var tokens = formula.split('(')
        var func = tokens[0];
        if(cols && data) {
            if (tokens.length > 1) {
                var parameters = tokens[1].trim()
                parameters=parameters.indexOf(')')>0?parameters.substr(0,parameters.indexOf(')')+1):parameters
                if (parameters.endsWith(')')) {
                    parameters = parameters.substr(0, parameters.length - 1)
                    if (parameters.trim().length > 0) {
                        var paramArr = parameters.split(",")
                        if (paramArr.length > 0) {
                            for (var i = 0; i < paramArr.length; i++) {
                                paramArr[i] = paramArr[i].trim();
                            }
                            var colEnabled = [];
                            for (var i = 0; i < cols.length; i++) {
                                colEnabled.push(i==0||paramArr.indexOf(cols[i]) >= 0);
                            }
                            var newCols = [];
                            var newData = [];

                            for (var r = 0; r < data.length; r++) {
                                var newRow = []
                                for (var c = 0; c < cols.length; c++) {
                                    if (colEnabled[c]) {
                                        if (r == 0) {
                                            newCols.push(cols[c]);
                                        }
                                        newRow.push(data[r][c])
                                    }
                                }
                                newData.push(newRow)
                            }
                            renderCols = newCols;
                            renderData = newData;
                        }
                    }
                }
            }

            var subCaption="";

            if(formula.indexOf('.groupBy(')>=0){
                var groupByField=formula.substr(formula.indexOf('.groupBy(')+9).replace(')','')
                var groupByFieldIndex=-1;
                for(i=0;i<cols.length && groupByFieldIndex<0;i++){
                    if(cols[i]==groupByField){
                        groupByFieldIndex=i;
                    }
                }
                if(groupByFieldIndex>=0){
                    var groupByObj={}
                    var rowIndexSet={}
                    var groupBySet={}
                    for(var i=0;i<renderData.length;i++){
                        groupByObj[data[i][0]+"-"+data[i][groupByFieldIndex]]=renderData[i][1];
                        groupBySet[data[i][groupByFieldIndex]]=true
                        rowIndexSet[data[i][0]]=true
                    }

                    var groupedByCols=Object.keys(groupBySet)
                    var rowIndex=Object.keys(rowIndexSet)
                    var groupedByData=[]
                    for(var i=0;i<rowIndex.length;i++){
                        var row=[]
                        row.push(rowIndex[i])
                        for(var j=0;j<groupedByCols.length;j++){
                            var key=rowIndex[i]+'-'+groupedByCols[j]
                            row.push(groupByObj[key])
                        }
                        groupedByData.push(row)
                    }
                    subCaption=newCols[1];
                }
                groupedByCols=Array(cols[0]).concat(groupedByCols);
                renderCols=groupedByCols;
                renderData=groupedByData;
            }

            $('#' + targetId).empty();
            for (var i = 0; i < renderData.length; i++) {
                for (var j = 1; j < renderCols.length; j++) {
                    if (!isNaN(renderData[i][j])) {
                        renderData[i][j] = parseFloat(renderData[i][j]);
                    }
                }
            }
        }
        return eval('chart_render_' + func + '(targetId,renderCols,renderData,width,height,subCaption);');
    }
}

function chart_render_line(targetId, cols, data, width, height,subCaption) {
    var category = [];
    for (var i = 0; i < data.length; i++) {
        category.push({"label": data[i][0]})
    }
    var dataset = []
    for (var i = 1; i < cols.length; i++) {
        var dataEntry = {}
        dataEntry['seriesname'] = cols[i]
        var dataValues = []
        for (var j = 0; j < data.length; j++) {
            dataValues.push({"value": data[j][i]})
        }
        dataEntry['data'] = dataValues
        dataset.push(dataEntry)
    }
    return new FusionCharts({
        type: 'msline',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "usePlotGradientColor": 0,
                "bgColor": "#ffffff",
                "showBorder": "1",
                "showHoverEffect": "1",
                "showCanvasBorder": "0",
                "plotBorderAlpha": "10",
                "legendBorderAlpha": "0",
                "legendShadow": "0",
                "showValues": "0",
                "showXAxisLine": "1",
                "xAxisLineColor": "#999999",
                "divlineColor": "#999999",
                "divLineIsDashed": "1",
                "showAlternateVGridColor": "0",
                "subcaptionFontBold": "0",
                "subcaptionFontSize": "14"
            },
            "categories": [{"category": category}],
            "dataset": dataset,
        }
    }).render();
}

function chart_render_col(targetId, cols, data, width, height,subCaption) {
    var category = [];
    for (var i = 0; i < data.length; i++) {
        category.push({"label": data[i][0]})
    }
    var dataset = []
    for (var i = 1; i < cols.length; i++) {
        var dataEntry = {}
        dataEntry['seriesname'] = cols[i]
        var dataValues = []
        for (var j = 0; j < data.length; j++) {
            dataValues.push({"value": data[j][i]})
        }
        dataEntry['data'] = dataValues
        dataset.push(dataEntry)
    }
    return new FusionCharts({
        type: 'mscolumn2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "usePlotGradientColor": 0,
                "bgColor": "#ffffff",
                "showBorder": "1",
                "showHoverEffect": "1",
                "showCanvasBorder": "0",
                "plotBorderAlpha": "10",
                "legendBorderAlpha": "0",
                "legendShadow": "0",
                "showValues": "0",
                "showXAxisLine": "1",
                "xAxisLineColor": "#999999",
                "divlineColor": "#999999",
                "divLineIsDashed": "1",
                "showAlternateVGridColor": "0",
                "subcaptionFontBold": "0",
                "subcaptionFontSize": "14"
            },
            "categories": [{"category": category}],
            "dataset": dataset,
        }
    }).render();
}

function chart_render_bar(targetId, cols, data, width, height,subCaption) {
    var category = [];
    for (var i = 0; i < data.length; i++) {
        category.push({"label": data[i][0]})
    }
    var dataset = []
    for (var i = 1; i < cols.length; i++) {
        var dataEntry = {}
        dataEntry['seriesname'] = cols[i]
        var dataValues = []
        for (var j = 0; j < data.length; j++) {
            dataValues.push({"value": data[j][i]})
        }
        dataEntry['data'] = dataValues
        dataset.push(dataEntry)
    }
    return new FusionCharts({
        type: 'msbar2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "valueFontColor": "#ffffff",
                "usePlotGradientColor": 0,
                "bgColor": "#ffffff",
                "showBorder": "1",
                "showHoverEffect": "1",
                "showCanvasBorder": "0",
                "plotBorderAlpha": "10",
                "legendBorderAlpha": "0",
                "legendShadow": "0",
                "placevaluesInside": "1",
                "showXAxisLine": "1",
                "xAxisLineColor": "#999999",
                "divlineColor": "#999999",
                "divLineIsDashed": "1",
                "showAlternateVGridColor": "0",
                "subcaptionFontBold": "0",
                "subcaptionFontSize": "14"
            },
            "categories": [{"category": category}],
            "dataset": dataset,
        }
    }).render();

}

function chart_render_area(targetId, cols, data, width, height,subCaption) {
    var category = [];
    for (var i = 0; i < data.length; i++) {
        category.push({"label": data[i][0]})
    }
    var dataset = []
    for (var i = 1; i < cols.length; i++) {
        var dataEntry = {}
        dataEntry['seriesname'] = cols[i]
        var dataValues = []
        for (var j = 0; j < data.length; j++) {
            dataValues.push({"value": data[j][i]})
        }
        dataEntry['data'] = dataValues
        dataset.push(dataEntry)
    }
    return new FusionCharts({
        type: 'msarea',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "usePlotGradientColor": 0,
                "bgColor": "#ffffff",
                "showBorder": "1",
                "showHoverEffect": "1",
                "showCanvasBorder": "0",
                "plotBorderAlpha": "10",
                "legendBorderAlpha": "0",
                "legendShadow": "0",
                "showValues": "0",
                "showXAxisLine": "1",
                "xAxisLineColor": "#999999",
                "divlineColor": "#999999",
                "divLineIsDashed": "1",
                "showAlternateVGridColor": "0",
                "subcaptionFontBold": "0",
                "subcaptionFontSize": "14"
            },
            "categories": [{"category": category}],
            "dataset": dataset,
        }
    }).render();
}

function chart_render_stack_col(targetId, cols, data, width, height,subCaption) {
    var category = [];
    for (var i = 0; i < data.length; i++) {
        category.push({"label": data[i][0]})
    }
    var dataset = []
    for (var i = 1; i < cols.length; i++) {
        var dataEntry = {}
        dataEntry['seriesname'] = cols[i]
        var dataValues = []
        for (var j = 0; j < data.length; j++) {
            dataValues.push({"value": data[j][i]})
        }
        dataEntry['data'] = dataValues
        dataset.push(dataEntry)
    }
    return new FusionCharts({
        type: 'stackedcolumn2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "showsum": "1",
                "usePlotGradientColor": 0,
                "bgColor": "#ffffff",
                "showBorder": "1",
                "showHoverEffect": "1",
                "showCanvasBorder": "0",
                "plotBorderAlpha": "10",
                "legendBorderAlpha": "0",
                "legendShadow": "0",
                "showValues": "0",
                "showXAxisLine": "1",
                "xAxisLineColor": "#999999",
                "divlineColor": "#999999",
                "divLineIsDashed": "1",
                "showAlternateVGridColor": "0",
                "subcaptionFontBold": "0",
                "subcaptionFontSize": "14"
            },
            "categories": [{"category": category}],
            "dataset": dataset,
        }
    }).render();
}

function chart_render_stack_bar(targetId, cols, data, width, height,subCaption) {
    var category = [];
    for (var i = 0; i < data.length; i++) {
        category.push({"label": data[i][0]})
    }
    var dataset = []
    for (var i = 1; i < cols.length; i++) {
        var dataEntry = {}
        dataEntry['seriesname'] = cols[i]
        var dataValues = []
        for (var j = 0; j < data.length; j++) {
            dataValues.push({"value": data[j][i]})
        }
        dataEntry['data'] = dataValues
        dataset.push(dataEntry)
    }
    return new FusionCharts({
        type: 'stackedbar2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "showsum": "1",
                "theme": "zune",
                "usePlotGradientColor": 0,
                "bgColor": "#ffffff",
                "showBorder": "1",
                "showHoverEffect": "1",
                "showCanvasBorder": "0",
                "plotBorderAlpha": "10",
                "legendBorderAlpha": "0",
                "legendShadow": "0",
                "showValues": "0",
                "showXAxisLine": "1",
                "xAxisLineColor": "#999999",
                "divlineColor": "#999999",
                "divLineIsDashed": "1",
                "showAlternateVGridColor": "0",
                "subcaptionFontBold": "0",
                "subcaptionFontSize": "14"
            },
            "categories": [{"category": category}],
            "dataset": dataset,
        }
    }).render();
}

function chart_render_stack_area(targetId, cols, data, width, height,subCaption) {
    var category = [];
    for (var i = 0; i < data.length; i++) {
        category.push({"label": data[i][0]})
    }
    var dataset = []
    for (var i = 1; i < cols.length; i++) {
        var dataEntry = {}
        dataEntry['seriesname'] = cols[i]
        var dataValues = []
        for (var j = 0; j < data.length; j++) {
            dataValues.push({"value": data[j][i]})
        }
        dataEntry['data'] = dataValues
        dataset.push(dataEntry)
    }
    return new FusionCharts({
        type: 'stackedarea2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "usePlotGradientColor": 0,
                "bgColor": "#ffffff",
                "showBorder": "1",
                "showHoverEffect": "1",
                "showCanvasBorder": "0",
                "plotBorderAlpha": "10",
                "legendBorderAlpha": "0",
                "legendShadow": "0",
                "showValues": "0",
                "showXAxisLine": "1",
                "xAxisLineColor": "#999999",
                "divlineColor": "#999999",
                "divLineIsDashed": "1",
                "showAlternateVGridColor": "0",
                "subcaptionFontBold": "0",
                "subcaptionFontSize": "14"
            },
            "categories": [{"category": category}],
            "dataset": dataset,
        }
    }).render();
}

function chart_render_rpie(targetId, cols, data, width, height,subCaption) {
    var dataset = [];

    for (var i = 0; i < data.length; i++) {
        var label = data[i][0];
        var sum = 0;
        for (var j = 1; j < cols.length; j++) {
            sum += parseFloat(data[i][j])
        }
        var dataEntry = {}
        dataEntry['label'] = label
        dataEntry['value'] = sum
        dataset.push(dataEntry)
    }

    return new FusionCharts({
        type: 'pie2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "bgColor": "#ffffff",
                "showBorder": "1",
                "use3DLighting": "0",
                "showShadow": "0",
                "enableSmartLabels": "0",
                "startingAngle": "0",
                "showPercentValues": "1",
                "showPercentInTooltip": "0",
                "decimals": "1",
                "captionFontSize": "14",
                "subcaptionFontSize": "14",
                "subcaptionFontBold": "0",
                "toolTipColor": "#ffffff",
                "toolTipBorderThickness": "0",
                "toolTipBgColor": "#000000",
                "toolTipBgAlpha": "80",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect": "1",
                "showLegend": "1",
                "legendBgColor": "#ffffff",
                "legendBorderAlpha": '0',
                "legendShadow": '0',
                "legendItemFontSize": '10',
                "legendItemFontColor": '#666666'
            },
            "data": dataset,
        }
    }).render();
}

function chart_render_rdonut(targetId, cols, data, width, height,subCaption) {
    var dataset = [];

    for (var i = 0; i < data.length; i++) {
        var label = data[i][0];
        var sum = 0;
        for (var j = 1; j < cols.length; j++) {
            sum += parseFloat(data[i][j])
        }
        var dataEntry = {}
        dataEntry['label'] = label
        dataEntry['value'] = sum
        dataset.push(dataEntry)
    }

    return new FusionCharts({
        type: 'doughnut2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "bgColor": "#ffffff",
                "showBorder": "1",
                "use3DLighting": "0",
                "showShadow": "0",
                "enableSmartLabels": "0",
                "startingAngle": "0",
                "showPercentValues": "1",
                "showPercentInTooltip": "0",
                "decimals": "1",
                "captionFontSize": "14",
                "subcaptionFontSize": "14",
                "subcaptionFontBold": "0",
                "toolTipColor": "#ffffff",
                "toolTipBorderThickness": "0",
                "toolTipBgColor": "#000000",
                "toolTipBgAlpha": "80",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect": "1",
                "showLegend": "1",
                "legendBgColor": "#ffffff",
                "legendBorderAlpha": '0',
                "legendShadow": '0',
                "legendItemFontSize": '10',
                "legendItemFontColor": '#666666'
            },
            "data": dataset,
        }
    }).render();
}

function chart_render_rfunnel(targetId, cols, data, width, height,subCaption) {
    var dataset = [];

    for (var i = 0; i < data.length; i++) {
        var label = data[i][0];
        var sum = 0;
        for (var j = 1; j < cols.length; j++) {
            sum += parseFloat(data[i][j])
        }
        var dataEntry = {}
        dataEntry['label'] = label
        dataEntry['value'] = sum
        dataset.push(dataEntry)
    }

    return new FusionCharts({
        type: 'funnel',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "is2D": "1",
                "bgColor": "#ffffff",
                "showBorder": "1",
                "use3DLighting": "0",
                "showShadow": "0",
                "enableSmartLabels": "0",
                "startingAngle": "0",
                "showPercentValues": "1",
                "showPercentInTooltip": "0",
                "decimals": "1",
                "captionFontSize": "14",
                "subcaptionFontSize": "14",
                "subcaptionFontBold": "0",
                "toolTipColor": "#ffffff",
                "toolTipBorderThickness": "0",
                "toolTipBgColor": "#000000",
                "toolTipBgAlpha": "80",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect": "1",
                "showLegend": "1",
                "legendBgColor": "#ffffff",
                "legendBorderAlpha": '0',
                "legendShadow": '0',
                "legendItemFontSize": '10',
                "legendItemFontColor": '#666666'
            },
            "data": dataset,
        }
    }).render();
}

function chart_render_cpie(targetId, cols, data, width, height,subCaption) {
    var dataset = [];

    for (var i = 1; i < cols.length; i++) {
        var label = cols[i];
        var sum = 0;
        for (var j = 0; j < data.length; j++) {
            sum += parseFloat(data[j][i])
        }
        var dataEntry = {}
        dataEntry['label'] = label
        dataEntry['value'] = sum
        dataset.push(dataEntry)
    }

    return new FusionCharts({
        type: 'pie2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "bgColor": "#ffffff",
                "showBorder": "1",
                "use3DLighting": "0",
                "showShadow": "0",
                "enableSmartLabels": "0",
                "startingAngle": "0",
                "showPercentValues": "1",
                "showPercentInTooltip": "0",
                "decimals": "1",
                "captionFontSize": "14",
                "subcaptionFontSize": "14",
                "subcaptionFontBold": "0",
                "toolTipColor": "#ffffff",
                "toolTipBorderThickness": "0",
                "toolTipBgColor": "#000000",
                "toolTipBgAlpha": "80",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect": "1",
                "showLegend": "1",
                "legendBgColor": "#ffffff",
                "legendBorderAlpha": '0',
                "legendShadow": '0',
                "legendItemFontSize": '10',
                "legendItemFontColor": '#666666'
            },
            "data": dataset,
        }
    }).render();
}

function chart_render_cdonut(targetId, cols, data, width, height,subCaption) {
    var dataset = [];

    for (var i = 1; i < cols.length; i++) {
        var label = cols[i];
        var sum = 0;
        for (var j = 0; j < data.length; j++) {
            sum += parseFloat(data[j][i])
        }
        var dataEntry = {}
        dataEntry['label'] = label
        dataEntry['value'] = sum
        dataset.push(dataEntry)
    }

    return new FusionCharts({
        type: 'doughnut2d',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "theme": "zune",
                "bgColor": "#ffffff",
                "showBorder": "1",
                "use3DLighting": "0",
                "showShadow": "0",
                "enableSmartLabels": "0",
                "startingAngle": "0",
                "showPercentValues": "1",
                "showPercentInTooltip": "0",
                "decimals": "1",
                "captionFontSize": "14",
                "subcaptionFontSize": "14",
                "subcaptionFontBold": "0",
                "toolTipColor": "#ffffff",
                "toolTipBorderThickness": "0",
                "toolTipBgColor": "#000000",
                "toolTipBgAlpha": "80",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect": "1",
                "showLegend": "1",
                "legendBgColor": "#ffffff",
                "legendBorderAlpha": '0',
                "legendShadow": '0',
                "legendItemFontSize": '10',
                "legendItemFontColor": '#666666'
            },
            "data": dataset,
        }
    }).render();
}

function chart_render_cfunnel(targetId, cols, data, width, height,subCaption) {
    var dataset = [];

    for (var i = 1; i < cols.length; i++) {
        var label = cols[i];
        var sum = 0;
        for (var j = 0; j < data.length; j++) {
            sum += parseFloat(data[j][i])
        }
        var dataEntry = {}
        dataEntry['label'] = label
        dataEntry['value'] = sum
        dataset.push(dataEntry)
    }

    return new FusionCharts({
        type: 'funnel',
        renderAt: targetId,
        width: width,
        height: height,
        dataFormat: 'json',
        dataSource: {
            "chart": {
                subCaption: subCaption,
                "animation": 0,
                "is2D": "1",
                "theme": "zune",
                "bgColor": "#ffffff",
                "showBorder": "1",
                "use3DLighting": "0",
                "showShadow": "0",
                "enableSmartLabels": "0",
                "startingAngle": "0",
                "showPercentValues": "1",
                "showPercentInTooltip": "0",
                "decimals": "1",
                "captionFontSize": "14",
                "subcaptionFontSize": "14",
                "subcaptionFontBold": "0",
                "toolTipColor": "#ffffff",
                "toolTipBorderThickness": "0",
                "toolTipBgColor": "#000000",
                "toolTipBgAlpha": "80",
                "toolTipBorderRadius": "2",
                "toolTipPadding": "5",
                "showHoverEffect": "1",
                "showLegend": "1",
                "legendBgColor": "#ffffff",
                "legendBorderAlpha": '0',
                "legendShadow": '0',
                "legendItemFontSize": '10',
                "legendItemFontColor": '#666666'
            },
            "data": dataset,
        }
    }).render();
}

