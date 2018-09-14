var sectionMap = {};
var sectionBuffer ={}; //this is used to store temporary objects to be destroyed later, per module
var sectionLastFocus = {};
var sectionEntityOpened = {};
var instancesStatusTable = null;
var topologyEditor= null;

$(document).ready(function () {
    disablePreviousPage();

    $(".sectionMenu").click(function () {
        var section = jQuery(this).attr("href").substring(1)
        displaySection(section)
    })

    var currentSection=getCurrentSection()

    $('.sectionMenu').each(function (i, obj) {
        var sectionName = jQuery(this).attr("href").substring(1)
        var url = "/" + sectionName + ".html"
        $("#" + sectionName + "PanelDiv").load(url, function () {
            if (sectionMap[sectionName] != undefined) {
                if (typeof sectionMap[sectionName].setup == "function") {
                    sectionMap[sectionName].setup()
                }
            }
            if(sectionName==currentSection){
                displaySection(currentSection);
            }
        });
    });

    $(document).on('click', '.restartInstanceBtn', function() {
        $('#instancesModal').modal('hide');
        var instanceName = this.id;
        Modal.confirm('Are you sure you want to restart <b>' + instanceName + '</b>?<br/>Running jobs will be interrupted and waiting jobs will be erased.',
            'Warning',
            (function(instanceName) {
                return function() {
                    $.ajax({
                        type: "POST",
                        url: "/instance/restart/" + instanceName,
                    }).error(function (result) {
                        Modal.show('Error occurred: ' + result.responseText)
                    })
                }
            })(instanceName));
    })

    $('#instancesStatus').click(function() {
        $('#instanceStatusLoading').show();
        $.getJSON('/instance/status.json', function (list) {
            if (instancesStatusTable) {
                instancesStatusTable.destroy();
            }
            $('#instanceStatusLoading').hide();

            if(list) {
                for(var i = 0; i < list.length; i++) {
                    var instance = list[i];
                    if(instance['status'] == 'running') {
                        instance['action'] = '<button type="button" class="restartInstanceBtn" id="' + instance.name + '">Restart</button>'
                    }
                    if(instance['sparkUI'] != '--') {
                        var sparkUIUrl = 'http://' + instance['sparkUI']
                        instance['sparkUI'] = '<a target="_blank" href="' + sparkUIUrl + '">' + sparkUIUrl + '</a>';
                    }
                }
            }

            instancesStatusTable = $('#instancesStatusTable').DataTable({
                data: list,
                paging: false,
                searching: false,
                bStateSave: true,
                bInfo: false,
                lengthChange: false,
                ordering: false,
                columnDefs: [{
                    defaultContent: "",
                    targets: "_all"
                }],
                columns: [
                    {title: 'Name', data: 'name'},
                    {title: 'Heap Size', data: 'heapSize'},
                    {title: 'Status', data: 'status'},
                    {title: 'Start Time', data: 'startTime'},
                    {title: 'Service Port', data: 'servicePort'},
                    {title: 'Spark UI', data: 'sparkUI'},
                    {title: 'Action',data: 'action' },
                ]
            });


        })
    })

    $('#topology').click(function() {
        $('#topologyBody').hide();
        $('#topologyMessage').hide();
        $.get('/topology.json', {}).done(function(topology) {
                setupTopologyEditor(topology);
                $('#topologyBody').show();
            }).error(function (result) {
                $('#topologyMessage').text(result.responseText);
                $('#topologyMessage').show();
            })
    })

    function setupTopologyEditor(topology) {
        var langTools = ace.require("ace/ext/language_tools");
        topologyEditor = ace.edit("topologyEditor")
        topologyEditor.setShowPrintMargin(false)
        topologyEditor.setTheme("ace/theme/chrome")
        topologyEditor.getSession().setMode("ace/mode/json")
        topologyEditor.getSession().setOptions({
            tabSize: 2,
            useSoftTabs: true,
        })
        topologyEditor.getSession().setUseWrapMode(true)
        topologyEditor.setOptions({
            minLines: 18,
            maxLines: 28,
        })

        topologyEditor.setValue(JSON.stringify(JSON.parse(topology), null, 2));
        topologyEditor.focus();
    }

    $('#updateTopologyBtn').click(function() {
        if(topologyEditor != null) {
            $('#updateTopologyBtn img').show();
            $('#updateTopologyBtn span').hide();
            var topology = topologyEditor.getValue();
            $('#topologyMessage').hide();
            $.ajax({
                type: "POST",
                url: "/topology.json",
                data: topology,
                contentType: "application/json charset=utf-8",
            }).success(function(result) {
                Modal.show('Topology updated', 'Message');
                $('#updateTopologyBtn img').hide();
                $('#updateTopologyBtn span').show();
                $('#topologyModal').modal('hide');
            }).error(function (result) {
                $('#updateTopologyBtn img').hide();
                $('#updateTopologyBtn span').show();
                $('#topologyMessage').text(result.responseText);
                $('#topologyMessage').show();
            })
        }
    });

})

