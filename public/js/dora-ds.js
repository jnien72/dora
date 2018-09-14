var dsOriginalTitle = $('#dsPageHeader').html()
var dsDataTable = null

sectionMap["ds"] = (function () {
    return {
        setup: setup,
        beforeShowSection: beforeShowSection,
        showSection: showSection,
        showEntityForm: showEntityForm,
        closeEntityForm: closeEntityForm,
        update:update,
        create:create,
        remove:remove,
    }

    function setup() {
        $(document).on('click', '.newDsOption', function () {
            var className = $(this).attr('id');
            showEntityForm(className, true);
        })

        $("#dsSelectTxDate").on('dp.change', function(e){
            $("#dsImportTxDate").html($("#dsSelectTxDate").find("input").val())
        })

        $("#dsImportTxDate").html($("#dsSelectTxDate").find("input").val())

        $("#dsImportTableConfirm").on('click',function(){
            dsImportTable();
        })
    }

    function dsImportTable(){
        var txDate=$("#dsImportTxDate").html()
        var tableName=$("#dsImportTableName").html()
        var button=document.getElementById('dsImport-'+tableName)
        var tableNameWithoutPrefix=tableName.substring(3)
        button.disabled=true
        button.innerHTML= '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
        setTimeout(function(){
            button.innerHTML="<i class='fa fa-file-text-o fa-f1'></i><i class='fa fa-arrow-right fa-fw'></i><i class='fa fa-database fa-f1'></i>";
            button.disabled=false;
        },1000);
        $.ajax({
            type: "GET",
            url: "/dsImport.json?tableName=" + tableNameWithoutPrefix+"&txDate="+txDate,
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
        })
    }

    function beforeShowSection() {
        if(!getUiEntityOpened("ds")){
            $('#dsTableDiv').fadeOut(0);
            $('#dsListLoading').fadeIn(1000);
        }
    }

    function showSection() {
        if(!getUiEntityOpened("ds")){
            showDsTable();
        }
        if(getUiLastFocus("ds")!=null){
            getUiLastFocus("ds").focus();

        }
    }

    function create(className){
        document.getElementById("dsApplyBtn").disabled=true;
        document.getElementById("dsApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('ds-new');
        var inputFieldList = getElementsByTagNames('input', element);
        var data={}
        for(var i=0; i<inputFieldList.length;i++){
            var key=inputFieldList[i].id
            var value=inputFieldList[i].value
            data[key]=value
        }
        var selectFieldList = getElementsByTagNames('select', element);
        for(var i=0; i<selectFieldList.length;i++){
            var key=selectFieldList[i].id
            var selectedOptions = $(selectFieldList[i]).find('option:selected')
            var value = $.map(selectedOptions, function(option) {
                return option.value
            }).join();
            data[key]=value
        }
        $.ajax({
            type: "POST",
            url: "/ds.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
            document.getElementById("dsApplyBtn").disabled=false;
            document.getElementById("dsApplyBtn").innerHTML="Apply"
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("dsApplyBtn").disabled=false;
            document.getElementById("dsApplyBtn").innerHTML="Apply"
        })
    }

    function update(className){
        document.getElementById("dsApplyBtn").disabled=true;
        document.getElementById("dsApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('ds-edit');
        var inputFieldList = getElementsByTagNames('input', element);
        var data={}
        for(var i=0; i<inputFieldList.length;i++){
            var key=inputFieldList[i].id
            var value=inputFieldList[i].value
            data[key]=value
        }
        var selectFieldList = getElementsByTagNames('select', element);
        for(var i=0; i<selectFieldList.length;i++){
            var key=selectFieldList[i].id
            var selectedOptions = $(selectFieldList[i]).find('option:selected')
            var value = $.map(selectedOptions, function(option) {
                return option.value
            }).join();
            data[key]=value
        }
        $.ajax({
            type: "PUT",
            url: "/ds.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("dsApplyBtn").disabled=false;
            document.getElementById("dsApplyBtn").innerHTML="Apply"
        })
    }

    function remove(name){
        document.getElementById("dsDeleteBtn").disabled=true;
        document.getElementById("dsDeleteBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';
        $.ajax({
            type: "DELETE",
            url: "/ds.json?name=" + name,
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
            document.getElementById("dsDeleteBtn").disabled=false;
            document.getElementById("dsDeleteBtn").innerHTML="Delete"
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("dsDeleteBtn").disabled=false;
            document.getElementById("dsDeleteBtn").innerHTML="Delete"
        })
    }

    function showEntityForm(target, create, duplicatedEntityName) {
        setUiEntityOpened("ds",true)
        $('#dsTableDiv').fadeOut(0,function(){
            $('#dsListLoading').fadeIn(1000);
            $('#dsEntity').load('dsEntity.html?' + (create ? 'class=' + target + (duplicatedEntityName ? '&duplicatedEntityName=' + duplicatedEntityName : '') : 'name=' + target), function () {
                $('#dsListLoading').stop();
                $('#dsListLoading').fadeOut(100, function () {
                    if (create) {
                        $('#dsPageHeader').html('<i class="fa fa-database fa-fw"></i> <a href="#ds" onclick="sectionMap[\'ds\'].closeEntityForm();">Data Source</a>' + '<i class="fa fa-angle-right fa-fw"></i> New')
                        $('#dsEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('ds-new'))[0].focus();
                    } else {
                        $('#dsPageHeader').html('<i class="fa fa-database fa-fw"></i> <a href="#ds" onclick="sectionMap[\'ds\'].closeEntityForm();">Data Source</a>' + '<i class="fa fa-angle-right fa-fw"></i> ' + target)
                        $('#dsEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('ds-edit'))[0].focus();
                    }

                });
            })
        });
    }

    function closeEntityForm() {
        setUiEntityOpened("ds",false)
        for (var i = 0; i < getUiBuffer("ds").length; i++) {
            getUiBuffer("ds")[i].destroy();
        }
        $('#dsEntity').empty();
        $('#dsEntity').hide();
        if(dsOriginalTitle!=null){
            $('#dsPageHeader').html(dsOriginalTitle)
        }
        $('#dsListLoading').fadeIn(1000);
        showDsTable();
    }

    function showDsTable() {
        $.getJSON('/dsList.json', function (result) {
            if(getUiEntityOpened("ds")){
                return;
            }
            $('#dsListLoading').stop();
            $('#dsListLoading').fadeOut(100,function(){
                $('#dsTableDiv').fadeIn(100, function(){
                    $('#newDsTypeSelect').empty();
                    $('#dsListTable').empty();

                    for (var key in result.dsTypeList) {
                        var value=result.dsTypeList[key]
                        $('#newDsTypeSelect').append(
                            '<li><a id="'+key+'" href="#ds" class="newDsOption">'
                            +value+'</a></li>');
                    }
                    var dsList = $.map(result.dsList, function(ds,idx) {
                        var editClick="sectionMap[\"ds\"].showEntityForm(\""+ds.name+"\",false)";
                        ds.clickableName =
                            "<a class='editBtn' onclick='"+editClick+"' style='cursor:pointer;' href='#ds'"
                            + ds.name + "'> "+ds.name + "</a>";

                        var dsImportClick=
                            "$(\"#dsImportTableName\").html(\"ds."+ds.name+"\");"+
                            "setTimeout(function () {$(\"#dsImportTableConfirm\").focus()}, 350)"
                        if(ds.connectionType!='static'){
                            ds.import="<button class='btn btn-default btn-table' id='dsImport-ds."+ds.name+"' data-toggle='modal' data-target='#dsImportTableModal' onclick='"+dsImportClick+"' style='margin-left:10px;'>" +
                                "<i class='fa fa-file-text-o fa-f1'></i><i class='fa fa-arrow-right fa-fw'></i><i class='fa fa-database fa-f1'></i>" +
                                "</button>";
                        }else{
                            ds.import=""
                        }


                        return ds
                    })

                    if(dsDataTable) {
                        dsDataTable.destroy();
                    }

                    dsDataTable = $('#dsListTable').DataTable({
                        data: dsList,
                        paging: false,
                        bStateSave: true,
                        columnDefs: [{
                            defaultContent: "",
                            targets: "_all"
                        }],
                        columns: [
                            { title:'Name', data: 'clickableName' },
                            { title:'Type', data: 'connectionType', },
                            { title:'Connection', data: 'connectionName' },
                            { title:'Schedule', data: 'cronExpression' },
                            { title:'Last TxDate (system)',data: 'lastTxDate' },
                            { title:'Import',data: 'import' },
                        ]
                    });
                    if(sectionMap["dsClearSearch"]==null){
                        sectionMap["dsClearSearch"]=true;
                        dsDataTable.search('').columns().search( '' ).draw();
                    }
                });
            });
        })
    }
})()


