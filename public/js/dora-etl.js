var etlOriginalTitle = $('#etlPageHeader').html()
var etlDataTable = null
var etlGUI = false
var etlLastResult = null
var tableTypeMapping = null
var tableTypeColorMapping = {}
var group = null
var groupEntities = null

sectionMap["etl"] = (function () {
    return {
        setup: setup,
        beforeShowSection: beforeShowSection,
        showSection: showSection,
        showEntityForm: showEntityForm,
        closeEntityForm: closeEntityForm,
        create:create,
        update:update,
        remove:remove,
        showGroupEntityForm: showGroupEntityForm,
        createGroup:createGroup,
        updateGroup:updateGroup,
        removeGroup:removeGroup,
    }

    function setup() {
        $(document).on('change', '#groupOption', function() {
            $(this).find('option[value=""]').remove();
            var thisEl = this
            var entity = $.grep(groupEntities, function(entity) {
                return entity.name == thisEl.value;
            })[0];
            group = entity.name
            $('.dagItem').show();
            var cronExpression = "not set"
            if(entity.cronExpression) {
                cronExpression = entity.cronExpression
            }
            $('#groupSchedule').text(cronExpression);
            displayEntities(group);
        })

        $(document).on('click', '#groupImportBtn', function() {
            $('#etlImportTableName').html(group);
            $('#importType').text('Group');
            setTimeout(function () {$('#etlImportTableConfirm').focus()}, 350);
            event.stopPropagation();
            $('#etlImportTableModal').modal('show');
        })

        $(document).on('click', '#groupEditBtn', function() {
            showGroupEntityForm(group)
        })

        $(document).on('click', '.newEtlOption', function () {
            var className = $(this).attr('id');
            showEntityForm(className, true);
        })

        $(document).on('click', '#newEtlGroupBtn', function () {
            showGroupEntityForm(null);
        })

        $( "#etlGuiBtn" ).click(function() {
            etlGUI=true;
            $("#etlGuiBtn").attr('disabled','disabled');
            $("#etlTableBtn").removeAttr('disabled');
            showEntityTable();
        });

        $( "#etlTableBtn" ).click(function() {
            etlGUI=false;
            $("#etlTableBtn").attr('disabled','disabled');
            $("#etlGuiBtn").removeAttr('disabled');
            showEntityTable();
        });


        $("#etlSelectTxDate").on('dp.change', function(e){
            $("#etlImportTxDate").html($("#etlSelectTxDate").find("input").val())
        })

        $("#etlImportTxDate").html($("#etlSelectTxDate").find("input").val())

        $("#etlImportTableConfirm").on('click',function(){
            etlImportTable();
        })
    }



    function etlImportTable(){
        var txDate=$("#etlImportTxDate").html();
        var name=$("#etlImportTableName").html();
        var importType = $('#importType').text();
        var button=document.getElementById('etlImport-'+name);
        if(button) {
            button.disabled=true
            button.innerHTML= '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
            setTimeout(function(){
                button.innerHTML="<i class='fa fa-file-text-o fa-1'></i><i class='fa fa-arrow-right fa-fw'></i><i class='fa fa-database fa-1'></i>";
                button.disabled=false;
            },1000);
        }
        var url = null
        if(importType.toLowerCase() == 'group') {
            url = "/etlGroupImport.json?group=" + name+"&txDate="+txDate
        } else {
            url = "/etlImport.json?tableName=" + name+"&txDate="+txDate
        }
        $.ajax({
            type: "GET",
            url: url,
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
        })
    }

    function beforeShowSection() {
        if(!getUiEntityOpened("etl")){
            $('#etlTableDiv').fadeOut(0);
            $('#etlListLoading').fadeIn(1000);
        }
    }

    function showSection() {
        showEtlContent();
        
        if(getUiLastFocus("etl")!=null){
            getUiLastFocus("etl").focus();
        }
    }

    function create(className){
        document.getElementById("etlApplyBtn").disabled=true;
        document.getElementById("etlApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('etl-new');
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
            url: "/etl.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("etlApplyBtn").disabled=false;
            document.getElementById("etlApplyBtn").innerHTML="Apply"
        })
    }

    function update(className){
        document.getElementById("etlApplyBtn").disabled=true;
        document.getElementById("etlApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('etl-edit');
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
            url: "/etl.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("etlApplyBtn").disabled=false;
            document.getElementById("etlApplyBtn").innerHTML="Apply"
        })
    }

    function remove(name){
        document.getElementById("etlDeleteBtn").disabled=true;
        document.getElementById("etlDeleteBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';
        $.ajax({
            type: "DELETE",
            url: "/etl.json?name=" + name,
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("etlDeleteBtn").disabled=false;
            document.getElementById("etlDeleteBtn").innerHTML="Delete"
        })
    }

    function showEntityForm(target, create, duplicatedEntityName) {
        setUiEntityOpened("etl",true)
        $('#etlTableDiv').fadeOut(0,function(){
            $('#etlListLoading').fadeIn(1000);
            var url = 'etlDataset.html?group=' + group + '&' + (create ? 'class=' + target + (duplicatedEntityName ? '&duplicatedEntityName=' + duplicatedEntityName : '') : 'name=' + target)
            $('#etlEntity').load(url, function () {
                $('#etlListLoading').stop();
                $('#etlListLoading').fadeOut(100, function () {
                    if (create) {
                        $('#etlPageHeader').html('<i class="fa fa-plug fa-fw"></i> <a href="#etl" onclick="sectionMap[\'etl\'].closeEntityForm();">ETL</a>' + '<i class="fa fa-angle-right fa-fw"></i> Entity<i class="fa fa-angle-right fa-fw"></i> New')
                        $('#etlEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('etl-new'))[0].focus();
                    } else {
                        $('#etlPageHeader').html('<i class="fa fa-plug fa-fw"></i> <a href="#etl" onclick="sectionMap[\'etl\'].closeEntityForm();">ETL</a>' + '<i class="fa fa-angle-right fa-fw"></i> ' + group + '<i class="fa fa-angle-right fa-fw"></i> ' + target)
                        $('#etlEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('etl-edit'))[0].focus();
                    }
                });
            })
        })
    }

    function closeEntityForm() {
        setUiEntityOpened("etl",false)
        for (var i = 0; i < getUiBuffer("etl").length; i++) {
            getUiBuffer("etl")[i].destroy();
        }
        $('#etlEntity').empty();
        $('#etlEntity').hide();
        if(etlOriginalTitle != null) {
            $('#etlPageHeader').html(etlOriginalTitle)
        }
        $('#etlListLoading').fadeIn(1000);
        showEtlContent();
    }

    function showGroupEntityForm(target, duplicatedEntityName) {
        setUiEntityOpened("etl",true)
        $('#etlTableDiv').fadeOut(0,function(){
            $('#etlListLoading').fadeIn(1000);
            var isFirstParam = true;
            var url = 'etlGroup.html';
            if(target) {
                url += '?name=' + target
                isFirstParam = false;
            }
            if(duplicatedEntityName) {
                if(isFirstParam) {
                    url += '?duplicatedEntityName=' + duplicatedEntityName
                } else {
                    url += '&duplicatedEntityName=' + duplicatedEntityName
                }
            }
            $('#etlEntity').load(url, function () {
                $('#etlListLoading').stop();
                $('#etlListLoading').fadeOut(100, function () {
                    if (!target) {
                        $('#etlPageHeader').html('<i class="fa fa-sitemap fa-fw"></i> <a href="#etl" onclick="sectionMap[\'etl\'].closeEntityForm();">ETL</a>' + '<i class="fa fa-angle-right fa-fw"></i> Group<i class="fa fa-angle-right fa-fw"></i> New')
                        $('#etlEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('etl-new'))[0].focus();
                    } else {
                        $('#etlPageHeader').html('<i class="fa fa-sitemap fa-fw"></i> <a href="#etl" onclick="sectionMap[\'etl\'].closeEntityForm();">ETL</a>' + '<i class="fa fa-angle-right fa-fw"></i> ' + target)
                        $('#etlEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('etl-edit'))[0].focus();
                    }
                });
            })
        })
    }

    function createGroup(className){
        document.getElementById("etlApplyBtn").disabled=true;
        document.getElementById("etlApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('etl-new');
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
            url: "/etlGroup.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("etlApplyBtn").disabled=false;
            document.getElementById("etlApplyBtn").innerHTML="Apply"
        })
    }

    function updateGroup(className){
        document.getElementById("etlApplyBtn").disabled=true;
        document.getElementById("etlApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('etl-edit');
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
            url: "/etlGroup.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("etlApplyBtn").disabled=false;
            document.getElementById("etlApplyBtn").innerHTML="Apply"
        })
    }

    function removeGroup(name){
        document.getElementById("etlDeleteBtn").disabled=true;
        document.getElementById("etlDeleteBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';
        $.ajax({
            type: "DELETE",
            url: "/etlGroup.json?name=" + name,
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("etlDeleteBtn").disabled=false;
            document.getElementById("etlDeleteBtn").innerHTML="Delete"
        })
    }

    function showEtlContent() {
        $('#etlListLoading').stop();

        if(getUiEntityOpened("etl")){
            return;
        }

        $('#etlListLoading').fadeOut(100);
        $('#etlTableDiv').fadeIn(100);

        $.getJSON('/etlGroupList.json', function (result) {
            $('#groupOption').find('option').remove();
            $('#groupOption').append('<option value="">--please select one--</option>')
            groupEntities = result;
            $.each(groupEntities, function(idx, entity){
                var optionHTML = '<option value=' + entity.name + '>' + entity.name + '</option>'
                $('#groupOption').append(optionHTML)
            });
            if(group) {
                $('#groupOption option[value=""]').remove();
                $('#groupOption option[value="' + group + '"]').prop('selected', true);
                $('#groupOption').trigger('change');
            }
        });
    }

    function displayEntities(group) {
        if(getUiEntityOpened("etl")){
            return;
        }

        if (etlDataTable) {
            etlDataTable.destroy();
            etlDataTable=null;
        }

        $('#etlListTable').hide();
        $( "#etlListTable" ).empty();

        if(!etlGUI) {
            $.getJSON('/etlList.json?group=' + group , function (result) {
                $('#entityTableDiv').show();
                $( "#etlListTable" ).css('margin-top','0px');
                $('#etlTableDiv').fadeIn(100, function () {
                    $('#newEtlTableTypeSelect').empty();
                    $('#etlListTable').empty();
                    tableTypeColorMapping = {};
                    for (var key in result.etlTypeList) {
                        var value = result.etlTypeList[key];
                        tableTypeColorMapping[key] = value.color;
                        $('#newEtlTableTypeSelect').append(
                            '<li><a id="' + key + '" href="#etl" class="newEtlOption">'
                            + '<span style="color:'+value.color+'">▣ </span>'+ value.name + '</a></li>');
                    }

                    var etlList = $.map(result.etlList, function (etl, idx) {

                        var etlImportClick=
                            "$(\"#etlImportTableName\").html(\""+etl.name+"\");"+
                            "$(\"#importType\").text(\"Entity\");" +
                            "setTimeout(function () {$(\"#etlImportTableConfirm\").focus()}, 350)"

                        var onClick="sectionMap[\"etl\"].showEntityForm(\""+etl.name+"\",false)";
                        etl.clickableName =
                            "<a class='editBtn' onclick='"+onClick+"' style='cursor:pointer;' href='#etl'"
                            + etl.name + "'> "+ etl.name + "</a>";
                        etl.import="<button class='btn btn-default btn-table' id='etlImport-"+etl.name+"' data-toggle='modal' data-target='#etlImportTableModal' onclick='"+etlImportClick+"' style='margin-left:10px;'>" +
                            "<i class='fa fa-file-text-o fa-f1'></i><i class='fa fa-arrow-right fa-fw'></i><i class='fa fa-database fa-f1'></i>" +
                            "</button>";
                        return etl
                    })
                    if (etlDataTable) {
                        etlDataTable.destroy();
                    }

                    etlDataTable = $('#etlListTable').DataTable({
                        data: etlList,
                        paging: false,
                        bStateSave: true,
                        columnDefs: [{
                            defaultContent: "",
                            targets: "_all"
                        }],
                        columns: [
                            {title: 'Name', data: 'clickableName'},
                            {title: 'Type', data: 'type',},
                            {title: 'Last TxDate (System)', data: 'lastTxDate'},
                            {title: 'Import',data: 'import' },
                        ]
                    });

                    if(sectionMap["etlClearSearch"]==null){
                        sectionMap["etlClearSearch"]=true;
                        etlDataTable.search('').columns().search( '' ).draw();
                    }
                });
                $("#etlGuiBtn").show();
            })
        } else {
            $.getJSON('/etlList.json?group=' + group + '&includeDep=true', function (result) {
                $( "#etlListTable" ).css('margin-top','30px');
                $('#etlTableDiv').fadeIn(100, function () {
                    $('#newEtlTableTypeSelect').empty();
                    $('#etlListTable').empty();
                    tableTypeColorMapping = {};
                    for (var key in result.etlTypeList) {
                        var value = result.etlTypeList[key]
                        tableTypeColorMapping[key] = value.color;
                        $('#newEtlTableTypeSelect').append(
                            '<li><a id="' + key + '" href="#etl" class="newEtlOption">'
                            + '<span style="color:'+value.color+'">▣ </span>'+ value.name + '</a></li>');

                    }
                    etlLastResult=result.etlList
                    $.getJSON('/tableTypeMapping.json', function (rs) {
                        tableTypeMapping = rs;
                        renderFilteredSankey(etlLastResult, rs, tableTypeColorMapping);
                    })
                });
                $("#etlTableBtn").show();
            });
        }
        $('#etlListTable').show();
    }

    function showEntityTable() {
        $("#etlGuiBtn").hide();
        $("#etlTableBtn").hide();

        if(!etlGUI){
            $('#nodeFilterSpan').hide();
            $('#tableImportSpan').show();
        }else{
            $('#nodeFilterSpan').show();
            $('#tableImportSpan').show();
        }

        displayEntities(group);
    }

})()


function renderFilteredSankey(tableList){
    $('#etlListTable').empty();
    var filter=$("#etlGuiFilter").val();
    if(filter.length>0){

        var tmpResult=jQuery.extend(true, [], tableList);

        tmpResult.map(function (x) {
            //unrelated, discard its children
            if(x.name.indexOf(filter)<0){
                x.dependencies=[];
            }
            return x;
        });

        tmpResult=tmpResult.filter(function (x) {
            if(x.name.indexOf(filter)>=0)
                return true;
            var valid=false;
            tmpResult.map(function(tmp){
                if(tmp.dependencies.indexOf(x.name)>=0){
                    valid=true;
                }
            })
            return valid
        });

        //remove unlinked nodes
        var linkedNodes={}

        tmpResult.map(function(x){
           if(x.dependencies!=null && x.dependencies.length>0){
               linkedNodes[x.name]=true;
               x.dependencies.map(function(dep){
                   var matchedEtl = etlLastResult.filter(function(x) {
                       return x.name == dep && x['@class'].indexOf['.etl.'] != -1
                   })
                   if(matchedEtl.length > 0){
                       linkedNodes[dep]=true;
                   }
               })
           }
        });

        var linkedNodeNames=Object.keys(linkedNodes);

        tmpResult=tmpResult.filter(function(x){
            return linkedNodeNames.indexOf(x.name)>=0;
        });

        renderSankey(tmpResult, tableTypeMapping, tableTypeColorMapping);
    }else{
        renderSankey(etlLastResult, tableTypeMapping, tableTypeColorMapping);
    }
}

$("#etlGuiFilter").on('input', function() {
    renderFilteredSankey(etlLastResult);
});