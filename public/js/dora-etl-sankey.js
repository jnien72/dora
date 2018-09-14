function renderSankey(tableList, tableTypeMapping, tableTypeColorMapping) {
    var exampleNodes = [];
    var exampleLinks = [];
    var addedTables = [];
    var firstLevelTables = [];
    tableList.map(function (x) {
        if (x.dependencies != null) {
            x.dependencies.map(function (dep) {
                if (tableTypeMapping[dep] != null && (addedTables.indexOf(dep) < 0)) {
                    addedTables.push(dep)
                    firstLevelTables.push(dep)
                }
            })
        }
    })
    var levelMapping = [];

    levelMapping.push(firstLevelTables)

    var dsNodesCount = addedTables.length

    addedTables.map(function (tableName) {
        exampleNodes.push({"type": tableTypeMapping[tableName], "id": tableName, "parent": null, "name": tableName});
    })
    var lastLength = -1
    while (addedTables.length != lastLength) {
        lastLength = addedTables.length
        var levelNodes = 0
        var start = addedTables.length
        var levelTables = []
        addedTables.map(function (addedTable) {
            tableList.map(function (tmp) {
                var name = tmp.name
                if (tmp.dependencies != null &&
                    (tmp.dependencies.indexOf(addedTable) >= 0)) {
                    if (addedTables.indexOf(name) < 0) {
                        levelNodes++;
                        var clazz = tmp['@class']
                        var type = extractNodeType(clazz);
                        var lastTxDate = tmp.lastTxDate;
                        var group=tmp.group;
                        exampleNodes.push({
                            "type": type,
                            "id": name,
                            "group": group,
                            "parent": null,
                            "name": name,
                            "lastTxDate": lastTxDate,
                        });
                        addedTables.push(name);
                        levelTables.push(name)
                    }
                }
            })
        })
        if (levelTables.length > 0)
            levelMapping.push(levelTables)

        var firstLevelNodes = dsNodesCount
        var end = addedTables.length
        if (start == end) {
            //add all without dependencies
            tableList.map(function (tmp) {
                var name = tmp.name
                if ((tmp.dependencies == null || tmp.dependencies.length == 0) && addedTables.indexOf(name) < 0) {
                    var lastTxDate = tmp.lastTxDate
                    var clazz = tmp['@class']
                    var type = extractNodeType(clazz);
                    var group=tmp.group
                    exampleNodes.push({
                        "type": type,
                        "id": name,
                        "group": group,
                        "parent": null,
                        "name": name,
                        "lastTxDate": lastTxDate,
                    });
                    addedTables.push(name);
                    firstLevelNodes++
                }
            })
        }
    }

    tableList.map(function (x) {
        if (x.dependencies != null && x.dependencies.length > 0) {
            x.dependencies.map(function (dep) {
                var exist = $.grep(exampleNodes, function(el) {
                    return el.name === dep
                });
                if(exist.length > 0) {
                    exampleLinks.push({"source": dep, "target": x.name, "value": 1});
                }
            })
        }
    })
    //compute height
    var maxLevelLinks = 0
    levelMapping.map(function (level) {
        var left = 0
        var right = 0
        level.map(function (dep) {
            var localLeft = 0
            var localRight = 0
            exampleLinks.map(function (link) {
                if (link.source == dep) {
                    localLeft++
                }
                if (link.target == dep) {
                    localRight++
                }
            })
            if (localLeft == 0)localLeft = 1
            if (localRight == 0)localRight = 1
            left += localLeft
            right += localRight
        })
        maxLevelLinks = Math.max(Math.max(left, right), maxLevelLinks)
    })

    'use strict';

    var svg, tooltip, biHiSankey, path, defs, colorScale, highlightColorScale, isTransitioning;

    var types = ["Predefined", "DataSource"];
    var typeColors = ["#999", "#0E9B17"];
    var typeHighLightColors = ["#999", "#0E9B17"];

    for(var key in tableTypeColorMapping) {
        var type = extractNodeType(key);
        types.push(type);
        typeColors.push(tableTypeColorMapping[key]);
        typeHighLightColors.push(tableTypeColorMapping[key]);
    }

    var OPACITY = {
            NODE_DEFAULT: 0.8,
            NODE_FADED: 0.1,
            NODE_HIGHLIGHT: 0.8,
            LINK_DEFAULT: 0.5,
            LINK_FADED: 0.05,
            LINK_HIGHLIGHT: 0.8
        },
        TYPES = types,
        TYPE_COLORS = typeColors,
        TYPE_HIGHLIGHT_COLORS = typeHighLightColors,
        LINK_COLOR = "#C0C0C0",
        OUTFLOW_COLOR = "#87CEEB",
        INFLOW_COLOR = "#FFB6C1",
        NODE_WIDTH = 16,
        OUTER_MARGIN = 10,
        MARGIN = {
            TOP: OUTER_MARGIN,
            RIGHT: OUTER_MARGIN,
            BOTTOM: OUTER_MARGIN,
            LEFT: OUTER_MARGIN
        },
        TRANSITION_DURATION = 200,
        HEIGHT = maxLevelLinks * 50,
        WIDTH = Math.min($(window).width() - 150, levelMapping.length * 240),
        LAYOUT_INTERATIONS = 32,
        REFRESH_INTERVAL = 7000;


    // Used when temporarily disabling user interractions to allow animations to complete
    disableUserInterractions = function (time) {
        isTransitioning = true;
        setTimeout(function () {
            isTransitioning = false;
        }, time);
    },

        hideTooltip = function () {
            return tooltip.transition()
                .duration(TRANSITION_DURATION)
                .style("opacity", 0);
        },

        showTooltip = function () {
            return tooltip
                .style("left", d3.event.pageX + "px")
                .style("top", d3.event.pageY + 15 + "px")
                .transition()
                .duration(TRANSITION_DURATION)
                .style("opacity", 1);
        };

    colorScale = d3.scale.ordinal().domain(TYPES).range(TYPE_COLORS),
        highlightColorScale = d3.scale.ordinal().domain(TYPES).range(TYPE_HIGHLIGHT_COLORS),

        svg = d3.select("#etlListTable").append("svg")
            .attr("width", WIDTH + MARGIN.LEFT + MARGIN.RIGHT)
            .attr("height", HEIGHT + MARGIN.TOP + MARGIN.BOTTOM)
            .append("g")
            .attr("transform", "translate(" + MARGIN.LEFT + "," + MARGIN.TOP + ")");

    svg.append("g").attr("id", "links");
    svg.append("g").attr("id", "nodes");


    tooltip = d3.select("#etlListTable").append("div").attr("id", "tooltip");

    tooltip.style("opacity", 0)
        .append("p")
        .attr("class", "value");

    biHiSankey = d3.biHiSankey();

    // Set the biHiSankey diagram properties
    biHiSankey
        .nodeWidth(NODE_WIDTH)
        .nodeSpacing(20)
        .linkSpacing(20)
        .size([WIDTH, HEIGHT]);

    path = biHiSankey.link().curvature(0.45);

    defs = svg.append("defs");

    defs.append("marker")
        .style("fill", LINK_COLOR)
        .attr("id", "arrowHead")
        .attr("viewBox", "0 0 6 10")
        .attr("refX", "1")
        .attr("refY", "5")
        .attr("markerUnits", "strokeWidth")
        .attr("markerWidth", "1")
        .attr("markerHeight", "1")
        .attr("orient", "auto")
        .append("path")
        .attr("d", "M 0 0 L 1 0 L 6 5 L 1 10 L 0 10 z");

    defs.append("marker")
        .style("fill", OUTFLOW_COLOR)
        .attr("id", "arrowHeadInflow")
        .attr("viewBox", "0 0 6 10")
        .attr("refX", "1")
        .attr("refY", "5")
        .attr("markerUnits", "strokeWidth")
        .attr("markerWidth", "1")
        .attr("markerHeight", "1")
        .attr("orient", "auto")
        .append("path")
        .attr("d", "M 0 0 L 1 0 L 6 5 L 1 10 L 0 10 z");

    defs.append("marker")
        .style("fill", INFLOW_COLOR)
        .attr("id", "arrowHeadOutlow")
        .attr("viewBox", "0 0 6 10")
        .attr("refX", "1")
        .attr("refY", "5")
        .attr("markerUnits", "strokeWidth")
        .attr("markerWidth", "1")
        .attr("markerHeight", "1")
        .attr("orient", "auto")
        .append("path")
        .attr("d", "M 0 0 L 1 0 L 6 5 L 1 10 L 0 10 z");

    function update() {
        var link, linkEnter, node, nodeEnter;

        function dragmove(node) {
            node.x = Math.max(0, Math.min(WIDTH - node.width, d3.event.x));
            node.y = Math.max(0, Math.min(HEIGHT - node.height, d3.event.y));
            d3.select(this).attr("transform", "translate(" + node.x + "," + node.y + ")");
            biHiSankey.relayout();
            svg.selectAll(".node").selectAll("rect").attr("height", function (d) {
                return d.height;
            });
            link.attr("d", path);
        }

        function containChildren(node) {
            node.children.forEach(function (child) {
                child.state = "contained";
                child.parent = this;
                child._parent = null;
                containChildren(child);
            }, node);
        }

        function expand(node) {
            node.state = "expanded";
            node.children.forEach(function (child) {
                child.state = "collapsed";
                child._parent = this;
                child.parent = null;
                containChildren(child);
            }, node);
        }

        function collapse(node) {
            node.state = "collapsed";
            containChildren(node);
        }

        function restoreLinksAndNodes() {
            link
                .style("stroke", LINK_COLOR)
                .style("marker-end", function () {
                    return 'url(#arrowHead)';
                })
                .transition()
                .duration(TRANSITION_DURATION)
                .style("opacity", OPACITY.LINK_DEFAULT);

            node
                .selectAll("rect")
                .style("cursor", "pointer")
                .style("fill", function (d) {
                    d.color = colorScale(d.type.replace(/ .*/, ""));
                    return d.color;
                })
                .style("stroke", function (d) {
                    return d3.rgb(colorScale(d.type.replace(/ .*/, ""))).darker(0.1);
                })
                .style("fill-opacity", OPACITY.NODE_DEFAULT);

            node.filter(function (n) {
                    return n.state === "collapsed";
                })
                .transition()
                .duration(TRANSITION_DURATION)
                .style("opacity", OPACITY.NODE_DEFAULT);
        }

        function showHideChildren(node) {
            disableUserInterractions(2 * TRANSITION_DURATION);
            hideTooltip();
            if (node.state === "collapsed") {
                expand(node);
            }
            else {
                collapse(node);
            }

            biHiSankey.relayout();
            update();
            link.attr("d", path);
            restoreLinksAndNodes();
        }

        function highlightConnected(g) {
            link.filter(function (d) {
                    return d.source === g;
                })
                .style("marker-end", function () {
                    return 'url(#arrowHeadInflow)';
                })
                .style("stroke", OUTFLOW_COLOR)
                .style("opacity", OPACITY.LINK_DEFAULT);

            link.filter(function (d) {
                    return d.target === g;
                })
                .style("marker-end", function () {
                    return 'url(#arrowHeadOutlow)';
                })
                .style("stroke", INFLOW_COLOR)
                .style("opacity", OPACITY.LINK_DEFAULT);
        }

        function fadeUnconnected(g) {
            link.filter(function (d) {
                    return d.source !== g && d.target !== g;
                })
                .style("marker-end", function () {
                    return 'url(#arrowHead)';
                })
                .transition()
                .duration(TRANSITION_DURATION)
                .style("opacity", OPACITY.LINK_FADED);

            node.filter(function (d) {
                return (d.name === g.name) ? false : !biHiSankey.connected(d, g);
            }).transition()
                .duration(TRANSITION_DURATION)
                .style("opacity", OPACITY.NODE_FADED);
        }

        link = svg.select("#links").selectAll("path.link")
            .data(biHiSankey.visibleLinks(), function (d) {
                return d.id;
            });

        link.transition()
            .duration(TRANSITION_DURATION)
            .style("stroke-WIDTH", function (d) {
                return Math.max(1, d.thickness);
            })
            .attr("d", path)
            .style("opacity", OPACITY.LINK_DEFAULT);


        link.exit().remove();


        linkEnter = link.enter().append("path")
            .attr("class", "link")
            .style("fill", "none");

        linkEnter.on('mouseenter', function (d) {
            if (!isTransitioning) {

                d3.select(this)
                    .style("stroke", LINK_COLOR)
                    .transition()
                    .duration(TRANSITION_DURATION / 2)
                    .style("opacity", OPACITY.LINK_HIGHLIGHT);
            }
        });

        linkEnter.on('mouseleave', function () {
            if (!isTransitioning) {
                hideTooltip();

                d3.select(this)
                    .style("stroke", LINK_COLOR)
                    .transition()
                    .duration(TRANSITION_DURATION / 2)
                    .style("opacity", OPACITY.LINK_DEFAULT);
            }
        });

        linkEnter.sort(function (a, b) {
                return b.thickness - a.thickness;
            })
            .classed("leftToRight", function (d) {
                return d.direction > 0;
            })
            .classed("rightToLeft", function (d) {
                return d.direction < 0;
            })
            .style("marker-end", function () {
                return 'url(#arrowHead)';
            })
            .style("stroke", LINK_COLOR)
            .style("opacity", 0)
            .transition()
            .delay(TRANSITION_DURATION)
            .duration(TRANSITION_DURATION)
            .attr("d", path)
            .style("stroke-WIDTH", function (d) {
                return Math.max(1, d.thickness);
            })
            .style("opacity", OPACITY.LINK_DEFAULT);


        node = svg.select("#nodes").selectAll(".node")
            .data(biHiSankey.collapsedNodes(), function (d) {
                return d.id;
            });


        node.transition()
            .duration(TRANSITION_DURATION)
            .attr("transform", function (d) {
                return "translate(" + d.x + "," + d.y + ")";
            })
            .style("opacity", OPACITY.NODE_DEFAULT)
            .select("rect")
            .style("fill", function (d) {
                d.color = colorScale(d.type.replace(/ .*/, ""));
                return d.color;
            })
            .style("stroke", function (d) {
                return d3.rgb(colorScale(d.type.replace(/ .*/, ""))).darker(0.1);
            })
            .style("stroke-WIDTH", "1px")
            .attr("height", function (d) {
                return d.height;
            })
            .attr("width", biHiSankey.nodeWidth());


        node.exit()
            .transition()
            .duration(TRANSITION_DURATION)
            .attr("transform", function (d) {
                var collapsedAncestor, endX, endY;
                collapsedAncestor = d.ancestors.filter(function (a) {
                    return a.state === "collapsed";
                })[0];
                endX = collapsedAncestor ? collapsedAncestor.x : d.x;
                endY = collapsedAncestor ? collapsedAncestor.y : d.y;
                return "translate(" + endX + "," + endY + ")";
            })
            .remove();


        nodeEnter = node.enter().append("g").attr("class", "node");

        nodeEnter
            .attr("transform", function (d) {
                var startX = d._parent ? d._parent.x : d.x,
                    startY = d._parent ? d._parent.y : d.y;
                return "translate(" + startX + "," + startY + ")";
            })
            .style("opacity", 1e-6)
            .transition()
            .duration(TRANSITION_DURATION)
            .style("opacity", OPACITY.NODE_DEFAULT)
            .attr("transform", function (d) {
                return "translate(" + d.x + "," + d.y + ")";
            });

        nodeEnter.append("text");
        nodeEnter.append("rect")
            .style("fill", function (d) {
                d.color = colorScale(d.type.replace(/ .*/, ""));
                return d.color;
            })
            .style("stroke", function (d) {
                return d3.rgb(colorScale(d.type.replace(/ .*/, ""))).darker(0.1);
            })
            .style("stroke-WIDTH", "1px")
            .attr("height", function (d) {
                return d.height;
            })
            .attr("width", biHiSankey.nodeWidth());

        node.on("mouseenter", function (g) {
            if (!isTransitioning) {
                restoreLinksAndNodes();
                highlightConnected(g);
                fadeUnconnected(g);
                d3.select(this).select("rect")
                    .style("fill", function (d) {
                        return d3.rgb(colorScale(d.type.replace(/ .*/, ""))).brighter(0.8);
                    })
                    .style("stroke", function (d) {
                        return d3.rgb(d.color).darker(1);
                    });

                // Get the horizontal coordinate
                tooltip.style("left", event.clientX - 100 + "px")
                    .style("top", event.pageY + event.clientY - 550 - 10 + "px")
                    .transition()
                    .duration(TRANSITION_DURATION)
                    .style("opacity", 1).select(".value")
                    .text(function () {
                        if (g.type == 'Unmanaged' || g.type == 'DataSource') {
                            return "Name: " + g.name + "\nType: " + g.type;
                        } else {
                            return "Name: " + g.name +
                                "\nGroup: " + (g.group === undefined ? "n/a" : g.group) +
                                "\nTxDate: " + (g.lastTxDate === undefined ? "n/a" : g.lastTxDate) +
                                "\nType: " + g.type;

                        }
                    });
            }
        })

        node.filter(function (d) {
            return d.type.indexOf("etl.") != -1;
        }).on("dblclick", function(g) {
            $(this).popover({
                placement: 'auto right',
                html: 'true',
                title : g.name,
                content : '<div class="btn-group-vertical"><button class="btn btn-default btn-table editBtn" id="' + g.name + '" style="margin-right: 0px;" onClick="sectionMap[\'etl\'].showEntityForm(this.id, false);"><i class="fa fa-pencil fa-fw"></i> Open</button>' +
                '<button class="btn btn-default  btn-table importBtn" id="' + g.name + '" style="margin-right: 0px;"><i class="fa fa-file-text-o fa-fw"></i> <i class="fa fa-arrow-right fa-fw"></i><i class="fa fa-database fa-fw"></i></button></div>',
                container: 'body',
                trigger: 'manual',
            });
            $(this).popover('toggle');
        })

        $(document).on('click', '.importBtn', function() {
            $('#etlImportTableName').text(this.id);
            $('#etlImportTableModal').modal('show');
            setTimeout(function () {
                $("#etlImportTableConfirm").focus()
            }, 350)
        })

        $(document).on('click', 'html', function(e) {
            if (typeof $(e.target).data('original-title') == 'undefined') {
                $('[data-original-title]').popover('hide');
            }
        });

        node.on("mouseleave", function () {
            if (!isTransitioning) {
                hideTooltip();
                restoreLinksAndNodes();
            }
        });

        node.filter(function (d) {
                return d.children.length;
            })
            .on("dblclick", showHideChildren);

        // allow nodes to be dragged to new positions
        node.call(d3.behavior.drag()
            .origin(function (d) {
                return d;
            })
            .on("drag", dragmove));

        // add in the text for the nodes
        node.filter(function (d) {
                return d.value !== 0;
            })
            .select("text")
            .attr("x", -6)
            .attr("y", function (d) {
                return d.height / 2;
            })
            .attr("dy", ".35em")
            .attr("text-anchor", "end")
            .attr("transform", null)
            .text(function (d) {
                return d.name;
            })
            .filter(function (d) {
                return d.x < WIDTH / 2;
            })
            .attr("x", 6 + biHiSankey.nodeWidth())
            .attr("text-anchor", "start");
    }

    try {
        biHiSankey
            .nodes(exampleNodes)
            .links(exampleLinks)
            .initializeNodes(function (node) {
                node.state = node.parent ? "contained" : "collapsed";
            })
            .layout(LAYOUT_INTERATIONS);

        disableUserInterractions(2 * TRANSITION_DURATION);

        update();
    } catch (x) {
        Modal.show("Error ocurred while trying to render ETL Graph, the workflow seems to be corrupted <br/>" + x, "ERROR")
    }
}

function extractNodeType(className) {
    if(className.indexOf("com.eds.dora.") != 0 || className.indexOf(".model.") == -1) {
        throw new Error("className [" + className + "] is not match class name pattern");
    }
    return className.replace("com.eds.dora.", "").replace(".model.", ".");
}
