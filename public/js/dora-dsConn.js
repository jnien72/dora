var dsConnOriginalTitle = $('#dsConnPageHeader').html()
var dsConnDataTable = null

sectionMap["dsConn"] = (function () {
    return {
        setup: setup,
        beforeShowSection: beforeShowSection,
        showSection: showSection,
        showEntityForm: showEntityForm,
        closeEntityForm: closeEntityForm,
        create:create,
        update:update,
        remove:remove,
    }

    function setup() {
        $(document).on('click', '.newDsConnOption', function () {
            var className = $(this).attr('id');
            showEntityForm(className, true);
        })
    }

    function beforeShowSection() {
        if(!getUiEntityOpened("dsConn")){
            $('#dsConnTableDiv').fadeOut(0);
            $('#dsConnListLoading').fadeIn(1000);
        }
    }

    function showSection() {
        if(!getUiEntityOpened("dsConn")){
            showDsConnTable();
        }
        if(getUiLastFocus("dsConn")!=null){
            getUiLastFocus("dsConn").focus();
        }
    }

    function create(className){
        document.getElementById("dsConnApplyBtn").disabled=true;
        document.getElementById("dsConnApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('dsConn-new');
        var formFieldList = getElementsByTagNames('input,select',element);
        var data={}
        for(var i=0; i<formFieldList.length;i++){
            var key=formFieldList[i].id
            var value=formFieldList[i].value
            data[key]=value
        }
        $.ajax({
            type: "POST",
            url: "/dsConn.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
            document.getElementById("dsConnApplyBtn").disabled=false;
            document.getElementById("dsConnApplyBtn").innerHTML="Apply"
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("dsConnApplyBtn").disabled=false;
            document.getElementById("dsConnApplyBtn").innerHTML="Apply"
        })
    }

    function update(className){
        document.getElementById("dsConnApplyBtn").disabled=true;
        document.getElementById("dsConnApplyBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';

        var element = document.getElementById('dsConn-edit');
        var formFieldList = getElementsByTagNames('input,select',element);
        var data={}
        for(var i=0; i<formFieldList.length;i++){
            var key=formFieldList[i].id
            var value=formFieldList[i].value
            data[key]=value
        }
        $.ajax({
            type: "PUT",
            url: "/dsConn.json?className=" + className,
            data: JSON.stringify(data),
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
            document.getElementById("dsConnApplyBtn").disabled=false;
            document.getElementById("dsConnApplyBtn").innerHTML="Apply"
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("dsConnApplyBtn").disabled=false;
            document.getElementById("dsConnApplyBtn").innerHTML="Apply"
        })
    }

    function remove(name){
        document.getElementById("dsConnDeleteBtn").disabled=true;
        document.getElementById("dsConnDeleteBtn").innerHTML=
            '&nbsp;&nbsp;<img src="/img/loading.gif" style="width:16px;height;16px"/>&nbsp;&nbsp;';
        $.ajax({
            type: "DELETE",
            url: "/dsConn.json?name=" + name,
            contentType: "application/json charset=utf-8"
        }).success(function () {
            closeEntityForm();
            document.getElementById("dsConnDeleteBtn").disabled=false;
            document.getElementById("dsConnDeleteBtn").innerHTML="Delete"
        }).error(function (result) {
            Modal.show("<textarea " +
                "style='border: none; border-color: Transparent; white-space: pre; overflow-x: scroll; overflow-wrap: normal;width:100%; height:300px;'" +
                " disabled>" + result.responseText + "</textarea>", "ERROR");
            document.getElementById("dsConnDeleteBtn").disabled=false;
            document.getElementById("dsConnDeleteBtn").innerHTML="Delete"
        })
    }

    function showEntityForm(target, create, duplicatedEntityName) {
        console.log('duplicatedEntityName:' + duplicatedEntityName)
        setUiEntityOpened("dsConn",true)
        $('#dsConnTableDiv').fadeOut(0,function(){
            $('#dsConnListLoading').fadeIn(1000);
            $('#dsConnEntity').load('dsConnEntity.html?' + (create ? 'class=' + target + (duplicatedEntityName ? '&duplicatedEntityName=' + duplicatedEntityName : '') : 'name=' + target), function () {
                $('#dsConnListLoading').stop();
                $('#dsConnListLoading').fadeOut(100, function () {
                    if (create) {
                        $('#dsConnPageHeader').html('<i class="fa fa-plug fa-fw"></i> <a href="#dsConn" onclick="sectionMap[\'dsConn\'].closeEntityForm();">Connection</a>' + '<i class="fa fa-angle-right fa-fw"></i> New')
                        $('#dsConnEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('dsConn-new'))[0].focus();
                    } else {
                        $('#dsConnPageHeader').html('<i class="fa fa-plug fa-fw"></i> <a href="#dsConn" onclick="sectionMap[\'dsConn\'].closeEntityForm();">Connection</a>' + '<i class="fa fa-angle-right fa-fw"></i> ' + target)
                        $('#dsConnEntity').show();
                        getElementsByTagNames('input,select',document.getElementById('dsConn-edit'))[0].focus();
                    }
                });
            })
        })
    }

    function closeEntityForm() {
        setUiEntityOpened("dsConn",false)
        for (var i = 0; i < getUiBuffer("dsConn").length; i++) {
            getUiBuffer("dsConn")[i].destroy();
        }
        $('#dsConnEntity').empty();
        $('#dsConnEntity').hide();
        if(dsConnOriginalTitle!=null){
            $('#dsConnPageHeader').html(dsConnOriginalTitle)
        }
        $('#dsConnListLoading').fadeIn(1000);
        showDsConnTable();
    }

    function showDsConnTable() {
        $.getJSON('/dsConnTable.json', function (result) {
            if(getUiEntityOpened("dsConn")){
                return;
            }
            $('#dsConnListLoading').stop();
            $('#dsConnListLoading').fadeOut(100, function () {
                $('#dsConnTableDiv').fadeIn(100, function () {
                    $('#newDsConnTypeSelect').empty();
                    $('#dsConnListTable').empty();
                    for (var key in result.dsConnTypeList) {
                        var value = result.dsConnTypeList[key]
                        $('#newDsConnTypeSelect').append(
                            '<li><a id="' + key + '" href="#dsConn" class="newDsConnOption">'
                            + value + '</a></li>');
                    }

                    var dsConnList = $.map(result.dsConnList, function (dsConn, idx) {
                        var onClick="sectionMap[\"dsConn\"].showEntityForm(\""+dsConn.connectionName+"\",false)";
                        dsConn.clickableName =
                            "<a class='editBtn' onclick='"+onClick+"' style='cursor:pointer;' href='#dsConn'"
                            + dsConn.connectionName + "'>"+ dsConn.connectionName + "</a>";
                        return dsConn
                    })
                    if (dsConnDataTable) {
                        dsConnDataTable.destroy();
                    }

                    dsConnDataTable = $('#dsConnListTable').DataTable({
                        data: dsConnList,
                        paging: false,
                        bStateSave: true,
                        columnDefs: [{
                            defaultContent: "",
                            targets: "_all"
                        }],
                        columns: [
                            {title: 'Name', data: 'clickableName'},
                            {title: 'Type', data: 'connectionType',},
                            {title: 'Description', data: 'description'},
                            {title: 'Last Modified', data: 'lastModifiedTime'},
                        ]
                    });
                    if(sectionMap["dsConnClearSearch"]==null){
                        sectionMap["dsConnClearSearch"]=true;
                        dsConnDataTable.search('').columns().search( '' ).draw();
                    }
                });
            });
        })
    }
})()