function displaySection(section) {

    $('.sectionMenu').each(function (i, obj) {
        var sectionName = jQuery(this).attr("href").substring(1)
        if (section != sectionName) {
            $("#" + sectionName + "PanelDiv").fadeOut(0)
        } else {
            sectionMap[sectionName].beforeShowSection()
            $("#" + sectionName + "PanelDiv").fadeIn(0)
            refreshPage()
            sectionMap[sectionName].showSection(function () {
                refreshPage()
            })
        }
    })
}

function disablePreviousPage() {
    history.pushState(null, null, location.href);
    window.onpopstate = function (event) {
        history.go(1);
    }
}

var formatDateTime = function (time, format) {
    var t = new Date(time);
    var tf = function (i) {
        return (i < 10 ? '0' : '') + i
    };
    return format.replace(/yyyy|MM|dd|HH|mm|ss/g, function (pattern) {
        switch (pattern) {
            case 'yyyy':
                return tf(t.getFullYear());
                break;
            case 'MM':
                return tf(t.getMonth() + 1);
                break;
            case 'mm':
                return tf(t.getMinutes());
                break;
            case 'dd':
                return tf(t.getDate());
                break;
            case 'HH':
                return tf(t.getHours());
                break;
            case 'ss':
                return tf(t.getSeconds());
                break;
        }
    })
}

function getElementsByTagNames(list,obj) {
    if (!obj) var obj = document;
    var tagNames = list.split(',');
    var resultArray = new Array();
    for (var i=0;i<tagNames.length;i++) {
        var tags = obj.getElementsByTagName(tagNames[i]);
        for (var j=0;j<tags.length;j++) {
            resultArray.push(tags[j]);
        }
    }
    var testNode = resultArray[0];
    if (!testNode) return [];
    if (testNode.sourceIndex) {
        resultArray.sort(function (a,b) {
            return a.sourceIndex - b.sourceIndex;
        });
    }
    else if (testNode.compareDocumentPosition) {
        resultArray.sort(function (a,b) {
            return 3 - (a.compareDocumentPosition(b) & 6);
        });
    }
    return resultArray;
}

function refreshPage() {
    topOffset = 50;
    width = (this.window.innerWidth > 0) ? this.window.innerWidth : this.screen.width;
    if (width < 768) {
        $('div.navbar-collapse').addClass('collapse');
        topOffset = 100; // 2-row-menu
    } else {
        $('div.navbar-collapse').removeClass('collapse');
    }

    height = ((this.window.innerHeight > 0) ? this.window.innerHeight : this.screen.height) - 1;
    height = height;
    if (height < 1) height = 1;
    if (height > topOffset) {
        $(".sectionContent").css("min-height", (height) + "px");
    }
}

var Modal = (function () {
    return {
        show: function show(message, title) {
            $('#messageModal .modal-title').html(title);
            $('#messageModal .modal-body').html(message);
            $('#messageModal #modalConfirmBtn').hide();
            $('#messageModal').modal('show');
            $('#messageModal .modalCloseBtn').click(function () {
                //workaround to remove backgrop when the alert popup after confirmation callback executed
                $('.modal-backdrop').remove()
            })
        }
        ,
        confirm: function confirm(message, title, callback) {
            $('#messageModal .modal-title').html(title);
            $('#messageModal .modal-body').html(message);
            $('#messageModal #modalConfirmBtn').show();
            $('#messageModal #modalConfirmBtn').click(function () {
                callback();
            })
            $('#messageModal').modal('show');
        },
    }
})()

function getCurrentSection(){
    if(window.location.hash) {
        return window.location.hash.substring(1).split('/')[0];
    } else {
        return ""
    }
}


function getUiBuffer(sectionName){
    if(sectionBuffer[sectionName]==null)
        sectionBuffer[sectionName]=[]
    return sectionBuffer[sectionName];
}

function getUiEntityOpened(sectionName){
    if(sectionEntityOpened[sectionName]==null){
        sectionEntityOpened[sectionName]=false
    }
    return sectionEntityOpened[sectionName];
}

function setUiEntityOpened(sectionName,obj){
    return sectionEntityOpened[sectionName]=obj;
}


function getUiLastFocus(sectionName){
    return sectionLastFocus[sectionName];
}

function setUiLastFocus(sectionName,obj){
    return sectionLastFocus[sectionName]=obj;
}


