var queryInputFieldId = "query";
var allguides = new Array();
var guideClassName = "guide";

var elementsClassNames = new Array();
elementsClassNames.push('training');
elementsClassNames.push('latestguides');
elementsClassNames.push('guidegroup');
elementsClassNames.push('tagsbytopic');
elementsClassNames.push('guidesuggestion');

onload = function () {
    var elements = document.getElementsByClassName(guideClassName);
    for ( var i = 0; i < elements.length; i++ ) {
        var element = elements[i];
        var guide = { href: element.getAttribute('href'), title: element.text, tags: tagsAtGuide(element.parentNode)  }; /* */
        allguides.push(guide);
    }

    if ( document.getElementById(queryInputFieldId) ) {
        var e = document.getElementById(queryInputFieldId);
        e.oninput = onQueryChanged;
        e.onpropertychange = e.oninput; // for IE8
    }
};

function hideElementsToDisplaySearchResults() {
    for ( var i = 0; i < elementsClassNames.length; i++) {
        var className = elementsClassNames[i];
        hideElementsByClassName(className);
    }
}

function showElementsToDisplaySearchResults() {
    for (var i = 0; i < elementsClassNames.length; i++) {
        var className = elementsClassNames[i];
        showElementsByClassName(className);
    }
}

function hideElementsByClassName(className) {
    var elements = document.getElementsByClassName(className);
    for ( var i = 0; i < elements.length; i++ ) {
        var element = elements[i];
        element.style.display = "none";
    }
}

function showElementsByClassName(className) {
    var elements = document.getElementsByClassName(className);
    for ( var i = 0; i < elements.length; i++ ) {
        var element = elements[i];
        element.style.display = "block";
    }
}

function tagsAtGuide(element) {
    var tags = new Array();
    for (var y = 0; y < element.childNodes.length; y++) {

        if (element.childNodes[y].className == "tag") {
            var tag = element.childNodes[y];
            tags.push(tag.textContent);
        }
    }
    return tags;
}

function onQueryChanged() {
    var query = queryValue();
    query = query.trim();
    if ( query === '' ) {
        showElementsToDisplaySearchResults();
        document.getElementById("searchresults").innerHTML = "";
        document.getElementById("noresults").innerHTML = "";

        return;
    }

    var matchingGuides = new Array();
    if ( query !== '' ) {
        for (var i = 0; i < allguides.length; i++) {
            var guide = allguides[i];
            if ( doesGuideMatchesQuery(guide, query) ) {
                matchingGuides.push(guide);
            }
        }
    }
    if ( matchingGuides.length > 0 ) {
        hideElementsToDisplaySearchResults();
        var html = renderGuideGroup(matchingGuides);
        document.getElementById("searchresults").innerHTML = html;
        document.getElementById("noresults").innerHTML = "";

    } else {
        document.getElementById("searchresults").innerHTML = "";
        document.getElementById("noresults").innerHTML = '<p>No results found</p>'
    }
}

function doesGuideMatchesQuery(guide, query) {
    if (guide.title.toLowerCase().indexOf(query.toLowerCase()) !== -1) {
        return true;
    }

    for ( var i = 0; i < guide.tags.length; i++) {
        var tag = guide.tags[i];
        if (tag.toLowerCase().indexOf(query.toLowerCase()) !== -1) {
            return true;
        }
    }
    return false;
}

function queryValue() {
    return document.getElementById(queryInputFieldId).value;
}

function renderGuideGroup(guides) {
    var html = "";
    html += "<div class='guidegroup'>";
    html += "  <div class='guidegroupheader'>";
    html += "    <h2>Guides Filtered by: " + queryValue() + "</h2>";
    html += "  </div>";
    html += "  <ul>";
    for ( var i = 0; i < guides.length; i++ ) {
        html += "    " + renderGuideAsHtmlLi(guides[i]);
    }
    html += "  </ul>";
    html += "</div>";
    return html;
}

function renderGuideAsHtmlLi(guide) {
    var html = "<li>";
    html += "<a class='"+guideClassName+"' href='" + guide.href + "'>" + guide.title + "</a>";
    for ( var i = 0; i < guide.tags.length; i++ ) {
        var tag = guide.tags[i];
        html += "<span style='display: none' class='tag'>"+tag+"</span>";
    }
    html += "</li>"
    return html;
}